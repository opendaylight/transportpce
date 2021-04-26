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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
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

            ContextBuilder contextBuilder = new ContextBuilder()
                    .setName(Map.of(contextName.key(), contextName))
                    .setUuid(
                        new Uuid(UUID.nameUUIDFromBytes(TAPI_CONTEXT.getBytes(Charset.forName("UTF-8"))).toString()))
                    .setServiceInterfacePoint(new HashMap<>())
                    .addAugmentation(connectivityContext)
                    .addAugmentation(topologyContext);

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
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext
                    topologyContext = new TopologyContextBuilder()
                    //.setNwTopologyService(new NwTopologyServiceBuilder().build())
                    .setTopology(topologyMap)
                    .build();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext> topologycontextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.topology.rev181210.Context1.class)
                            .child(org.opendaylight.yang.gen.v1.urn
                                    .onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
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
}
