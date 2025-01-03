/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.crossconnect;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.RpcService;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrail;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.connection.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.connection.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
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

    public Optional<String> postCrossConnect(
            String deviceId, String srcTp, String destTp, SpectrumInformation spectrumInformation) {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isEmpty()) {
                LOG.error("Device transaction for device {} was not found!", deviceId);
                return Optional.empty();
            }
            deviceTx = deviceTxOpt.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to obtain device transaction for device {}!", deviceId, e);
            return Optional.empty();
        }
        String connectionNumber = spectrumInformation.getIdentifierFromParams(srcTp, destTp);
        RoadmConnections rdmConn = new RoadmConnectionsBuilder()
            .setConnectionNumber(connectionNumber)
            .setWavelengthNumber(spectrumInformation.getWaveLength())
            .setOpticalControlMode(OpticalControlMode.Off)
            .setSource(
                new SourceBuilder().setSrcIf(spectrumInformation.getIdentifierFromParams(srcTp)).build())
            .setDestination(
                new DestinationBuilder().setDstIf(spectrumInformation.getIdentifierFromParams(destTp)).build())
            .build();
        // post the cross connect on the device
        deviceTx.merge(
            LogicalDatastoreType.CONFIGURATION,
            DataObjectIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber))
                .build(),
            rdmConn);
        FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Roadm-connection successfully created: {}-{}-{}-{}", srcTp, destTp,
                    spectrumInformation.getLowerSpectralSlotNumber(),
                    spectrumInformation.getHigherSpectralSlotNumber());
            return Optional.of(connectionNumber);
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to post {}. Exception: ", rdmConn, e);
        }
        return Optional.empty();
    }

    public List<String> deleteCrossConnect(String deviceId, String connectionNumber) {
        Optional<RoadmConnections> xc = getCrossConnect(deviceId, connectionNumber);
        //Check if cross connect exists before delete
        if (xc.isEmpty()) {
            LOG.warn("Cross connect does not exist, halting delete");
            return null;
        }
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isEmpty()) {
                LOG.error("Device transaction for device {} was not found!", deviceId);
                return null;
            }
            deviceTx = deviceTxOpt.orElseThrow();
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
            return new ArrayList<>(List.of(
                xc.orElseThrow().getSource().getSrcIf(),
                xc.orElseThrow().getDestination().getDstIf()));
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete {}", connectionNumber, e);
        }
        return null;
    }


    public List<Ports> getConnectionPortTrail(String nodeId, String srcTp, String destTp,
            int lowerSpectralSlotNumber, int higherSpectralSlotNumber) throws OpenRoadmInterfaceException {
        Optional<MountPoint> mountPointOpt = deviceTransactionManager.getDeviceMountPoint(nodeId);
        if (mountPointOpt.isEmpty()) {
            LOG.error("Failed to obtain mount point for device {}!", nodeId);
            return Collections.emptyList();
        }
        String connectionName = generateConnectionNumber(srcTp, destTp,
            String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber), String.valueOf(higherSpectralSlotNumber)));
        MountPoint mountPoint = mountPointOpt.orElseThrow();
        final Optional<RpcService> service = mountPoint.getService(RpcService.class);
        if (service.isEmpty()) {
            LOG.error("Failed to get RpcService for node {}", nodeId);
        }
        final GetConnectionPortTrail rpcService = service.orElseThrow().getRpc(GetConnectionPortTrail.class);
        final Future<RpcResult<GetConnectionPortTrailOutput>> portTrailOutput = rpcService.invoke(
                new GetConnectionPortTrailInputBuilder().setConnectionNumber(connectionName).build());
        if (portTrailOutput == null) {
            LOG.warn("Port trail is null in getConnectionPortTrail for nodeId {}", nodeId);
            return Collections.emptyList();
        }
        List<Ports> ports = null;
        try {
            RpcResult<GetConnectionPortTrailOutput> connectionPortTrailOutputRpcResult = portTrailOutput.get();
            GetConnectionPortTrailOutput connectionPortTrailOutput = connectionPortTrailOutputRpcResult.getResult();
            if (connectionPortTrailOutput == null) {
                throw new OpenRoadmInterfaceException(String.format(
                    "RPC get connection port trail called on node %s returned null!", nodeId));
            }
            LOG.info("Getting port trail for node {}'s connection number {}", nodeId, connectionName);
            ports = connectionPortTrailOutput.getPorts();
            for (Ports port : ports) {
                LOG.info("{} - Circuit pack {} - Port {}", nodeId, port.getCircuitPackName(), port.getPortName());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception caught", e);
        }
        return ports != null ? ports : Collections.emptyList();
    }

    private DataObjectIdentifier<RoadmConnections> generateRdmConnectionIID(String connectionNumber) {
        return DataObjectIdentifier.builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber))
            .build();
    }

    private String generateConnectionNumber(String srcTp, String destTp, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, srcTp, destTp, spectralSlotName);
    }

    public boolean setPowerLevel(String deviceId, OpticalControlMode mode, Decimal64 powerValue, String ctNumber) {
        Optional<RoadmConnections> rdmConnOpt = getCrossConnect(deviceId, ctNumber);
        if (rdmConnOpt.isEmpty()) {
            LOG.warn("Roadm-Connection is null in set power level ({})", ctNumber);
            return false;
        }
        RoadmConnections newRdmConn =
            powerValue == null
                ? new RoadmConnectionsBuilder(rdmConnOpt.orElseThrow())
                    .setOpticalControlMode(mode)
                    .build()
                : new RoadmConnectionsBuilder(rdmConnOpt.orElseThrow())
                    .setOpticalControlMode(mode)
                    .setTargetOutputPower(new PowerDBm(powerValue))
                    .build();
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isEmpty()) {
                LOG.error("Transaction for device {} was not found!", deviceId);
                return false;
            }
            deviceTx = deviceTxOpt.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get transaction for device {}!", deviceId, e);
            return false;
        }
        // post the cross connect on the device
        deviceTx.merge(
            LogicalDatastoreType.CONFIGURATION,
            DataObjectIdentifier.builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(ctNumber))
                .build(),
            newRdmConn);
        FluentFuture<? extends @NonNull CommitInfo> commit =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Roadm connection power level successfully set ");
            return true;
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Failed to post {}", newRdmConn, ex);
        }
        return false;
    }
}
