/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.impl;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.Services;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register
 * Servicehandler Service and Notification.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
@Component
public class ServicehandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerProvider.class);
    private static final InstanceIdentifier<Services> SERVICE = InstanceIdentifier.builder(ServiceList.class)
            .child(Services.class).build();

    private ListenerRegistration<TransportpcePceListener> pcelistenerRegistration;
    private ListenerRegistration<DataTreeChangeListener<Services>> serviceDataTreeChangeListenerRegistration;
    private ListenerRegistration<TransportpceRendererListener> rendererlistenerRegistration;
    private ListenerRegistration<TransportpceNetworkmodelListener> networkmodellistenerRegistration;
    private ObjectRegistration<OrgOpenroadmServiceService> rpcRegistration;
    private ServiceDataStoreOperations serviceDataStoreOperations;

    @Activate
    public ServicehandlerProvider(@Reference final DataBroker dataBroker,
            @Reference RpcProviderService rpcProviderService,
            @Reference NotificationService notificationService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference TransportpcePceListener pceListenerImpl,
            @Reference TransportpceRendererListener rendererListenerImpl,
            @Reference TransportpceNetworkmodelListener networkModelListenerImpl,
            @Reference NotificationPublishService notificationPublishService,
            @Reference OrgOpenroadmServiceService servicehandler,
            @Reference DataTreeChangeListener<Services> serviceListener) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.serviceDataStoreOperations.initialize();
        pcelistenerRegistration = notificationService.registerNotificationListener(pceListenerImpl);
        rendererlistenerRegistration = notificationService.registerNotificationListener(rendererListenerImpl);
        networkmodellistenerRegistration = notificationService.registerNotificationListener(networkModelListenerImpl);
        serviceDataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, SERVICE), serviceListener);
        rpcRegistration = rpcProviderService
            .registerRpcImplementation(OrgOpenroadmServiceService.class, servicehandler);
        LOG.info("ServicehandlerProvider Session Initiated");
        LOG.info("Transportpce controller started");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @Deactivate
    public void close() {
        LOG.info("ServicehandlerProvider Closed");
        pcelistenerRegistration.close();
        serviceDataTreeChangeListenerRegistration.close();
        rendererlistenerRegistration.close();
        networkmodellistenerRegistration.close();
        rpcRegistration.close();
    }
}