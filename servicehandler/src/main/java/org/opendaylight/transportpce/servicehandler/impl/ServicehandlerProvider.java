/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServicehandlerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class to register
 * Servicehandler Service and Notification.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class ServicehandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;

    //private ListenerRegistration<ServicehandlerListener> ServicehandlerlistenerRegistration;
    /* Listener register for StubPce Notification. */
    private ListenerRegistration<StubpceListener> stubPcelistenerRegistration;
    /* Listener register for StubRender Notification. */
    private ListenerRegistration<StubrendererListener> stubRendererlistenerRegistration;
    private RpcRegistration<ServicehandlerService> rpcRegistration;


    public ServicehandlerProvider(final DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry,
            NotificationService notificationService, NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcProviderRegistry;
        this.notificationService = notificationService;
        this.notificationPublishService = notificationPublishService;
    }

    /*
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ServicehandlerProvider Session Initiated");
        final ServicehandlerImpl consumer = new ServicehandlerImpl(dataBroker, rpcRegistry, notificationPublishService);
        stubPcelistenerRegistration = notificationService.registerNotificationListener(consumer);
        stubRendererlistenerRegistration = notificationService.registerNotificationListener(consumer);
        rpcRegistration = rpcRegistry.addRpcImplementation(ServicehandlerService.class, consumer);
    }

    /*
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ServicehandlerProvider Closed");
        stubPcelistenerRegistration.close();
        stubRendererlistenerRegistration.close();
        rpcRegistration.close();
    }
}
