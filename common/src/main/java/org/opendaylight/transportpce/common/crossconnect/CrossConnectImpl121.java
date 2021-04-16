/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.crossconnect;

import com.google.common.util.concurrent.FluentFuture;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
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

public class CrossConnectImpl121 {
    private static final Logger LOG = LoggerFactory.getLogger(CrossConnectImpl121.class);

    private final DeviceTransactionManager deviceTransactionManager;

    public CrossConnectImpl121(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public Optional<RoadmConnections> getCrossConnect(String deviceId, String connectionNumber) {
        return deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL,
                generateRdmConnectionIID(connectionNumber), Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }

    public Optional<String> postCrossConnect(String deviceId, String srcTp, String destTp,
            SpectrumInformation spectrumInformation) {
        RoadmConnectionsBuilder rdmConnBldr = new RoadmConnectionsBuilder();
        String connectionNumber = spectrumInformation.getIdentifierFromParams(srcTp, destTp);
        rdmConnBldr.setConnectionNumber(connectionNumber);
        rdmConnBldr.setWavelengthNumber(spectrumInformation.getWaveLength());
        rdmConnBldr.setOpticalControlMode(OpticalControlMode.Off);
        rdmConnBldr.setSource(new SourceBuilder().setSrcIf(spectrumInformation.getIdentifierFromParams(srcTp))
                .build());
        rdmConnBldr.setDestination(new DestinationBuilder()
                .setDstIf(spectrumInformation.getIdentifierFromParams(destTp))
                .build());
        InstanceIdentifier<RoadmConnections> rdmConnectionIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(rdmConnBldr.getConnectionNumber()));

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
        deviceTx.put(LogicalDatastoreType.CONFIGURATION, rdmConnectionIID, rdmConnBldr.build());
        FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Roadm-connection successfully created: {}-{}-{}-{}", srcTp, destTp,
                    spectrumInformation.getLowerSpectralSlotNumber(),
                    spectrumInformation.getHigherSpectralSlotNumber());
            return Optional.of(connectionNumber);
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to post {}. Exception: ", rdmConnBldr.build(), e);
        }
        return Optional.empty();
    }

    public List<String> deleteCrossConnect(String deviceId, String connectionNumber) {
        List<String> interfList = new ArrayList<>();
        Optional<RoadmConnections> xc = getCrossConnect(deviceId, connectionNumber);
        //Check if cross connect exists before delete
        if (xc.isPresent()) {
            interfList.add(xc.get().getSource().getSrcIf());
            interfList.add(xc.get().getDestination().getDstIf());
        } else {
            LOG.warn("Cross connect does not exist, halting delete");
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

        // post the cross connect on the device
        deviceTx.delete(LogicalDatastoreType.CONFIGURATION, generateRdmConnectionIID(connectionNumber));
        FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Roadm connection successfully deleted ");
            return interfList;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete {}", connectionNumber, e);
        }
        return null;
    }


    public List<Ports> getConnectionPortTrail(String nodeId, String srcTp, String destTp,
            int lowerSpectralSlotNumber, int higherSpectralSlotNumber) throws OpenRoadmInterfaceException {
        String spectralSlotName = String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber), String.valueOf(higherSpectralSlotNumber));
        String connectionName = generateConnectionNumber(srcTp, destTp, spectralSlotName);
        Optional<MountPoint> mountPointOpt = deviceTransactionManager.getDeviceMountPoint(nodeId);
        List<Ports> ports = null;
        MountPoint mountPoint;
        if (mountPointOpt.isPresent()) {
            mountPoint = mountPointOpt.get();
        } else {
            LOG.error("Failed to obtain mount point for device {}!", nodeId);
            return Collections.emptyList();
        }
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
                RpcResult<GetConnectionPortTrailOutput> connectionPortTrailOutputRpcResult = portTrailOutput.get();
                GetConnectionPortTrailOutput connectionPortTrailOutput = connectionPortTrailOutputRpcResult.getResult();
                if (connectionPortTrailOutput == null) {
                    throw new OpenRoadmInterfaceException(String.format("RPC get connection port trail called on"
                            + " node %s returned null!", nodeId));
                }
                LOG.info("Getting port trail for node {}'s connection number {}", nodeId, connectionName);
                ports = connectionPortTrailOutput.getPorts();
                for (Ports port : ports) {
                    LOG.info("{} - Circuit pack {} - Port {}", nodeId, port.getCircuitPackName(), port.getPortName());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Exception caught", e);
            }
        } else {
            LOG.warn("Port trail is null in getConnectionPortTrail for nodeId {}", nodeId);
        }
        return ports != null ? ports : Collections.emptyList();
    }

    private InstanceIdentifier<RoadmConnections> generateRdmConnectionIID(String connectionNumber) {
        return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber));
    }

    private String generateConnectionNumber(String srcTp, String destTp, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, srcTp, destTp, spectralSlotName);
    }

    public boolean setPowerLevel(String deviceId, OpticalControlMode mode, BigDecimal powerValue, String ctNumber) {

        Optional<RoadmConnections> rdmConnOpt = getCrossConnect(deviceId, ctNumber);
        if (!rdmConnOpt.isPresent()) {
            LOG.warn("Roadm-Connection is null in set power level ({})", ctNumber);
            return false;
        }
        RoadmConnections newRdmConn = new RoadmConnectionsBuilder(rdmConnOpt.get())
            .setOpticalControlMode(mode)
            .setTargetOutputPower(powerValue == null ? null : new PowerDBm(powerValue))
            .build();

        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                LOG.error("Transaction for device {} was not found!", deviceId);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get transaction for device {}!", deviceId, e);
            return false;
        }

        // post the cross connect on the device
        InstanceIdentifier<RoadmConnections> roadmConnIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(ctNumber));
        deviceTx.put(LogicalDatastoreType.CONFIGURATION, roadmConnIID, newRdmConn);
        FluentFuture<? extends @NonNull CommitInfo> commit =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Roadm connection power level successfully set ");
            return true;
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Failed to post {}", newRdmConn, ex);
            return false;
        }
    }

}
