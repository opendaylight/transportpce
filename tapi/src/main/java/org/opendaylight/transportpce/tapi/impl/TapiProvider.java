/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.connectivity.TapiConnectivityImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiNetworkModelNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceNotificationHandler;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.transportpce.tapi.topology.TapiOrLinkListener;
import org.opendaylight.transportpce.tapi.topology.TapiPortMappingListener;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyImpl;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.ServiceInterfacePoints;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
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
    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final OrgOpenroadmServiceService serviceHandler;
    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private ObjectRegistration<TransportpceTapinetworkutilsService> tapiNetworkutilsServiceRpcRegistration;
    private List<Registration> listeners;
    private List<Registration> rpcRegistrations = new ArrayList<>();
    private Registration pcelistenerRegistration;
    private Registration rendererlistenerRegistration;
    private Registration servicehandlerlistenerRegistration;
    private Registration tapinetworkmodellistenerRegistration;

    @Activate
    public TapiProvider(@Reference DataBroker dataBroker,
            @Reference RpcProviderService rpcProviderService,
            @Reference NotificationService notificationService,
            @Reference NotificationPublishService notificationPublishService,
            @Reference NetworkTransactionService networkTransactionService,
            @Reference OrgOpenroadmServiceService serviceHandler,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference TapiNetworkModelNotificationHandler tapiNetworkModelNotificationHandler,
            @Reference TapiNetworkModelService tapiNetworkModelServiceImpl) {
        this.dataBroker = dataBroker;
        this.networkTransactionService = networkTransactionService;
        this.serviceHandler = serviceHandler;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        LOG.info("TapiProvider Session Initiated");
        TapiContext tapiContext = new TapiContext(this.networkTransactionService);
        LOG.info("Empty TAPI context created: {}", tapiContext.getTapiContext());
        TapiLink tapiLink = new TapiLinkImpl(this.networkTransactionService);
        TopologyUtils topologyUtils = new TopologyUtils(this.networkTransactionService, this.dataBroker, tapiLink);
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(this.serviceDataStoreOperations, new HashMap<>(),
                tapiContext, this.networkTransactionService);
        TapiInitialORMapping tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
                tapiContext, this.serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        tapiInitialORMapping.performServInitialMapping();
        TapiPceNotificationHandler pceListenerImpl = new TapiPceNotificationHandler(dataBroker, connectivityUtils);
        TapiRendererNotificationHandler rendererListenerImpl = new TapiRendererNotificationHandler(dataBroker,
                notificationPublishService);

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(this.serviceHandler, tapiContext,
                connectivityUtils, pceListenerImpl, rendererListenerImpl, networkTransactionService);
        rpcRegistrations.add(rpcProviderService.registerRpcImplementations(tapiConnectivity.registerRPCs()));
        TapiTopologyImpl topo = new TapiTopologyImpl(this.dataBroker, tapiContext, topologyUtils, tapiLink);
        rpcRegistrations.add(rpcProviderService.registerRpcImplementations(topo.registerRPCs()));

        this.listeners = new ArrayList<>();
        TapiNetconfTopologyListener topologyListener = new TapiNetconfTopologyListener(tapiNetworkModelServiceImpl);
        TapiOrLinkListener orLinkListener = new TapiOrLinkListener(tapiLink, networkTransactionService);
        TapiPortMappingListener tapiPortMappingListener = new TapiPortMappingListener(tapiNetworkModelServiceImpl);
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, LINK_II), orLinkListener));
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, InstanceIdentifiers.NETCONF_TOPOLOGY_II
                    .child(Node.class)),
                topologyListener));
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, MAPPING_II), tapiPortMappingListener));
        TapiListener tapiListener = new TapiListener();
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(
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
        for (Registration reg : rpcRegistrations) {
            reg.close();
        }
        LOG.info("TapiProvider Session Closed");
    }

    public List<Registration> getRegisteredRpcs() {
        return rpcRegistrations;
    }

}
