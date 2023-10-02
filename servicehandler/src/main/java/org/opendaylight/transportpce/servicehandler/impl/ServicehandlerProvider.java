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
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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

    private final Registration pcelistenerRegistration;
    private ListenerRegistration<DataTreeChangeListener<Services>> serviceDataTreeChangeListenerRegistration;
    private final Registration rendererlistenerRegistration;
    private final Registration networkmodellistenerRegistration;
    private ObjectRegistration<OrgOpenroadmServiceService> rpcRegistration;
    private ServiceDataStoreOperations serviceDataStoreOperations;

    @Activate
    public ServicehandlerProvider(@Reference final DataBroker dataBroker,
            @Reference RpcProviderService rpcProviderService,
            @Reference NotificationService notificationService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference PceListenerImpl pceListenerImpl,
            @Reference RendererListenerImpl rendererListenerImpl,
            @Reference NetworkModelListenerImpl networkModelListenerImpl,
            @Reference NotificationPublishService notificationPublishService,
            @Reference OrgOpenroadmServiceService serviceHandler,
            @Reference DataTreeChangeListener<Services> serviceListener) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.serviceDataStoreOperations.initialize();
        pcelistenerRegistration = notificationService.registerCompositeListener(pceListenerImpl.getCompositeListener());
        rendererlistenerRegistration = notificationService
            .registerCompositeListener(rendererListenerImpl.getCompositeListener());
        networkmodellistenerRegistration = notificationService
            .registerCompositeListener(networkModelListenerImpl.getCompositeListener());
        serviceDataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, SERVICE), serviceListener);
        rpcRegistration = rpcProviderService
            .registerRpcImplementation(OrgOpenroadmServiceService.class, serviceHandler);
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