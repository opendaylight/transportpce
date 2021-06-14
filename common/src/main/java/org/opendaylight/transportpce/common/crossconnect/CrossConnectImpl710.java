/*
 * Copyright © 2021 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OduConnection.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduConnectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduConnectionKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.otn.renderer.input.Nodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossConnectImpl710 {

    private static final Logger LOG = LoggerFactory.getLogger(CrossConnectImpl710.class);
    private final DeviceTransactionManager deviceTransactionManager;

    public CrossConnectImpl710(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container
        .org.openroadm.device.OduConnection> getOtnCrossConnect(String deviceId, String connectionNumber) {

        return deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL,
            generateOduConnectionIID(connectionNumber), Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device
        .container.org.openroadm.device.OduConnection> generateOduConnectionIID(String connectionNumber) {

        return InstanceIdentifier.create(OrgOpenroadmDevice.class).child(org.opendaylight.yang.gen.v1.http.org
                .openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduConnection.class,
            new OduConnectionKey(connectionNumber));
    }

    public Optional<String> postOtnCrossConnect(List<String> createdOduInterfaces, Nodes node) {
        String deviceId = node.getNodeId();
        String srcTp = createdOduInterfaces.get(0);
        String dstTp = createdOduInterfaces.get(1);

        OduConnectionBuilder oduConnectionBuilder = new OduConnectionBuilder()
            .setConnectionName(srcTp + "-x-" + dstTp)
            .setDestination(new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.odu.connection
                .DestinationBuilder().setDstIf(dstTp).build())
            .setSource(new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.odu.connection
                .SourceBuilder().setSrcIf(srcTp).build())
            .setDirection(Direction.Bidirectional);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device
            .container.org.openroadm.device.OduConnection> oduConnectionIID =
            InstanceIdentifier.create(
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container
                    .OrgOpenroadmDevice.class)
                .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device
                .container.org.openroadm.device.OduConnection.class,
                    new OduConnectionKey(oduConnectionBuilder.getConnectionName())
                );

        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                LOG.error("Device transaction for device {} was not found!", deviceId);
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to obtain device transaction for device {}!", deviceId, e);
            return Optional.empty();
        }

        // post the cross connect on the device
        deviceTx.merge(LogicalDatastoreType.CONFIGURATION, oduConnectionIID, oduConnectionBuilder.build());
        FluentFuture<? extends @NonNull CommitInfo> commit =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Otn-connection successfully created: {}-{}", srcTp, dstTp);
            return Optional.of(srcTp + "-x-" + dstTp);
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to post {}.", oduConnectionBuilder.build(), e);
        }
        return Optional.empty();

    }

    public List<String> deleteOtnCrossConnect(String deviceId, String connectionName) {
        List<String> interfList = new ArrayList<>();
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device
            .container.org.openroadm.device.OduConnection> otnXc = getOtnCrossConnect(deviceId, connectionName);

        if (otnXc.isPresent()) {
            interfList.add(otnXc.get().getSource().getSrcIf());
            interfList.add(otnXc.get().getDestination().getDstIf());
        } else {
            LOG.warn("Cross connect {} does not exist, halting delete", connectionName);
            return null;
        }
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                LOG.error("Device transaction for device {} was not found!", deviceId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to obtain device transaction for device {}!", deviceId, e);
            return null;
        }

        // delete the cross connect on the device
        deviceTx.delete(LogicalDatastoreType.CONFIGURATION, generateOduConnectionIID(connectionName));
        FluentFuture<? extends @NonNull CommitInfo> commit =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Connection {} successfully deleted on {}", connectionName, deviceId);
            return interfList;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete {}", connectionName, e);
        }
        return null;
    }

}
