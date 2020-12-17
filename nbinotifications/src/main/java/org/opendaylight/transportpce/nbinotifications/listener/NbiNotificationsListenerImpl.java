/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.listener;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsListenerImpl implements NbiNotificationsListener {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsListenerImpl.class);
    private Map<String, Publisher> publishersMap =  new HashMap<>();

    public NbiNotificationsListenerImpl(Map<String, Publisher> publishersMap) {
        this.publishersMap = publishersMap;
    }

    @Override
    public void onPublishNotificationService(PublishNotificationService notification) {
        LOG.info("Receiving request for publishing notification service");
        String topic = notification.getTopic();
        if (!publishersMap.containsKey(topic)) {
            LOG.error("Unknown topic {}", topic);
            return;
        }
        Publisher publisher = publishersMap.get(topic);
        publisher.sendEvent(new NotificationServiceBuilder().setCommonId(notification.getCommonId())
                .setConnectionType(notification.getConnectionType()).setMessage(notification.getMessage())
                .setOperationalState(notification.getOperationalState())
                .setResponseFailed(notification.getResponseFailed())
                .setServiceAEnd(notification.getServiceAEnd()).setServiceName(notification.getServiceName())
                .setServiceZEnd(notification.getServiceZEnd()).build());

    }

}
