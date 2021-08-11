/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.context.NotificationContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.NwTopologyServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiContext {

    private static final Logger LOG = LoggerFactory.getLogger(TapiContext.class);
    public static final String TAPI_CONTEXT = "T-API context";
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

            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1 topologyContext
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1Builder()
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

            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Context1 notificationContext
                = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Context1Builder()
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
            return optionalContext.get();
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
                            .onf.otcc.yang.tapi.topology.rev181210.Context1.class)
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
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext
                connectivityContext = new ConnectivityContextBuilder()
                .setConnectivityService(connServMap)
                .setConnection(connectionFullMap)
                .build();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext> connectivitycontextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
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

    public void updateTopologyWithCep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, ConnectionEndPoint cep) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
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
            OwnedNodeEdgePoint onep = optionalOnep.get();
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
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
                        new NodeKey(nodeUuid)).build();
        try {
            Optional<Node> optNode = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, nodeIID)
                    .get();
            if (!optNode.isPresent()) {
                LOG.error("Node is not present in datastore");
                return null;
            }
            // TODO -> Need to remove CEPs from NEPs. If not error from get Topology details output
            Node node = optNode.get();
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
                    .setSupportedCepLayerProtocolQualifier(onep.getSupportedCepLayerProtocolQualifier())
                    .setAdministrativeState(onep.getAdministrativeState())
                    .setOperationalState(onep.getOperationalState())
                    .setLifecycleState(onep.getLifecycleState())
                    .setTerminationDirection(onep.getTerminationDirection())
                    .setTerminationState(onep.getTerminationState())
                    .setLinkPortDirection(onep.getLinkPortDirection())
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
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
                new NodeKey(nodeUuid)).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid)).build();
        try {
            Optional<OwnedNodeEdgePoint> optNode = this.networkTransactionService
                    .read(LogicalDatastoreType.OPERATIONAL, nepIID)
                    .get();
            if (!optNode.isPresent()) {
                LOG.error("Node is not present in datastore");
                return null;
            }
            return optNode.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read NEP in topology", e);
            return null;
        }
    }

    public Link getTapiLink(Uuid topoUuid, Uuid linkUuid) {
        InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(Link.class, new LinkKey(linkUuid)).build();
        try {
            Optional<Link> optLink = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, linkIID)
                    .get();
            if (!optLink.isPresent()) {
                LOG.error("Node is not present in datastore");
                return null;
            }
            return optLink.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read link in topology", e);
            return null;
        }
    }

    public Map<TopologyKey, Topology> getTopologyContext() {
        InstanceIdentifier<TopologyContext> topologycontextIID =
                InstanceIdentifier.builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.topology.rev181210.Context1.class)
                        .child(TopologyContext.class)
                        .build();
        try {
            Optional<TopologyContext> optTopoContext = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, topologycontextIID).get();
            if (!optTopoContext.isPresent()) {
                LOG.error("Topology context is not present in datastore");
                return null;
            }
            return optTopoContext.get().getTopology();
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
                        .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(serviceUuid))
                    .build();

            Optional<ConnectivityService> optConnServ =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityServIID).get();
            if (!optConnServ.isPresent()) {
                LOG.error("Connectivity service not found in tapi context");
                return null;
            }
            return optConnServ.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connectivity service not found in tapi context. Error:", e);
            return null;
        }
    }

    public void deleteConnectivityService(Uuid serviceUuid) {
        ConnectivityService connectivityService = getConnectivityService(serviceUuid);
        if (connectivityService == null) {
            LOG.error("Service doesnt exist in tapi context");
            return;
        }
        for (org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection connection:
                    connectivityService.getConnection().values()) {
            deleteConnection(connection.getConnectionUuid());
        }
        InstanceIdentifier<ConnectivityService> connectivityServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
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

    private void deleteConnection(Uuid connectionUuid) {
        // First read connectivity service with service uuid and update info
        InstanceIdentifier<org.opendaylight.yang.gen.v1
            .urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection> connectionIID =
            InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection.class,
                    new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey(
                            connectionUuid))
                .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectionIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI Connection", e);
        }
    }

    public Connection getConnection(Uuid connectionUuid) {
        try {
            // First read connectivity service with service uuid and update info
            InstanceIdentifier<Connection> connIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                    .child(Connection.class, new ConnectionKey(connectionUuid))
                    .build();

            Optional<Connection> optConn =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connIID).get();
            if (!optConn.isPresent()) {
                LOG.error("Connection not found in tapi context");
                return null;
            }
            return optConn.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connection not found in tapi context. Error:", e);
            return null;
        }
    }

    public Map<ConnectivityServiceKey, ConnectivityService> getConnectivityServices() {
        try {
            // First read connectivity service with service uuid and update info
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext> connectivityContextIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                    .build();

            Optional<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext> optConnContext =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityContextIID)
                        .get();
            if (!optConnContext.isPresent()) {
                LOG.error("Connectivity context not found in tapi context");
                return null;
            }
            return optConnContext.get().getConnectivityService();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connectivity context not found in tapi context. Error:", e);
            return null;
        }
    }

    public ConnectionEndPoint getTapiCEP(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, Uuid cepUuid) {
        InstanceIdentifier<OwnedNodeEdgePoint> nepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
                new NodeKey(nodeUuid)).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid)).build();
        try {
            Optional<OwnedNodeEdgePoint> optNode = this.networkTransactionService
                .read(LogicalDatastoreType.OPERATIONAL, nepIID).get();
            if (!optNode.isPresent()) {
                LOG.error("Node is not present in datastore");
                return null;
            }
            if (optNode.get().augmentation(OwnedNodeEdgePoint1.class) == null) {
                LOG.error("Node doesnt have ceps");
                return null;
            }
            return optNode.get().augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                .get(new ConnectionEndPointKey(cepUuid));
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read node in topology");
            return null;
        }
    }
}
