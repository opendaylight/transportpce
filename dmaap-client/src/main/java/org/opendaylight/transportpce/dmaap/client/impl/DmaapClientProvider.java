/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.impl;

import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.transportpce.dmaap.client.listener.NbiNotificationsHandler;
//import org.opendaylight.transportpce.dmaap.client.listener.NbiNotificationsListenerImpl;
//import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "org.opendaylight.transportpce.dmaap")
public class DmaapClientProvider {

    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition
        String dmaapBaseUrl() default "http://localhost:8080";
        @AttributeDefinition
        String dmaapUsername() default "";
        @AttributeDefinition
        String dmaapPassword() default "";
    }

    private static final Logger LOG = LoggerFactory.getLogger(DmaapClientProvider.class);
    private Registration listenerRegistration;

    @Activate
    public DmaapClientProvider(@Reference NotificationService notificationService, Configuration config) {
        this(notificationService, config.dmaapBaseUrl(), config.dmaapUsername(), config.dmaapPassword());
    }

    public DmaapClientProvider(NotificationService notificationService, String baseUrl,
            String username, String password) {
        final var listener = new NbiNotificationsHandler(baseUrl, username, password);
        listenerRegistration = notificationService.registerCompositeListener(listener.getCompositeListener());
        LOG.info("DmaapClientProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        listenerRegistration.close();
        LOG.info("DmaapClientProvider Closed");
    }
}
