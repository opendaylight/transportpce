/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.impl.rpc.CreateConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.DeleteConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectionDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectivityServiceDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectivityServiceListImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetLinkDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeEdgePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointListImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyListImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiNetworkModelNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceNotificationHandler;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.transportpce.tapi.topology.TapiOrLinkListener;
import org.opendaylight.transportpce.tapi.topology.TapiPortMappingListener;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.transportpce.tapi.utils.TapiTopoContextInit;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.ServiceInterfacePoints;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register TAPI interface Service and Notification.
 *
 * @author Gilles Thouenon (gilles.thouenon@orange.com) on behalf of Orange
 *
 */
@Component
public class TapiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TapiProvider.class);

    private static final InstanceIdentifier<Nodes> MAPPING_II = InstanceIdentifier.create(Network.class)
        .child(org.opendaylight.yang.gen.v1.http
            .org.opendaylight.transportpce.portmapping.rev231221.network.Nodes.class);
    private static final InstanceIdentifier<Link> LINK_II = InstanceIdentifier.create(Networks.class).child(
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network.class,
            new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).augmentation(Network1.class)
        .child(Link.class);
    public static final Uuid TAPI_TOPO_UUID = new Uuid(UUID.nameUUIDFromBytes(
        TapiStringConstants.T0_FULL_MULTILAYER.getBytes(StandardCharsets.UTF_8)).toString());
    public static final String TOPOLOGICAL_MODE = "Full";
    private final DataBroker dataBroker;
    private final NetworkModelService netModServ;
    private final NetworkTransactionService networkTransactionService;
    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private List<Registration> listeners;
    private Registration rpcRegistration;
    private Registration pcelistenerRegistration;
    private Registration rendererlistenerRegistration;
    private Registration servicehandlerlistenerRegistration;
    private Registration tapinetworkmodellistenerRegistration;

    @Activate
    public TapiProvider(@Reference DataBroker dataBroker,
            @Reference RpcProviderService rpcProviderService,
            @Reference RpcService rpcService,
            @Reference NotificationService notificationService,
            @Reference NotificationPublishService notificationPublishService,
            @Reference NetworkTransactionService networkTransactionService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference NetworkModelService networkModelService,
            @Reference TapiNetworkModelNotificationHandler tapiNetworkModelNotificationHandler,
            @Reference TapiNetworkModelService tapiNetworkModelServiceImpl,
            @Reference TapiLink tapiLink,
            @Reference TapiContext tapiContext) {
        this.dataBroker = dataBroker;
        this.networkTransactionService = networkTransactionService;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.netModServ = networkModelService;
        netModServ.createTapiExtNodeAtInit();
        LOG.info("TapiProvider Session Initiated");
        LOG.info("Empty TAPI context created: {}", tapiContext.getTapiContext());
        TopologyUtils topologyUtils = new TopologyUtils(this.networkTransactionService, this.dataBroker, tapiLink);
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(this.serviceDataStoreOperations, new HashMap<>(),
                tapiContext, this.networkTransactionService, TAPI_TOPO_UUID);
        TapiTopoContextInit tapiTopoContextInit = new TapiTopoContextInit(tapiContext,this.networkTransactionService);
        tapiTopoContextInit.initializeTopoContext();
        TapiInitialORMapping tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
                tapiContext, this.serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        tapiInitialORMapping.performServInitialMapping();
        TapiPceNotificationHandler pceListenerImpl = new TapiPceNotificationHandler(dataBroker, connectivityUtils);
        TapiRendererNotificationHandler rendererListenerImpl = new TapiRendererNotificationHandler(dataBroker,
                notificationPublishService);

        rpcRegistration = rpcProviderService.registerRpcImplementations(
                new CreateConnectivityServiceImpl(rpcService, tapiContext, connectivityUtils, pceListenerImpl,
                        rendererListenerImpl),
                new GetConnectivityServiceDetailsImpl(tapiContext),
                new GetConnectionDetailsImpl(tapiContext),
                new DeleteConnectivityServiceImpl(rpcService, tapiContext, networkTransactionService),
                new GetConnectivityServiceListImpl(tapiContext),
                new GetNodeDetailsImpl(tapiContext),
                new GetTopologyDetailsImpl(tapiContext, topologyUtils, tapiLink, networkTransactionService),
                new GetNodeEdgePointDetailsImpl(tapiContext),
                new GetLinkDetailsImpl(tapiContext),
                new GetTopologyListImpl(tapiContext),
                new GetServiceInterfacePointDetailsImpl(tapiContext),
                new GetServiceInterfacePointListImpl(tapiContext));

        this.listeners = new ArrayList<>();
        TapiNetconfTopologyListener topologyListener = new TapiNetconfTopologyListener(tapiNetworkModelServiceImpl);
        TapiOrLinkListener orLinkListener = new TapiOrLinkListener(tapiLink, networkTransactionService);
        TapiPortMappingListener tapiPortMappingListener = new TapiPortMappingListener(tapiNetworkModelServiceImpl);
        listeners.add(dataBroker.registerTreeChangeListener(
                DataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, LINK_II), orLinkListener));
        listeners.add(dataBroker.registerTreeChangeListener(
                DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, InstanceIdentifiers.NETCONF_TOPOLOGY_II
                    .child(Node.class)),
                topologyListener));
        listeners.add(dataBroker.registerTreeChangeListener(
                DataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, MAPPING_II), tapiPortMappingListener));
        TapiListener tapiListener = new TapiListener();
        listeners.add(dataBroker.registerTreeChangeListener(
                DataTreeIdentifier.of(
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(ServiceInterfacePoints.class)),
                tapiListener));
        // Notification Listener
        pcelistenerRegistration = notificationService.registerCompositeListener(pceListenerImpl.getCompositeListener());
        rendererlistenerRegistration = notificationService
            .registerCompositeListener(rendererListenerImpl.getCompositeListener());
        TapiServiceNotificationHandler serviceHandlerListenerImpl = new TapiServiceNotificationHandler(dataBroker);
        servicehandlerlistenerRegistration = notificationService
            .registerCompositeListener(serviceHandlerListenerImpl.getCompositeListener());
        tapinetworkmodellistenerRegistration = notificationService
            .registerCompositeListener(tapiNetworkModelNotificationHandler.getCompositeListener());
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @Deactivate
    public void close() {
        listeners.forEach(lis -> lis.close());
        listeners.clear();
        pcelistenerRegistration.close();
        rendererlistenerRegistration.close();
        servicehandlerlistenerRegistration.close();
        tapinetworkmodellistenerRegistration.close();
        rpcRegistration.close();
        LOG.info("TapiProvider Session Closed");
    }

    public Registration getRegisteredRpcs() {
        return rpcRegistration;
    }
}
