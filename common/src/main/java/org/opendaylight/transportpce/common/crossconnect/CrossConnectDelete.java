/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.crossconnect.ConnectionReader.Connection;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes a cross connect on a device, reading it and deleting it in one device transaction.
 *
 * <p>The same for every OpenROADM version: a {@link ConnectionReader} reads the cross connects of the device's
 * version.
 */
final class CrossConnectDelete {

    private static final Logger LOG = LoggerFactory.getLogger(CrossConnectDelete.class);

    private final DeviceTransactionManager deviceTransactionManager;

    private final ConnectionReader connectionReader;

    /**
     * Creates a delete.
     *
     * @param deviceTransactionManager opens the transactions on the devices
     * @param connectionReader reads the cross connects of the devices' OpenROADM version
     */
    CrossConnectDelete(DeviceTransactionManager deviceTransactionManager, ConnectionReader connectionReader) {
        this.deviceTransactionManager =
            Objects.requireNonNull(deviceTransactionManager, "deviceTransactionManager cannot be null");
        this.connectionReader = Objects.requireNonNull(connectionReader, "connectionReader cannot be null");
    }

    /**
     * Deletes the roadm-connection or the odu-connection with the given name, looking for the roadm-connection first.
     *
     * @param deviceId the device
     * @param connectionName name of the cross connect
     * @param roadm whether to look for a roadm-connection
     * @param odu whether to look for an odu-connection
     * @return the interfaces of the deleted connection, or null if none of the connections looked for exists
     * @throws DeviceReadException if the device could not be reached in time or read
     * @throws OpenRoadmInterfaceException if the cross connect could not be deleted
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    List<String> delete(String deviceId, String connectionName, boolean roadm, boolean odu)
            throws DeviceReadException, OpenRoadmInterfaceException {
        // Read and delete in one transaction, so the cross connect cannot change in between. The wait for the device
        // is bounded, so a frozen device fails the delete instead of holding up the caller. The transaction may stay
        // open for each read and for the start of the commit.
        int reads = (roadm ? 1 : 0) + (odu ? 1 : 0);
        DeviceTransaction deviceTx = deviceTransactionManager.awaitDeviceTransaction(deviceId,
                Timeouts.DEVICE_READ_TIMEOUT, (reads + 1) * Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        List<String> interfList = null;
        DataObjectIdentifier<?> connectionIID = null;
        try {
            // A device that cannot be read throws, so it is not mistaken for a missing cross connect
            DeviceReadException roadmFailure = null;
            if (roadm) {
                try {
                    Optional<Connection> xc = connectionReader.readRoadmConnection(deviceTx, connectionName);
                    if (xc.isPresent()) {
                        interfList = roadmConnectionInterfaces(xc.orElseThrow());
                        connectionIID = xc.orElseThrow().path();
                    }
                } catch (DeviceReadException e) {
                    if (!odu || !deviceAnswered(e)) {
                        throw e;
                    }
                    // For example a transponder that rejects the roadm-connection read: its odu-connection may still
                    // be readable
                    roadmFailure = e;
                }
            }
            if (interfList == null && odu) {
                try {
                    Optional<Connection> xc = connectionReader.readOduConnection(deviceTx, connectionName);
                    if (xc.isPresent()) {
                        interfList = interfaceNames(xc.orElseThrow());
                        connectionIID = xc.orElseThrow().path();
                    }
                } catch (DeviceReadException e) {
                    if (roadmFailure == null) {
                        throw e;
                    }
                    roadmFailure.addSuppressed(e);
                }
                if (interfList == null && roadmFailure != null) {
                    // No odu-connection, but the roadm-connection that could not be read may exist
                    throw roadmFailure;
                }
            }
        } finally {
            if (interfList == null) {
                // Nothing to delete, or the read failed: let the next transaction have the device
                deviceTx.cancel();
            }
        }
        if (interfList == null) {
            LOG.warn("Cross connect {} does not exist, halting delete", connectionName);
            return null;
        }

        // delete the cross connect on the device
        try {
            deviceTx.delete(LogicalDatastoreType.CONFIGURATION, connectionIID);
        } catch (RuntimeException e) {
            // For example, the mount point was torn down after the read. The transaction will not be committed, so
            // release the device now rather than when the transaction times out.
            deviceTx.cancel();
            throw new OpenRoadmInterfaceException("Failed to delete cross connect %s on node %s: %s"
                .formatted(connectionName, deviceId, e.getMessage()), e);
        }
        FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenRoadmInterfaceException(
                "Interrupted while deleting cross connect %s on node %s".formatted(connectionName, deviceId), e);
        } catch (ExecutionException | CancellationException e) {
            throw new OpenRoadmInterfaceException("Failed to delete cross connect %s on node %s: %s"
                .formatted(connectionName, deviceId, e.getMessage()), e);
        }
        LOG.info("Cross connect {} on node {} successfully deleted.", connectionName, deviceId);
        return interfList;
    }

    /**
     * Returns whether the device answered the read with an error, as opposed to not answering in time or the read
     * being interrupted or cancelled. A device that did not answer one read is unlikely to answer the next.
     *
     * <p>{@link DeviceTransaction}'s timed read reports such an error either as an {@link ExecutionException} cause,
     * when the device's asynchronous reply carried it, or as a synchronous {@link RuntimeException} cause thrown by
     * the read itself, for example when the mount point is torn down while the read is issued. A
     * {@link CancellationException} is a {@link RuntimeException} but means the read did not complete, not that the
     * device answered.
     */
    private boolean deviceAnswered(DeviceReadException failure) {
        Throwable cause = failure.getCause();
        return cause instanceof ExecutionException
            || (cause instanceof RuntimeException && !(cause instanceof CancellationException));
    }

    /**
     * Returns the nmc interfaces a roadm-connection names, and the mc interfaces under them.
     */
    private List<String> roadmConnectionInterfaces(Connection xc) {
        List<String> interfaces = interfaceNames(xc);
        interfaces.addAll(interfaces.stream().map(intf -> intf.replace("nmc", "mc")).toList());
        return interfaces;
    }

    /**
     * Returns the interfaces a cross connect read from the device names. A device may leave out src-if or dst-if,
     * so a missing name is skipped instead of failing the delete.
     */
    private List<String> interfaceNames(Connection xc) {
        List<String> interfaces = new ArrayList<>();
        if (xc.srcIf() != null) {
            interfaces.add(xc.srcIf());
        }
        if (xc.dstIf() != null) {
            interfaces.add(xc.dstIf());
        }
        return interfaces;
    }
}
