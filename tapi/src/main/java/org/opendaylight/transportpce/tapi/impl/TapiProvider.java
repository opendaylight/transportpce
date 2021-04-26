/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import java.util.HashMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.connectivity.TapiConnectivityImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiPortMappingListener;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyImpl;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.ServiceInterfacePoints;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register TAPI interface Service and Notification.
 *
 * @author Gilles Thouenon (gilles.thouenon@orange.com) on behalf of Orange
 *
 */
public class TapiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TapiProvider.class);

    private static final InstanceIdentifier<Nodes> MAPPING_II = InstanceIdentifier.create(Network.class)
        .child(org.opendaylight.yang.gen.v1.http
            .org.opendaylight.transportpce.portmapping.rev210315.network.Nodes.class);
    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderService;
    private ObjectRegistration<TapiConnectivityService> rpcRegistration;
    private ListenerRegistration<TapiNetconfTopologyListener> dataTreeChangeListenerRegistration;
    private ListenerRegistration<TapiPortMappingListener> mappingListenerListenerRegistration;
    private final OrgOpenroadmServiceService serviceHandler;
    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiListener tapiListener;
    private final TapiNetconfTopologyListener topologyListener;
    private TapiPortMappingListener tapiPortMappingListener;
    private final NetworkTransactionService networkTransactionService;

    public TapiProvider(DataBroker dataBroker, RpcProviderService rpcProviderService,
            OrgOpenroadmServiceService serviceHandler, ServiceDataStoreOperations serviceDataStoreOperations,
            TapiListener tapiListener, NetworkTransactionService networkTransactionService,
            TapiNetconfTopologyListener topologyListener, TapiPortMappingListener tapiPortMappingListener) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
        this.serviceHandler = serviceHandler;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiListener = tapiListener;
        this.networkTransactionService = networkTransactionService;
        this.topologyListener = topologyListener;
        this.tapiPortMappingListener = tapiPortMappingListener;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("TapiProvider Session Initiated");
        TapiContext tapiContext = new TapiContext(this.networkTransactionService);
        LOG.info("Empty TAPI context created: {}", tapiContext.getTapiContext());

        TopologyUtils topologyUtils = new TopologyUtils(this.networkTransactionService, this.dataBroker);
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(this.serviceDataStoreOperations, new HashMap<>(),
                tapiContext);
        TapiInitialORMapping tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
                tapiContext, this.serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        tapiInitialORMapping.performServInitialMapping();

        TapiConnectivityImpl tapi = new TapiConnectivityImpl(this.serviceHandler);
        TapiTopologyImpl topo = new TapiTopologyImpl(this.dataBroker, tapiContext, topologyUtils);
        rpcRegistration = rpcProviderService.registerRpcImplementation(TapiConnectivityService.class, tapi);
        rpcProviderService.registerRpcImplementation(TapiTopologyService.class, topo);
        dataTreeChangeListenerRegistration =
            dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), topologyListener);
        mappingListenerListenerRegistration =
            dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                MAPPING_II), tapiPortMappingListener);
        @NonNull
        InstanceIdentifier<ServiceInterfacePoints> sipIID = InstanceIdentifier.create(ServiceInterfacePoints.class);
        dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, sipIID), tapiListener);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("TapiProvider Session Closed");
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
        if (mappingListenerListenerRegistration != null) {
            mappingListenerListenerRegistration.close();
        }
        rpcRegistration.close();
    }
}
