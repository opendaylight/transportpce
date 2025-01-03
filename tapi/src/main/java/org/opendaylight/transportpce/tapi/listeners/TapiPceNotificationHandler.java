/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiPceNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiPceNotificationHandler.class);

    private ServicePathRpcResult servicePathRpcResult;
    private CreateConnectivityServiceInput input;
    private Uuid serviceUuid;
    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final ConnectivityUtils connectivityUtils;
    private final Map<org.opendaylight.yang.gen.v1.urn
        .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey,
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection>
        connectionFullMap; // this variable is for complete connection objects

    public TapiPceNotificationHandler(DataBroker dataBroker, ConnectivityUtils connecUtil) {
        this.connectionFullMap = new HashMap<>();
        this.dataBroker = dataBroker;
        this.networkTransactionService = new NetworkTransactionImpl(this.dataBroker);
        this.connectivityUtils = connecUtil;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(ServicePathRpcResult.class, this::onServicePathRpcResult)));
    }

    private void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (compareServicePathRpcResult(notification)) {
            LOG.warn("ServicePathRpcResult already wired !");
            return;
        }
        servicePathRpcResult = notification;
        switch (servicePathRpcResult.getNotificationType().getIntValue()) {
            /* path-computation-request. */
            case 1:
                onPathComputationResult(notification);
                break;
            /* cancel-resource-reserve. */
            case 2:
                onCancelResourceResult(notification.getServiceName());
                break;
            default:
                break;
        }
    }

    /**
     * Process path computation request result.
     * @param notification the result notification.
     */
    private void onPathComputationResult(ServicePathRpcResult notification) {
        this.connectionFullMap.clear();
        LOG.info("PCE '{}' Notification received : {}",servicePathRpcResult.getNotificationType().getName(),
            notification);
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.error("PCE path computation failed !");
            return;
        } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE path computation returned a Penging RpcStatusEx code!");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful) {
            LOG.error("PCE path computation returned an unknown RpcStatusEx code!");
            return;
        }

        LOG.info("PCE calculation done OK !");
        if (servicePathRpcResult.getPathDescription() == null) {
            LOG.error("'PathDescription' parameter is null ");
            return;
        }
        PathDescription pathDescription = new PathDescriptionBuilder()
            .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
            .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection())
            .build();
        LOG.info("PathDescription for TAPI gets : {}", pathDescription);
        if (input == null) {
            LOG.error("Input is null !");
            return;
        }
        // TODO: check kind of service: based on the device Id of the input,
        //  verify the type of XPDR and the capacity and determine if it is an OTN service or pure WDM service
        // Create connections and ceps for the connectivity service.
        //  Connections must be with a locked stated. As the renderer hasnt implemented yet the oc's
        Map<ConnectionKey, Connection> connectionMap = connectivityUtils.createConnectionsFromService(
                pathDescription, input.getLayerProtocolName());
        this.connectionFullMap.putAll(connectivityUtils.getConnectionFullMap());
        LOG.debug("Connection Map from createConnectionsAndCepsForService is {}, LAYERPROTOCOL of service is {} ",
            connectionMap.toString(), input.getLayerProtocolName());
        // add connections to connection context and to connectivity context
        updateConnectionContextWithConn(this.connectionFullMap, connectionMap, serviceUuid);
    }

    /**
     * Process cancel resource result.
     * @param serviceName Service name to build uuid.
     */
    private void onCancelResourceResult(String serviceName) {
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.info("PCE cancel resource failed !");
            return;
        } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE cancel returned a Penging RpcStatusEx code!");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful) {
            LOG.error("PCE cancel returned an unknown RpcStatusEx code!");
            return;
        }
        LOG.info("PCE cancel resource done OK !");
        Uuid suuid = new Uuid(UUID.nameUUIDFromBytes(serviceName.getBytes(Charset.forName("UTF-8")))
            .toString());
        // get connections of connectivity service and remove them from tapi context and then remove
        //  service from context. The CEPs are maintained as they could be reused by another service
        ConnectivityService connService = getConnectivityService(suuid);
        if (connService == null) {
            LOG.error("Service doesnt exist in tapi context");
            return;
        }
        for (Connection connection:connService.getConnection().values()) {
            deleteConnection(connection.getConnectionUuid());
        }
        deleteConnectivityService(suuid);
    }

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        if (servicePathRpcResult == null) {
            return false;
        }
        if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (servicePathRpcResult.getStatus() != notification.getStatus()) {
            return false;
        }
        if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
    }


    public void updateTopologyWithCep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, ConnectionEndPoint cep) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        DataObjectIdentifier<OwnedNodeEdgePoint> onepIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (!optionalOnep.isPresent()) {
                LOG.error("ONEP is not present in datastore");
                return;
            }
            OwnedNodeEdgePoint onep = optionalOnep.orElseThrow();
            LOG.info("ONEP found = {}", onep.toString());
            // TODO -> If cep exists -> skip merging to datasore
            OwnedNodeEdgePoint1 onep1 = onep.augmentation(OwnedNodeEdgePoint1.class);
            if (onep1 != null && onep1.getCepList() != null && onep1.getCepList().getConnectionEndPoint() != null) {
                if (onep1.getCepList().getConnectionEndPoint().containsKey(
                        new org.opendaylight.yang.gen.v1
                            .urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey(cep.key()))) {
                    LOG.info("CEP already in topology, skipping merge");
                    return;
                }
            }
            // Updated ONEP
            CepList cepList = new CepListBuilder().setConnectionEndPoint(Map.of(cep.key(), cep)).build();
            OwnedNodeEdgePoint1 onep1Bldr = new OwnedNodeEdgePoint1Builder().setCepList(cepList).build();
            OwnedNodeEdgePoint newOnep = new OwnedNodeEdgePointBuilder(onep)
                .addAugmentation(onep1Bldr)
                .build();
            LOG.info("New ONEP is {}", newOnep.toString());
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID,
                newOnep);
            this.networkTransactionService.commit().get();
            LOG.info("CEP added successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt update cep in topology", e);
        }
    }

    public void updateTopologyWithNep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, OwnedNodeEdgePoint onep) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        DataObjectIdentifier<OwnedNodeEdgePoint> onepIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                LOG.error("ONEP is already present in datastore");
                return;
            }
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID,
                onep);
            this.networkTransactionService.commit().get();
            LOG.info("NEP {} added successfully.", onep.getName().toString());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt put NEP {} in topology, error = ", onep.getName().toString(), e);
        }
    }


    private void updateConnectionContextWithConn(
            Map<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey,
                org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection> connFullMap,
            Map<ConnectionKey, Connection> connMap, Uuid suuid) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            ConnectivityService connServ = getConnectivityService(suuid);
            ConnectivityService updtConnServ = new ConnectivityServiceBuilder(connServ)
                .setConnection(connMap)
                .build();

            // Perform the merge operation with the new conn service and the connection context updated
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext
                connectivityContext = new ConnectivityContextBuilder()
                    .setConnectivityService(Map.of(updtConnServ.key(), updtConnServ))
                    .setConnection(connFullMap)
                    .build();
            DataObjectIdentifier<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext> connectivitycontextIID =
                DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                .build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectivitycontextIID,
                connectivityContext);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI connectivity merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI connectivity", e);
        }
    }

    private ConnectivityService getConnectivityService(Uuid suuid) {
        try {
            // First read connectivity service with service uuid and update info
            DataObjectIdentifier<ConnectivityService> connectivityServIID = DataObjectIdentifier.builder(Context.class)
                    .augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(suuid))
                    .build();

            Optional<ConnectivityService> optConnServ =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityServIID).get();
            if (optConnServ.isEmpty()) {
                LOG.error("Connectivity service not found in tapi context");
                return null;
            }
            return optConnServ.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connectivity service not found in tapi context. Error:", e);
            return null;
        }
    }

    private void deleteConnectivityService(Uuid suuid) {
        // First read connectivity service with service uuid and update info
        DataObjectIdentifier<ConnectivityService> connectivityServIID = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                .child(ConnectivityService.class, new ConnectivityServiceKey(suuid))
                .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectivityServIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI connectivity service", e);
        }
    }

    private void deleteConnection(Uuid connectionUuid) {
        // First read connectivity service with service uuid and update info
        DataObjectIdentifier<org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection> connectionIID =
            DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection.class,
                    new org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey(
                        connectionUuid))
                .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectionIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI connection", e);
        }
    }

    public void setInput(CreateConnectivityServiceInput input) {
        this.input = input;
    }

    public void setServiceUuid(Uuid serviceUuid) {
        this.serviceUuid = serviceUuid;
    }
}