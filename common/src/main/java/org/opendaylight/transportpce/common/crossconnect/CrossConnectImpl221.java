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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.GetConnectionPortTrail;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.GetConnectionPortTrailInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OduConnection.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.connection.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.connection.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.otn.renderer.nodes.Nodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossConnectImpl221 {

    private static final Logger LOG = LoggerFactory.getLogger(CrossConnectImpl221.class);
    private static final String DEV_TRANSACTION_NOT_FOUND = "Device transaction for device {} was not found!";
    private static final String UNABLE_DEV_TRANSACTION = "Unable to obtain device transaction for device {}!";
    private final DeviceTransactionManager deviceTransactionManager;

    public CrossConnectImpl221(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public Optional<RoadmConnections> getCrossConnect(String deviceId, String connectionNumber) {
        //TODO Change it to Operational later for real device
        return deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.CONFIGURATION,
                generateRdmConnectionIID(connectionNumber), Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }

    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.OduConnection> getOtnCrossConnect(
            String deviceId, String connectionNumber) {
        //TODO Change it to Operational later for real device
        return deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.CONFIGURATION,
                generateOduConnectionIID(connectionNumber), Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }

    public Optional<String> postCrossConnect(
            String deviceId, String srcTp, String destTp, SpectrumInformation spectrumInformation) {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isEmpty()) {
                LOG.error(DEV_TRANSACTION_NOT_FOUND, deviceId);
                return Optional.empty();
            }
            deviceTx = deviceTxOpt.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(UNABLE_DEV_TRANSACTION, deviceId, e);
            return Optional.empty();
        }
        String connectionNumber = spectrumInformation.getIdentifierFromParams(srcTp, destTp);
        RoadmConnections rdmConn = new RoadmConnectionsBuilder()
                .setConnectionName(connectionNumber)
                .setOpticalControlMode(OpticalControlMode.Off)
                .setSource(new SourceBuilder()
                        .setSrcIf(spectrumInformation.getIdentifierFromParams(srcTp,"nmc"))
                        .build())
                .setDestination(new DestinationBuilder()
                        .setDstIf(spectrumInformation.getIdentifierFromParams(destTp,"nmc"))
                        .build())
                .build();
        // post the cross connect on the device
        deviceTx.merge(
            LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier
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

    public List<String> deleteCrossConnect(String deviceId, String connectionName, boolean isOtn) {
        List<String> interfList = new ArrayList<>();
        Optional<RoadmConnections> xc = getCrossConnect(deviceId, connectionName);
        //Check if cross connect exists before delete
        if (xc.isPresent()) {
            interfList.add(xc.orElseThrow().getSource().getSrcIf());
            interfList.add(xc.orElseThrow().getDestination().getDstIf());
            interfList.add(xc.orElseThrow().getSource().getSrcIf().replace("nmc", "mc"));
            interfList.add(xc.orElseThrow().getDestination().getDstIf().replace("nmc", "mc"));
        } else {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                    .org.openroadm.device.container.org.openroadm.device.OduConnection> otnXc =
                getOtnCrossConnect(deviceId, connectionName);
            if (otnXc.isEmpty()) {
                LOG.warn("Cross connect {} does not exist, halting delete", connectionName);
                return null;
            }
            interfList.add(otnXc.orElseThrow().getSource().getSrcIf());
            interfList.add(otnXc.orElseThrow().getDestination().getDstIf());
        }
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isEmpty()) {
                LOG.error(DEV_TRANSACTION_NOT_FOUND, deviceId);
                return null;
            }
            deviceTx = deviceTxOpt.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(UNABLE_DEV_TRANSACTION, deviceId, e);
            return null;
        }

        // post the cross connect on the device
        deviceTx.delete(
            LogicalDatastoreType.CONFIGURATION,
            isOtn ? generateOduConnectionIID(connectionName) : generateRdmConnectionIID(connectionName));
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


    public List<Ports> getConnectionPortTrail(
            String nodeId, String srcTp, String destTp, int lowerSpectralSlotNumber, int higherSpectralSlotNumber)
            throws OpenRoadmInterfaceException {
        Optional<MountPoint> mountPointOpt = deviceTransactionManager.getDeviceMountPoint(nodeId);
        List<Ports> ports = null;
        if (mountPointOpt.isEmpty()) {
            LOG.error("Failed to obtain mount point for device {}!", nodeId);
            return Collections.emptyList();
        }
        MountPoint mountPoint = mountPointOpt.orElseThrow();
        final Optional<RpcService> service = mountPoint.getService(RpcService.class);
        if (service.isEmpty()) {
            LOG.error("Failed to get RpcService for node {}", nodeId);
        }
        String connectionName = generateConnectionName(srcTp, destTp,
            String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber), String.valueOf(higherSpectralSlotNumber)));
        GetConnectionPortTrail rpcService = service.orElseThrow().getRpc(GetConnectionPortTrail.class);
        final Future<RpcResult<GetConnectionPortTrailOutput>> portTrailOutput = rpcService.invoke(
                new GetConnectionPortTrailInputBuilder()
                    .setConnectionName(connectionName)
                    .build());
        if (portTrailOutput == null) {
            LOG.warn("Port trail is null in getConnectionPortTrail for nodeId {}", nodeId);
            return Collections.emptyList();
        }
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
        return ports == null ? Collections.emptyList() : ports;
    }


    public boolean setPowerLevel(String deviceId, OpticalControlMode mode, Decimal64 powerValue, String ctName) {
        Optional<RoadmConnections> rdmConnOpt = getCrossConnect(deviceId, ctName);
        if (rdmConnOpt.isEmpty()) {
            LOG.warn("Roadm-Connection is null in set power level ({})", ctName);
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
            InstanceIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(ctName))
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

    private InstanceIdentifier<RoadmConnections> generateRdmConnectionIID(String connectionNumber) {
        return InstanceIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber))
            .build();
    }

    private InstanceIdentifier<OduConnection> generateOduConnectionIID(String connectionNumber) {
        return InstanceIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(OduConnection.class, new OduConnectionKey(connectionNumber))
            .build();
    }

    private String generateConnectionName(String srcTp, String destTp, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,srcTp, destTp, spectralSlotName);
    }

    public Optional<String> postOtnCrossConnect(List<String> createdOduInterfaces, Nodes node) {
        String deviceId = node.getNodeId();
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isEmpty()) {
                LOG.error(DEV_TRANSACTION_NOT_FOUND, deviceId);
                return Optional.empty();
            }
            deviceTx = deviceTxOpt.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(UNABLE_DEV_TRANSACTION, deviceId, e);
            return Optional.empty();
        }
        List<String> sortedCreatedOduInterfaces = new ArrayList<>(createdOduInterfaces);
        sortedCreatedOduInterfaces.sort((s1,s2) -> s1.compareTo(s2));
        String srcTp = sortedCreatedOduInterfaces.get(0);
        String dstTp = sortedCreatedOduInterfaces.get(1);
        // Strip the service name from the src and dst
        String oduXConnectionName = srcTp.split(":")[0] + "-x-" + dstTp.split(":")[0];
        OduConnection oduConnection = new OduConnectionBuilder()
                .setConnectionName(oduXConnectionName)
                .setDestination(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                        .odu.connection.DestinationBuilder().setDstIf(dstTp).build())
                .setSource(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                        .odu.connection.SourceBuilder().setSrcIf(srcTp).build())
                .setDirection(Direction.Bidirectional)
                .build();
        // post the cross connect on the device
        deviceTx.merge(
            LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                .child(OduConnection.class, new OduConnectionKey(oduXConnectionName))
                .build(),
            oduConnection);
        FluentFuture<? extends @NonNull CommitInfo> commit =
                deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Otn-connection successfully created: {}", oduXConnectionName);
            return Optional.of(oduXConnectionName);
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to post {}.", oduConnection, e);
        }
        return Optional.empty();
    }
}
