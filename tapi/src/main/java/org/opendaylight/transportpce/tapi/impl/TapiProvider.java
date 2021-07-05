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
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.connectivity.TapiConnectivityImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiPortMappingListener;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyImpl;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.TransportpceServicehandlerListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TapiCommonService;
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
            .org.opendaylight.transportpce.portmapping.rev210426.network.Nodes.class);
    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderService;
    private ObjectRegistration<TapiConnectivityService> rpcRegistration;
    private ObjectRegistration<TransportpceTapinetworkutilsService> tapiNetworkutilsServiceRpcRegistration;
    private ListenerRegistration<TapiNetconfTopologyListener> dataTreeChangeListenerRegistration;
    private ListenerRegistration<TapiPortMappingListener> mappingListenerListenerRegistration;
    private ListenerRegistration<TransportpcePceListener> pcelistenerRegistration;
    private ListenerRegistration<TransportpceRendererListener> rendererlistenerRegistration;
    private ListenerRegistration<TransportpceServicehandlerListener> servicehandlerlistenerRegistration;
    private final OrgOpenroadmServiceService serviceHandler;
    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiListener tapiListener;
    private final TapiNetconfTopologyListener topologyListener;
    private TapiPortMappingListener tapiPortMappingListener;
    private final NetworkTransactionService networkTransactionService;
    private final TransportpceTapinetworkutilsService tapiNetworkUtils;
    private TapiPceListenerImpl pceListenerImpl;
    private TapiRendererListenerImpl rendererListenerImpl;
    private TapiServiceHandlerListenerImpl serviceHandlerListenerImpl;
    private final NotificationService notificationService;

    public TapiProvider(DataBroker dataBroker, RpcProviderService rpcProviderService,
            OrgOpenroadmServiceService serviceHandler, ServiceDataStoreOperations serviceDataStoreOperations,
            TapiListener tapiListener, NetworkTransactionService networkTransactionService,
            TapiNetconfTopologyListener topologyListener, TapiPortMappingListener tapiPortMappingListener,
            TransportpceTapinetworkutilsService tapiNetworkUtils, TapiPceListenerImpl pceListenerImpl,
            TapiRendererListenerImpl rendererListenerImpl, TapiServiceHandlerListenerImpl serviceHandlerListenerImpl,
            NotificationService notificationService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
        this.serviceHandler = serviceHandler;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiListener = tapiListener;
        this.networkTransactionService = networkTransactionService;
        this.topologyListener = topologyListener;
        this.tapiPortMappingListener = tapiPortMappingListener;
        this.tapiNetworkUtils = tapiNetworkUtils;
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
        this.serviceHandlerListenerImpl = serviceHandlerListenerImpl;
        this.notificationService = notificationService;
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

        TapiConnectivityImpl tapi = new TapiConnectivityImpl(this.serviceHandler, tapiContext, connectivityUtils,
                pceListenerImpl, rendererListenerImpl, serviceHandlerListenerImpl);
        TapiTopologyImpl topo = new TapiTopologyImpl(this.dataBroker, tapiContext, topologyUtils);
        rpcRegistration = rpcProviderService.registerRpcImplementation(TapiConnectivityService.class, tapi);
        rpcProviderService.registerRpcImplementation(TapiTopologyService.class, topo);
        rpcProviderService.registerRpcImplementation(TapiCommonService.class, topo);
        dataTreeChangeListenerRegistration =
            dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), topologyListener);
        mappingListenerListenerRegistration =
            dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                MAPPING_II), tapiPortMappingListener);
        tapiNetworkutilsServiceRpcRegistration =
                rpcProviderService.registerRpcImplementation(TransportpceTapinetworkutilsService.class,
                        this.tapiNetworkUtils);
        @NonNull
        InstanceIdentifier<ServiceInterfacePoints> sipIID = InstanceIdentifier.create(ServiceInterfacePoints.class);
        dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, sipIID), tapiListener);
        // Notification Listener
        pcelistenerRegistration = notificationService.registerNotificationListener(pceListenerImpl);
        rendererlistenerRegistration = notificationService.registerNotificationListener(rendererListenerImpl);
        servicehandlerlistenerRegistration =
                notificationService.registerNotificationListener(serviceHandlerListenerImpl);
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
        if (tapiNetworkutilsServiceRpcRegistration != null) {
            tapiNetworkutilsServiceRpcRegistration.close();
        }
        pcelistenerRegistration.close();
        rendererlistenerRegistration.close();
        servicehandlerlistenerRegistration.close();
        rpcRegistration.close();
    }
}
