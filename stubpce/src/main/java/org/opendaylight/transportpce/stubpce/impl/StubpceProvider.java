/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubpce.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class to register
 * Stubpce Service & Notification.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class StubpceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(StubpceProvider.class);
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationPublishService notificationPublishService;


    private BindingAwareBroker.RpcRegistration<StubpceService> rpcRegistration;
    private ListenerRegistration<StubpceListener> stubPcelistenerRegistration;

    public StubpceProvider(RpcProviderRegistry rpcProviderRegistry,
        NotificationService notificationService,
        NotificationPublishService notificationPublishService) {
        this.rpcRegistry = rpcProviderRegistry;
        this.notificationPublishService = notificationPublishService;
    }

    /*
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("StubpceProvider Session Initiated");
        final StubpceImpl consumer = new StubpceImpl(notificationPublishService);
        rpcRegistration = rpcRegistry.addRpcImplementation(StubpceService.class, consumer);
    }

    /*
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("StubpceProvider Closed");
        rpcRegistration.close();
        stubPcelistenerRegistration.close();
    }
}
