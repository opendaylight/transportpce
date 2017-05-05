/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubrenderer.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *Class to register Stubrenderer Service & Notification.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class StubrendererProvider {
    private static final Logger LOG = LoggerFactory.getLogger(StubrendererProvider.class);
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationPublishService notificationPublishService;


    private BindingAwareBroker.RpcRegistration<StubrendererService> rpcRegistration;
    private ListenerRegistration<StubrendererListener> stubRendererlistenerRegistration;

    public StubrendererProvider(RpcProviderRegistry rpcProviderRegistry,
        NotificationService notificationService,
        NotificationPublishService notificationPublishService) {
        this.rpcRegistry = rpcProviderRegistry;
        this.notificationPublishService = notificationPublishService;
    }

    /*
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("StubrendererProvider Session Initiated");
        final StubrendererImpl consumer = new StubrendererImpl(notificationPublishService);
        rpcRegistration = rpcRegistry.addRpcImplementation(StubrendererService.class, consumer);
    }

    /*
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("StubrendererProvider Closed");
        rpcRegistration.close();
        stubRendererlistenerRegistration.close();
    }
}
