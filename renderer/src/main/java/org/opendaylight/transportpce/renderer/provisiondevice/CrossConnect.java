/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.transportpce.renderer.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.connection.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.connection.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossConnect {

    private final DataBroker deviceDb;
    private static final Logger LOG = LoggerFactory.getLogger(CrossConnect.class);
    private final String connectionNumber;
    private final InstanceIdentifier<RoadmConnections> rdmConnectionIID;

    public CrossConnect(DataBroker deviceDb) {
        this.deviceDb = deviceDb;
        connectionNumber = null;
        rdmConnectionIID = null;
    }

    public CrossConnect(DataBroker deviceDb, String connectionNumber) {
        this.deviceDb = deviceDb;
        this.connectionNumber = connectionNumber;
        rdmConnectionIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(RoadmConnections.class,
            new RoadmConnectionsKey(connectionNumber));
    }

    /**
     * This method return the RoadmConnection subtree for a given connection
     * number.
     *
     * @param connectionNumber
     *            Name of the cross connect.
     *
     * @return Roadm connection subtree from the device.
     */
    public RoadmConnections getCrossConnect(String connectionNumber) {
        if (connectionNumber == null && this.connectionNumber != null) {
            connectionNumber = this.connectionNumber;
        }
        if (deviceDb != null) {
            ReadOnlyTransaction rtx = deviceDb.newReadOnlyTransaction();
            Optional<RoadmConnections> roadmConnectionsObject;
            try {
                roadmConnectionsObject = rtx.read(LogicalDatastoreType.OPERATIONAL, rdmConnectionIID).get();
                if (roadmConnectionsObject.isPresent()) {
                    return roadmConnectionsObject.get();
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.info("Error getting roadm-connection subtree from the device for " + connectionNumber, ex);
                return null;
            }
        }
        return null;
    }

    /**
     * This method does a post(edit-config) on roadm connection subtree for a
     * given connection number.
     *
     * @param waveNumber
     *            Wavelength number.
     * @param srcTp
     *            Name of source termination point.
     * @param destTp
     *            Name of destination termination point.
     * @return true/false based on status of operation.
     */
    public boolean postCrossConnect(Long waveNumber, String srcTp, String destTp) {

        RoadmConnectionsBuilder rdmConnBldr = new RoadmConnectionsBuilder();
        rdmConnBldr.setConnectionNumber(srcTp + "-" + destTp + "-" + waveNumber);
        rdmConnBldr.setWavelengthNumber(waveNumber);
        rdmConnBldr.setOpticalControlMode(OpticalControlMode.Off);
        rdmConnBldr.setSource(new SourceBuilder().setSrcIf(srcTp + "-" + waveNumber.toString()).build());
        rdmConnBldr.setDestination(new DestinationBuilder().setDstIf(destTp + "-" + waveNumber.toString()).build());
        InstanceIdentifier<RoadmConnections> rdmConnectionIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(rdmConnBldr.getConnectionNumber()));

        if (deviceDb != null) {
            final WriteTransaction writeTransaction = deviceDb.newWriteOnlyTransaction();
            // post the cross connect on the device
            writeTransaction.put(LogicalDatastoreType.CONFIGURATION, rdmConnectionIID, rdmConnBldr.build());
            final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
            try {
                submit.checkedGet();
                LOG.info("Roadm-connection successfully created: " + srcTp + "-" + destTp + "-" + waveNumber);
                return true;
            } catch (TransactionCommitFailedException ex) {
                LOG.info("Failed to post {} ", rdmConnBldr.build(), ex);
                return false;
            }
        } else {
            LOG.error("Invalid device databroker");
            return false;
        }
    }

    /**
     * This method does a delete(edit-config) on roadm connection subtree for a
     * given connection number.
     *
     * @param connectionNumber
     *            Name of the cross connect.
     * @return true/false based on status of operation.
     */

    public boolean deleteCrossConnect(String connectionNumber) {
        if (connectionNumber == null && this.connectionNumber != null) {
            connectionNumber = this.connectionNumber;
        }
        return deleteCrossConnect();
    }

    /**
     * This method does a delete(edit-config) on roadm connection subtree for a
     * given connection number.
     *
     * @return true/false based on status of operation.
     */

    public boolean deleteCrossConnect() {

        //Check if cross connect exists before delete
        if (getCrossConnect(connectionNumber) == null) {
            LOG.info("Cross connect does not exist, halting delete");
            return false;
        }
        if (deviceDb != null) {
            final WriteTransaction writeTransaction = deviceDb.newWriteOnlyTransaction();
            // post the cross connect on the device
            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, rdmConnectionIID);
            final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
            try {
                submit.checkedGet();
                LOG.info("Roadm connection successfully deleted ");
                return true;
            } catch (TransactionCommitFailedException ex) {
                LOG.info("Failed to delete {} ", connectionNumber, ex);
                return false;
            }
        } else {
            LOG.error("Invalid device databroker");
            return false;
        }
    }

    /**
     * This method does an edit-config on roadm connection subtree for a given
     * connection number in order to set power level for use by the optical
     * power control.
     *
     * @param mode
     *            Optical control modelcan be off, power or gainLoss.
     * @param value
     *            Power value in DBm.
     * @param connectionNumber
     *            Name of the cross connect.
     * @return true/false based on status of operation.
     */
    public boolean setPowerLevel(OpticalControlMode mode, PowerDBm value, String connectionNumber) {
        if (connectionNumber == null && this.connectionNumber != null) {
            connectionNumber = this.connectionNumber;
        }
        return setPowerLevel(mode, value);
    }

    /**
     * This method does an edit-config on roadm connection subtree for a given
     * connection number in order to set power level for use by the optical
     * power control.
     *
     * @param mode
     *            Optical control modelcan be off, power or gainLoss.
     * @param value
     *            Power value in DBm.
     * @return true/false based on status of operation.
     */
    public boolean setPowerLevel(OpticalControlMode mode, PowerDBm value) {

        RoadmConnections rdmConn = getCrossConnect(connectionNumber);
        if (rdmConn != null) {
            RoadmConnectionsBuilder rdmConnBldr = new RoadmConnectionsBuilder(rdmConn);
            rdmConnBldr.setOpticalControlMode(mode);
            rdmConnBldr.setTargetOutputPower(value);
            if (deviceDb != null) {
                final WriteTransaction writeTransaction = deviceDb.newWriteOnlyTransaction();
                // post the cross connect on the device
                writeTransaction.put(LogicalDatastoreType.CONFIGURATION, rdmConnectionIID, rdmConnBldr.build());
                final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
                try {
                    submit.checkedGet();
                    LOG.info("Roadm connection power level successfully set ");
                    return false;
                } catch (TransactionCommitFailedException ex) {
                    LOG.info("Failed to post {} ", rdmConnBldr.build(), ex);
                    return false;
                }
            } else {
                LOG.error("Invalid device databroker");
                return false;
            }
        } else {
            LOG.info("Roadm-Connection does not exist");
            return false;
        }
    }

    /**
     * This public method returns the list of ports (port-trail) for a roadm's
     * cross connect. It calls rpc get-port-trail on device. To be used store
     * detailed path description.
     *
     * @param nodeId
     *            node-id of NE.
     * @param mountService
     *            Reference to mount point service.
     * @param waveNumber
     *            Wavelength number.
     * @param srcTp
     *            Source logical connection point.
     * @param destTp
     *            Destination logical connection point.
     *
     * @return list of Ports object type.
     */
    public Ports getConnectionPortTrail(String nodeId, MountPointService mountService, Long waveNumber, String srcTp,
        String destTp) {

        String connectionName = srcTp + "-" + destTp + "-" + waveNumber;
        MountPoint mountPoint = PortMapping.getDeviceMountPoint(nodeId, mountService);

        final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class);
        if (!service.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeId);
        }
        final OrgOpenroadmDeviceService rpcService = service.get().getRpcService(OrgOpenroadmDeviceService.class);
        final GetConnectionPortTrailInputBuilder portTrainInputBuilder = new GetConnectionPortTrailInputBuilder();
        portTrainInputBuilder.setConnectionNumber(connectionName);
        final Future<RpcResult<GetConnectionPortTrailOutput>> portTrailOutput = rpcService.getConnectionPortTrail(
            portTrainInputBuilder.build());
        if (portTrailOutput != null) {
            try {
                LOG.info("Getting port trail for node " + nodeId + "'s connection number " + connectionName);
                for (Ports ports : portTrailOutput.get().getResult().getPorts()) {
                    LOG.info(nodeId + " - " + "Circuit pack " + ports.getCircuitPackName() + "- Port " + ports
                        .getPortName());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.info("Exception caught", e);
            }
        } else {
            LOG.info("Port trail is null");
        }
        return null;
    }
}
