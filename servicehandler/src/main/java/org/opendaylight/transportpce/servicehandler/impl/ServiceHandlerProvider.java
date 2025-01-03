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
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.PceNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.RendererNotificationHandler;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
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
public class ServiceHandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHandlerProvider.class);
    private static final DataObjectReference<Services> SERVICE = DataObjectReference.builder(ServiceList.class)
            .child(Services.class)
            .build();

    private final Registration pcelistenerRegistration;
    private Registration serviceListListenerRegistration;
    private final Registration rendererlistenerRegistration;
    private final Registration networkmodellistenerRegistration;
    private ServiceDataStoreOperations serviceDataStoreOperations;

    @Activate
    public ServiceHandlerProvider(@Reference final DataBroker dataBroker,
            @Reference NotificationService notificationService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference PceNotificationHandler pceNotificationHandler,
            @Reference RendererNotificationHandler rendererNotificationHandler,
            @Reference NetworkModelNotificationHandler networkModelNotificationHandler,
            @Reference DataTreeChangeListener<Services> serviceListener) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.serviceDataStoreOperations.initialize();
        pcelistenerRegistration = notificationService
            .registerCompositeListener(pceNotificationHandler.getCompositeListener());
        rendererlistenerRegistration = notificationService
            .registerCompositeListener(rendererNotificationHandler.getCompositeListener());
        networkmodellistenerRegistration = notificationService
            .registerCompositeListener(networkModelNotificationHandler.getCompositeListener());
        serviceListListenerRegistration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.OPERATIONAL,
                SERVICE, serviceListener);
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
        serviceListListenerRegistration.close();
        rendererlistenerRegistration.close();
        networkmodellistenerRegistration.close();
    }
}
