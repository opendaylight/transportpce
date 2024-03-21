/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.listener;

import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishTapiNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsHandler.class);
    private Map<String, Publisher<NotificationProcessService>> publishersServiceMap;
    private Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap;
    private Map<String, Publisher<NotificationTapiService>> tapiPublisherMap;

    public NbiNotificationsHandler(Map<String, Publisher<NotificationProcessService>> publishersServiceMap,
                                        Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap,
                                        Map<String, Publisher<NotificationTapiService>> tapiPublisherMap) {
        this.publishersServiceMap = publishersServiceMap;
        this.publishersAlarmMap = publishersAlarmMap;
        this.tapiPublisherMap = tapiPublisherMap;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(
                PublishNotificationProcessService.class, this::onPublishNotificationProcessService),
            new CompositeListener.Component<>(
                PublishNotificationAlarmService.class, this::onPublishNotificationAlarmService),
            new CompositeListener.Component<>(
                PublishTapiNotificationService.class, this::onPublishTapiNotificationService)
        ));
    }

    void onPublishNotificationProcessService(PublishNotificationProcessService notification) {
        LOG.info("Receiving request for publishing notification service");
        String publisherName = notification.getPublisherName();
        if (!publishersServiceMap.containsKey(publisherName)) {
            LOG.error("Unknown publisher {}", publisherName);
            return;
        }
        Publisher<NotificationProcessService> publisher = publishersServiceMap.get(publisherName);
        NotificationProcessServiceBuilder notificationProcessServiceBuilder = new NotificationProcessServiceBuilder()
                .setCommonId(notification.getCommonId())
                .setConnectionType(notification.getConnectionType())
                .setMessage(notification.getMessage())
                .setOperationalState(notification.getOperationalState())
                .setResponseFailed(notification.getResponseFailed())
                .setServiceAEnd(notification.getServiceAEnd())
                .setServiceName(notification.getServiceName())
                .setServiceZEnd(notification.getServiceZEnd());

        if (notification.getIsTempService() == null || !notification.getIsTempService()) {
            publisher.sendEvent(notificationProcessServiceBuilder
                .build(), notification.getConnectionType().getName());
        } else {
            // A-to-Z and Z-to-A containers are only needed for temp-service-create notification
            publisher.sendEvent(notificationProcessServiceBuilder
                    .setAToZ(notification.getAToZ())
                    .setZToA(notification.getZToA())
                    .build(), notification.getConnectionType().getName());
        }
    }

    void onPublishNotificationAlarmService(PublishNotificationAlarmService notification) {
        LOG.info("Receiving request for publishing notification alarm service");
        String publisherName = notification.getPublisherName();
        if (!publishersAlarmMap.containsKey(publisherName)) {
            LOG.error("Unknown topic {}", publisherName);
            return;
        }
        Publisher<NotificationAlarmService> publisherAlarm = publishersAlarmMap.get(publisherName);
        publisherAlarm.sendEvent(new NotificationAlarmServiceBuilder()
                .setConnectionType(notification.getConnectionType())
                .setMessage(notification.getMessage())
                .setOperationalState(notification.getOperationalState())
                .setServiceName(notification.getServiceName())
                        .build(), "alarm" + notification.getConnectionType().getName());
    }

    void onPublishTapiNotificationService(PublishTapiNotificationService notification) {
        LOG.info("Receiving request for publishing TAPI notification");
        String topic = notification.getTopic();
        if (!tapiPublisherMap.containsKey(topic)) {
            LOG.error("Unknown topic {}", topic);
            return;
        }
        Publisher<NotificationTapiService> publisher = tapiPublisherMap.get(topic);
        publisher.sendEvent(new NotificationTapiServiceBuilder(
                NbiNotificationsUtils.transformTapiNotification(notification)).build(), topic);
    }

    public void setPublishersServiceMap(Map<String, Publisher<NotificationProcessService>> publishersServiceMap) {
        this.publishersServiceMap = publishersServiceMap;
    }

    public void setPublishersAlarmMap(Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap) {
        this.publishersAlarmMap = publishersAlarmMap;
    }

    public void setTapiPublishersMap(Map<String, Publisher<NotificationTapiService>> tapiPublishersMap) {
        this.tapiPublisherMap = tapiPublishersMap;
    }

    public Publisher<NotificationTapiService> getTapiPublisherFromTopic(String topic) {
        return this.tapiPublisherMap.get(topic);
    }
}
