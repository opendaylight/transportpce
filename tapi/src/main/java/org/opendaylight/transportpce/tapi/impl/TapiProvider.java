/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.kafka.Kafka;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.transportpce.tapi.listeners.ORNetworkModelListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.ORPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.ORRendererListenerImpl;
import org.opendaylight.transportpce.tapi.topology.MongoDbDataStoreService;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyImpl;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.TapiNetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TapiCommonService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationService;
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

    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderService;
    private ObjectRegistration<TapiConnectivityService> rpcRegistration;
    private ObjectRegistration<TapiTopologyService> rpcRegistration2;
    private ObjectRegistration<TapiCommonService> rpcRegistration3;
    private ObjectRegistration<TapiNotificationService> rpcRegistration4;
    private ObjectRegistration<TapiNetworkutilsService> rpcRegistration5;
    private final ServiceHandlerOperations serviceHandler;
    private ListenerRegistration<TapiListener> listenerRegistration;
    private final TapiListener tapiListener;
    private final Kafka kafka;
    private final TapiNetconfTopologyListener topologyListener;
    private ListenerRegistration<TapiNetconfTopologyListener> dataTreeChangeListenerRegistration;
    private final MongoDbDataStoreService mongoDbDataStoreService;
    private final TapiNetworkutilsService tapiNetworkutilsService;
    private final NotificationService notificationService;
    private ListenerRegistration<TransportpcePceListener> orpcelistenerRegistration;
    private ListenerRegistration<TransportpceRendererListener> orrendererlistenerRegistration;
    private ListenerRegistration<TransportpceNetworkmodelListener> ornetworkmodellistenerRegistration;

    public TapiProvider(DataBroker dataBroker, RpcProviderService rpcProviderService,
                        ServiceHandlerOperations serviceHanlder, TapiListener tapiListener, Kafka kafka,
                        TapiNetconfTopologyListener topologyListener, MongoDbDataStoreService mongoDbDataStoreService,
                        TapiNetworkutilsService tapiNetworkutilsService, NotificationService notificationService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
        this.serviceHandler = serviceHanlder;
        this.tapiListener = tapiListener;
        this.kafka = kafka;
        this.topologyListener = topologyListener;
        this.mongoDbDataStoreService = mongoDbDataStoreService;
        this.tapiNetworkutilsService = tapiNetworkutilsService;
        this.notificationService = notificationService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("TapiProvider Session Initiated");
        mongoDbDataStoreService.initialize();
        // todo register listeners in notification service
        final ORNetworkModelListenerImpl orNetworkModelListener = new ORNetworkModelListenerImpl(this
                .mongoDbDataStoreService);
        final ORRendererListenerImpl orRendererListener = new ORRendererListenerImpl(this
                .mongoDbDataStoreService);
        final ORPceListenerImpl orPceListener = new ORPceListenerImpl(this
                .mongoDbDataStoreService);
        orpcelistenerRegistration = notificationService.registerNotificationListener(orPceListener);
        orrendererlistenerRegistration = notificationService.registerNotificationListener(orRendererListener);
        ornetworkmodellistenerRegistration = notificationService.registerNotificationListener(orNetworkModelListener);

        TapiImpl tapi = new TapiImpl(this.serviceHandler, this.kafka, this.mongoDbDataStoreService,
                orNetworkModelListener, orPceListener, orRendererListener);
        TapiTopologyImpl topo = new TapiTopologyImpl(this.dataBroker, this.mongoDbDataStoreService);
        rpcRegistration = rpcProviderService.registerRpcImplementation(TapiConnectivityService.class, tapi);
        rpcRegistration2 = rpcProviderService.registerRpcImplementation(TapiTopologyService.class, topo);
        rpcRegistration3 = rpcProviderService.registerRpcImplementation(TapiCommonService.class, tapi);
        rpcRegistration4 = rpcProviderService.registerRpcImplementation(TapiNotificationService.class, tapi);
        rpcRegistration5 = rpcProviderService.registerRpcImplementation(TapiNetworkutilsService.class,
                tapiNetworkutilsService);
        @NonNull
        InstanceIdentifier<ServiceInterfacePoints> sipIID = InstanceIdentifier.create(ServiceInterfacePoints.class);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, sipIID), tapiListener);
        dataTreeChangeListenerRegistration =
                dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), topologyListener);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("TapiProvider Session Closed");
        // pcelistenerRegistration.close();
        listenerRegistration.close();
        dataTreeChangeListenerRegistration.close();
        orpcelistenerRegistration.close();
        orrendererlistenerRegistration.close();
        ornetworkmodellistenerRegistration.close();
        rpcRegistration.close();
        rpcRegistration2.close();
        rpcRegistration3.close();
        rpcRegistration4.close();
        rpcRegistration5.close();
        mongoDbDataStoreService.finish();
        kafka.getProducer().close();
    }

}
