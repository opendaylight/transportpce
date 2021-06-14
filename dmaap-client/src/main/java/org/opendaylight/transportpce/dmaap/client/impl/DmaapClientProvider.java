/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.impl;

import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.transportpce.dmaap.client.listener.NbiNotificationsListenerImpl;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NbiNotificationsListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmaapClientProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DmaapClientProvider.class);
    private ListenerRegistration<NbiNotificationsListener> listenerRegistration;
    private NotificationService notificationService;
    private final String baseUrl;
    private final String username;
    private final String password;

    public DmaapClientProvider(NotificationService notificationService, String baseUrl,
            String username, String password) {
        this.notificationService = notificationService;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("DmaapClientProvider Session Initiated");
        listenerRegistration = notificationService.registerNotificationListener(
                new NbiNotificationsListenerImpl(baseUrl, username, password));
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        listenerRegistration.close();
        LOG.info("DmaapClientProvider Closed");
    }

}
