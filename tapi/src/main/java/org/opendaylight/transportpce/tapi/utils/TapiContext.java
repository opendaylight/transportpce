/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.nio.charset.Charset;
//import java.util.ArrayList;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point
//.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.NwTopologyServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiContext {

    private static final Logger LOG = LoggerFactory.getLogger(TapiContext.class);
    public static final String TAPI_CONTEXT = "T-API context";
    public static final String NODE_NOT_PRESENT = "Node is not present in datastore";
    private final NetworkTransactionService networkTransactionService;

    public TapiContext(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
        createTapiContext();
    }

    private void createTapiContext() {
        try {
            // Augmenting tapi context to include topology and connectivity contexts
            Name contextName = new NameBuilder().setValue(TAPI_CONTEXT).setValueName("TAPI Context Name").build();

            Context1 connectivityContext =
                new Context1Builder()
                    .setConnectivityContext(
                        new ConnectivityContextBuilder()
                            .setConnection(new HashMap<>())
                            .setConnectivityService(new HashMap<>())
                            .build())
                    .build();

            Name nwTopoServiceName =
                new NameBuilder()
                    .setValue("Network Topo Service")
                    .setValueName("Network Topo Service Name")
                    .build();

            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1 topologyContext
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1Builder()
                    .setTopologyContext(new TopologyContextBuilder()
                        .setNwTopologyService(new NwTopologyServiceBuilder()
                            .setTopology(new HashMap<>())
                            .setUuid(
                                new Uuid(
                                    UUID.nameUUIDFromBytes("Network Topo Service".getBytes(Charset.forName("UTF-8")))
                                        .toString()))
                            .setName(Map.of(nwTopoServiceName.key(), nwTopoServiceName))
                            .build())
                        .setTopology(new HashMap<>())
                        .build())
                    .build();

            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1 notificationContext
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1Builder()
                    .setNotificationContext(new NotificationContextBuilder()
                        .setNotification(new HashMap<>())
                        .setNotifSubscription(new HashMap<>())
                        .build())
                    .build();

            ContextBuilder contextBuilder = new ContextBuilder()
                    .setName(Map.of(contextName.key(), contextName))
                    .setUuid(
                        new Uuid(UUID.nameUUIDFromBytes(TAPI_CONTEXT.getBytes(Charset.forName("UTF-8"))).toString()))
                    .setServiceInterfacePoint(new HashMap<>())
                    .addAugmentation(connectivityContext)
                    .addAugmentation(topologyContext)
                    .addAugmentation(notificationContext);

            // todo: add notification context
            InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
            // put in datastore
            this.networkTransactionService.put(LogicalDatastoreType.OPERATIONAL, contextIID, contextBuilder.build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI context created successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create TAPI context", e);
        }
    }

    public Context getTapiContext() {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        //  There is no Identifiable in Context model
        InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
        try {
            Optional<Context> optionalContext = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL,
                    contextIID).get();
            if (!optionalContext.isPresent()) {
                LOG.error("Tapi context is not present in datastore");
                return null;
            }
            return optionalContext.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read tapi context from datastore", e);
            return null;
        }
    }

    public void deleteTapiContext() {

    }

    public void updateTopologyContext(Map<TopologyKey, Topology> topologyMap) {
        // TODO: solve error when merging: Topology is not a valid child of topology context?
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            TopologyContext topologyContext = new TopologyContextBuilder()
                    //.setNwTopologyService(new NwTopologyServiceBuilder().build())
                    .setTopology(topologyMap)
                    .build();
            InstanceIdentifier<TopologyContext> topologycontextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.topology.rev221121.Context1.class)
                            .child(TopologyContext.class)
                            .build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topologycontextIID,
                    topologyContext);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI topology merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI topology", e);
        }
    }

    public void updateSIPContext(Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            ContextBuilder contextBuilder = new ContextBuilder().setServiceInterfacePoint(sipMap);
            InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, contextIID,
                    contextBuilder.build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI SIPs merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI SIPs", e);
        }
    }

    public void updateConnectivityContext(Map<ConnectivityServiceKey, ConnectivityService> connServMap,
                                          Map<ConnectionKey, Connection> connectionFullMap) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext
                connectivityContext = new ConnectivityContextBuilder()
                .setConnectivityService(connServMap)
                .setConnection(connectionFullMap)
                .build();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext> connectivitycontextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                        .build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectivitycontextIID,
                connectivityContext);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI connectivity merged successfully.");
            LOG.debug("TAPI connectivity merged successfully for services {}", connServMap.entrySet().iterator()
                    .next().getKey().toString());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI connectivity", e);
        }
    }

    public void updateTopologyWithCep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, ConnectionEndPoint cep) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
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
            LOG.info("ONEP found = {}", onep);
            // TODO -> If cep exists -> skip merging to datasore
            OwnedNodeEdgePoint1 onep1 = onep.augmentation(OwnedNodeEdgePoint1.class);
            if (onep1 != null && onep1.getCepList() != null && onep1.getCepList().getConnectionEndPoint() != null
                    && onep1.getCepList().getConnectionEndPoint().containsKey(new ConnectionEndPointKey(cep.key()))) {
                LOG.info("CEP already in topology, skipping merge");
                return;
            }
            // Updated ONEP
            CepList cepList = new CepListBuilder().setConnectionEndPoint(Map.of(cep.key(), cep)).build();
            OwnedNodeEdgePoint1 onep1Bldr = new OwnedNodeEdgePoint1Builder().setCepList(cepList).build();
            OwnedNodeEdgePoint newOnep = new OwnedNodeEdgePointBuilder(onep)
                    .addAugmentation(onep1Bldr)
                    .build();
            LOG.info("New ONEP is {}", newOnep);
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID,
                    newOnep);
            this.networkTransactionService.commit().get();
            LOG.info("CEP added successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt update cep in topology", e);
        }
    }

    public Node getTapiNode(Uuid topoUuid, Uuid nodeUuid) {
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                        new NodeKey(nodeUuid)).build();
        try {
            Optional<Node> optNode = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, nodeIID)
                    .get();
            if (!optNode.isPresent()) {
                LOG.error(NODE_NOT_PRESENT);
                return null;
            }
            // TODO -> Need to remove CEPs from NEPs. If not error from get Topology details output
            Node node = optNode.orElseThrow();
            LOG.debug("NEPs of node before creating map to be returned to the getTapiNode function = {}",
                node.getOwnedNodeEdgePoint().size());
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
            for (OwnedNodeEdgePoint onep: node.getOwnedNodeEdgePoint().values()) {
                if (onep.augmentation(OwnedNodeEdgePoint1.class) == null) {
                    onepMap.put(onep.key(), onep);
                    continue;
                }
                OwnedNodeEdgePointBuilder newOnepBuilder = new OwnedNodeEdgePointBuilder()
                    .setUuid(onep.getUuid())
                    .setLayerProtocolName(onep.getLayerProtocolName())
                    .setName(onep.getName())
                    .setSupportedCepLayerProtocolQualifierInstances(
                        onep.getSupportedCepLayerProtocolQualifierInstances())
                    .setAdministrativeState(onep.getAdministrativeState())
                    .setOperationalState(onep.getOperationalState())
                    .setLifecycleState(onep.getLifecycleState())
//                    .setTerminationDirection(onep.getTerminationDirection())
//                    .setTerminationState(onep.getTerminationState())
                    .setDirection(onep.getDirection())
                    .setLinkPortRole(onep.getLinkPortRole());
                if (onep.getMappedServiceInterfacePoint() != null) {
                    newOnepBuilder.setMappedServiceInterfacePoint(onep.getMappedServiceInterfacePoint());
                }
                OwnedNodeEdgePoint newOnep = newOnepBuilder.build();
                onepMap.put(newOnep.key(), newOnep);
            }
            LOG.debug("NEPs of node after creating map to be returned to the getTapiNode function = {}",
                onepMap.size());
            return new NodeBuilder(node)
                .setOwnedNodeEdgePoint(onepMap)
                .build();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read node in topology", e);
            return null;
        }
    }

    public OwnedNodeEdgePoint getTapiNEP(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid) {
        InstanceIdentifier<OwnedNodeEdgePoint> nepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                new NodeKey(nodeUuid)).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid)).build();
        try {
            Optional<OwnedNodeEdgePoint> optNode = this.networkTransactionService
                    .read(LogicalDatastoreType.OPERATIONAL, nepIID)
                    .get();
            if (!optNode.isPresent()) {
                LOG.error(NODE_NOT_PRESENT);
                return null;
            }
            return optNode.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read NEP in topology", e);
            return null;
        }
    }

    public Link getTapiLink(Uuid topoUuid, Uuid linkUuid) {
        InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(Link.class, new LinkKey(linkUuid)).build();
        try {
            Optional<Link> optLink = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, linkIID)
                    .get();
            if (!optLink.isPresent()) {
                LOG.error(NODE_NOT_PRESENT);
                return null;
            }
            return optLink.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read link in topology", e);
            return null;
        }
    }

    public Map<TopologyKey, Topology> getTopologyContext() {
        InstanceIdentifier<TopologyContext> topologycontextIID =
                InstanceIdentifier.builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.topology.rev221121.Context1.class)
                        .child(TopologyContext.class)
                        .build();
        try {
            Optional<TopologyContext> optTopoContext = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, topologycontextIID).get();
            if (!optTopoContext.isPresent()) {
                LOG.error("Topology context is not present in datastore");
                return null;
            }
            return optTopoContext.orElseThrow().getTopology();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read topology context", e);
            return null;
        }
    }

    public ConnectivityService getConnectivityService(Uuid serviceUuid) {
        try {
            // First read connectivity service with service uuid and update info
            InstanceIdentifier<ConnectivityService> connectivityServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(serviceUuid))
                    .build();

            Optional<ConnectivityService> optConnServ =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityServIID).get();
            if (!optConnServ.isPresent()) {
                LOG.error("Connectivity service not found in tapi context");
                return null;
            }
            return optConnServ.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connectivity service not found in tapi context. Error:", e);
            return null;
        }
    }

    public void deleteConnectivityService(Uuid serviceUuid) {
        // TODO: handle case where the infrastructure service is removed before the top level service?
        ConnectivityService connectivityService = getConnectivityService(serviceUuid);
        if (connectivityService == null) {
            LOG.error("Service doesnt exist in tapi context");
            return;
        }
        for (org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.Connection connection:
                    connectivityService.getConnection().values()) {
            deleteConnection(connection.getConnectionUuid(), serviceUuid, connectivityService.getLayerProtocolName());
        }
        InstanceIdentifier<ConnectivityService> connectivityServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                        .child(ConnectivityService.class, new ConnectivityServiceKey(serviceUuid))
                        .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectivityServIID);
            this.networkTransactionService.commit().get();
            LOG.info("Connectivity service deleted");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete Connectivity service", e);
        }
    }

    private void deleteConnection(Uuid connectionUuid, Uuid serviceUuid, LayerProtocolName serviceLayer) {
        // First read connectivity service with service uuid and update info
        InstanceIdentifier<org.opendaylight.yang.gen.v1
            .urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection> connectionIID =
            InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection.class,
                    new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey(
                        connectionUuid))
                .build();
        Connection connection = getConnection(connectionUuid);
        if (connection != null && isNotUsedByOtherService(connection, serviceUuid)) {
            Map<LowerConnectionKey, LowerConnection> lowerConnectionMap = connection.getLowerConnection();
            if (lowerConnectionMap != null) {
                for (LowerConnection lowerConnection:lowerConnectionMap.values()) {
                    // check layer of connection, for DSR service we only need to delete DSR layer
                    // connection and XC at ODU. For ODU, only need to delete ODU connections and for
                    // photonic media services all the photonic media. And when it is ETH we need to delete
                    // everything and also without checking the lower connection layer
                    Connection conn1 = getConnection(lowerConnection.getConnectionUuid());
                    if (conn1 == null) {
                        // connection not found in tapi context
                        continue;
                    }
                    LayerProtocolName lowerConnLayer = conn1.getLayerProtocolName();
                    switch (serviceLayer.getIntValue()) {
                        case 0:
                        case 3:
                            // PHOTONIC & ODU
                            if (lowerConnLayer.equals(serviceLayer)) {
                                deleteConnection(lowerConnection.getConnectionUuid(), serviceUuid, serviceLayer);
                            }
                            break;
                        case 1:
                            // ETH
                            deleteConnection(lowerConnection.getConnectionUuid(), serviceUuid, serviceLayer);
                            break;
                        case 2:
                            // DSR
                            if (lowerConnLayer.equals(serviceLayer) || (lowerConnLayer.equals(LayerProtocolName.ODU)
                                    && conn1.getName().values().stream().anyMatch(
                                            name -> name.getValue().contains("XC")))) {
                                deleteConnection(lowerConnection.getConnectionUuid(), serviceUuid, serviceLayer);
                            }
                            break;
                        default:
                            LOG.info("Unknown service Layer: {}", serviceLayer.getName());
                    }
                }
            }
        }
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectionIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI Connection", e);
        }
    }

    private boolean isNotUsedByOtherService(Connection connection, Uuid serviceUuid) {
        Map<ConnectivityServiceKey, ConnectivityService> connServicesMap = getConnectivityServices();
        if (connServicesMap == null) {
            LOG.info("isNotUsedByOtherService: No service in tapi context!");
            return true;
        }
        for (ConnectivityService connService: connServicesMap.values()) {
            if (connService.getConnection() == null || connService.getUuid().equals(serviceUuid)) {
                LOG.info("isNotUsedByOtherService: There are no connections in service {} or service in loop is the "
                        + "service to be deleted", connService.getUuid().getValue());
                continue;
            }
            if (connService.getConnection().containsKey(
                    new org.opendaylight.yang.gen.v1
                        .urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectionKey(
                            connection.getUuid()))) {
                LOG.info("isNotUsedByOtherService: Connection {} is in used by service {}. Cannot remove it from "
                        + "context", connection.getUuid().getValue(), connService.getUuid().getValue());
                return false;
            }
            LOG.info("isNotUsedByOtherService: Going to check lower connections");
            for (org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.Connection
                    conn:connService.getConnection().values()) {
                Connection connection1 = getConnection(conn.getConnectionUuid());
                if (connection1 == null || connection1.getLowerConnection() == null) {
                    continue;
                }
                if (connection1.getLowerConnection().containsKey(new LowerConnectionKey(connection.getUuid()))) {
                    LOG.info("isNotUsedByOtherService: Lower Connection {} is in used by service {}. Cannot remove it "
                            + "from context", connection.getUuid().getValue(), connService.getUuid().getValue());
                    return false;
                }
            }
        }
        LOG.info("isNotUsedByOtherService: No other service uses connection {}, therefore it can be safely deleted",
                connection.getUuid());
        return true;
    }

    public Connection getConnection(Uuid connectionUuid) {
        try {
            // First read connectivity service with service uuid and update info
            InstanceIdentifier<Connection> connIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .child(Connection.class, new ConnectionKey(connectionUuid))
                    .build();

            Optional<Connection> optConn =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connIID).get();
            if (!optConn.isPresent()) {
                LOG.error("Connection not found in tapi context");
                return null;
            }
            return optConn.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connection not found in tapi context. Error:", e);
            return null;
        }
    }

    public Map<ConnectivityServiceKey, ConnectivityService> getConnectivityServices() {
        try {
            // First read connectivity service with service uuid and update info
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext> connectivityContextIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .build();

            Optional<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext> optConnContext =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityContextIID)
                        .get();
            if (!optConnContext.isPresent()) {
                LOG.error("Connectivity context not found in tapi context");
                return null;
            }
            return optConnContext.orElseThrow().getConnectivityService();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connectivity context not found in tapi context. Error:", e);
            return null;
        }
    }

    public ConnectionEndPoint getTapiCEP(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, Uuid cepUuid) {
        InstanceIdentifier<OwnedNodeEdgePoint> nepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                new NodeKey(nodeUuid)).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid)).build();
        try {
            Optional<OwnedNodeEdgePoint> optNode = this.networkTransactionService
                .read(LogicalDatastoreType.OPERATIONAL, nepIID).get();
            if (!optNode.isPresent()) {
                LOG.error(NODE_NOT_PRESENT);
                return null;
            }
            if (optNode.orElseThrow().augmentation(OwnedNodeEdgePoint1.class) == null) {
                LOG.error("Node doesnt have ceps");
                return null;
            }
            return optNode.orElseThrow().augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                .get(new ConnectionEndPointKey(cepUuid));
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read node in topology", e);
            return null;
        }
    }
}
