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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.OrgOpenroadmServiceService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register
 * Servicehandler Service and Notification.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on behalf of Orange
 *
 */
public class ServicehandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;

    /** Listener register for TransportpceService Notification. */
    private ListenerRegistration<StubpceListener> stubpcelistenerRegistration;
    private ListenerRegistration<StubrendererListener> stubrendererlistenerRegistration;
    private RpcRegistration<OrgOpenroadmServiceService> rpcRegistration;


    public ServicehandlerProvider(final DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry,
            NotificationService notificationService, NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcProviderRegistry;
        this.notificationService = notificationService;
        this.notificationPublishService = notificationPublishService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ServicehandlerProvider Session Initiated");
        final ServicehandlerImpl consumer = new ServicehandlerImpl(dataBroker, rpcRegistry, notificationPublishService);
        stubpcelistenerRegistration = notificationService.registerNotificationListener(consumer);
        stubrendererlistenerRegistration = notificationService.registerNotificationListener(consumer);
        rpcRegistration = rpcRegistry.addRpcImplementation(OrgOpenroadmServiceService.class, consumer);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ServicehandlerProvider Closed");
        stubpcelistenerRegistration.close();
        stubrendererlistenerRegistration.close();
        rpcRegistration.close();
    }
}
