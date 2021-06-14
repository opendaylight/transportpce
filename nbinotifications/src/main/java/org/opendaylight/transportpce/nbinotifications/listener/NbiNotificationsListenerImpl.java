/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.listener;

import java.util.Map;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.producer.PublisherAlarm;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsListenerImpl implements NbiNotificationsListener {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsListenerImpl.class);
    private Map<String, Publisher> publishersServiceMap;
    private Map<String, PublisherAlarm> publishersAlarmMap;

    public NbiNotificationsListenerImpl(Map<String, Publisher> publishersServiceMap,
                                        Map<String, PublisherAlarm> publishersAlarmMap) {
        this.publishersServiceMap = publishersServiceMap;
        this.publishersAlarmMap = publishersAlarmMap;
    }

    @Override
    public void onPublishNotificationService(PublishNotificationService notification) {
        LOG.info("Receiving request for publishing notification service");
        String topic = notification.getTopic();
        if (!publishersServiceMap.containsKey(topic)) {
            LOG.error("Unknown topic {}", topic);
            return;
        }
        Publisher publisher = publishersServiceMap.get(topic);
        publisher.sendEvent(new NotificationServiceBuilder().setCommonId(notification.getCommonId())
                .setConnectionType(notification.getConnectionType()).setMessage(notification.getMessage())
                .setOperationalState(notification.getOperationalState())
                .setResponseFailed(notification.getResponseFailed())
                .setServiceAEnd(notification.getServiceAEnd())
                .setServiceName(notification.getServiceName())
                .setServiceZEnd(notification.getServiceZEnd()).build());
    }

    @Override
    public void onPublishNotificationAlarmService(PublishNotificationAlarmService notification) {
        LOG.info("Receiving request for publishing notification alarm service");
        String topic = notification.getTopic();
        if (!publishersAlarmMap.containsKey(topic)) {
            LOG.error("Unknown topic {}", topic);
            return;
        }
        PublisherAlarm publisherAlarm = publishersAlarmMap.get(topic);
        publisherAlarm.sendEvent(new NotificationAlarmServiceBuilder().setConnectionType(notification
                .getConnectionType())
                .setMessage(notification.getMessage())
                .setOperationalState(notification.getOperationalState())
                .setServiceName(notification.getServiceName())
                .build());
    }
}
