/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NbiNotificationsService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderService rpcService;
    private ObjectRegistration<NbiNotificationsService> rpcRegistration;

    public NbiNotificationsProvider(final DataBroker dataBroker, RpcProviderService rpcProviderService) {
        this.dataBroker = dataBroker;
        this.rpcService = rpcProviderService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NbiNotificationsProvider Session Initiated");
        final NbiNotificationsImpl nbiNotifications = new NbiNotificationsImpl(dataBroker);
        rpcRegistration = rpcService.registerRpcImplementation(NbiNotificationsService.class, nbiNotifications);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("NbiNotificationsProvider Closed");
        rpcRegistration.close();
    }

}
