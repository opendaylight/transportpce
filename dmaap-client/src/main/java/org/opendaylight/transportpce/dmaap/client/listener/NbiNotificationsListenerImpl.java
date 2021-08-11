/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.listener;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.opendaylight.transportpce.dmaap.client.resource.EventsApi;
import org.opendaylight.transportpce.dmaap.client.resource.config.JsonConfigurator;
import org.opendaylight.transportpce.dmaap.client.resource.model.CreatedEvent;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishTapiNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsListenerImpl implements NbiNotificationsListener {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsListenerImpl.class);
    private String topic = "unauthenticated.TPCE";
    private EventsApi api;

    public NbiNotificationsListenerImpl(String baseUrl, String username, String password) {
        LOG.info("Dmaap server {} for user {}", baseUrl, username);
        Client client = ClientBuilder.newClient();
        if (username != null && username.isBlank() && password != null && !password.isBlank()) {
            HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic(username, password);
            client.register(authFeature);
            topic = "authenticated.TPCE";
        }
        client.register(new LoggingFeature(java.util.logging.Logger.getLogger(this.getClass().getName())))
        .register(JacksonFeature.class).register(JsonConfigurator.class);
        api = WebResourceFactory.newResource(EventsApi.class, client.target(baseUrl));

    }

    @Override
    public void onPublishNotificationProcessService(PublishNotificationProcessService notification) {
        try {
            CreatedEvent response = api.sendEvent(topic, notification);
            LOG.info("Response received {}", response);
        } catch (WebApplicationException e) {
            LOG.warn("Cannot send event {}", notification, e);
        }

    }

    @Override
    public void onPublishNotificationAlarmService(PublishNotificationAlarmService notification) {
    }

    @Override
    public void onPublishTapiNotificationService(PublishTapiNotificationService notification) {
    }

}
