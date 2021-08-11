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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.Notification;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsListenerImpl implements NbiNotificationsListener {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsListenerImpl.class);
    private Map<String, Publisher<NotificationProcessService>> publishersServiceMap;
    private Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap;
    private Map<String, Publisher<Notification>> tapiPublisherMap;

    public NbiNotificationsListenerImpl(Map<String, Publisher<NotificationProcessService>> publishersServiceMap,
                                        Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap,
                                        Map<String, Publisher<Notification>> tapiPublisherMap) {
        this.publishersServiceMap = publishersServiceMap;
        this.publishersAlarmMap = publishersAlarmMap;
        this.tapiPublisherMap = tapiPublisherMap;
    }

    @Override
    public void onPublishNotificationProcessService(PublishNotificationProcessService notification) {
        LOG.info("Receiving request for publishing notification service");
        String publisherName = notification.getPublisherName();
        if (!publishersServiceMap.containsKey(publisherName)) {
            LOG.error("Unknown publisher {}", publisherName);
            return;
        }
        Publisher<NotificationProcessService> publisher = publishersServiceMap.get(publisherName);
        publisher.sendEvent(new NotificationProcessServiceBuilder()
                .setCommonId(notification.getCommonId())
                .setConnectionType(notification.getConnectionType())
                .setMessage(notification.getMessage())
                .setOperationalState(notification.getOperationalState())
                .setResponseFailed(notification.getResponseFailed())
                .setServiceAEnd(notification.getServiceAEnd())
                .setServiceName(notification.getServiceName())
                .setServiceZEnd(notification.getServiceZEnd())
                        .build(), notification.getConnectionType().getName());
    }

    @Override
    public void onPublishNotificationAlarmService(PublishNotificationAlarmService notification) {
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

    @Override
    public void onPublishTapiNotificationService(PublishTapiNotificationService notification) {
        LOG.info("Receiving request for publishing TAPI notification");
        String topic = notification.getTopic();
        if (!tapiPublisherMap.containsKey(topic)) {
            LOG.error("Unknown topic {}", topic);
            return;
        }
        Publisher publisher = tapiPublisherMap.get(topic);
        publisher.sendEvent(new NotificationBuilder()
                .setAlarmInfo(notification.getAlarmInfo())
                .setAdditionalText(notification.getAdditionalText())
                .setAdditionalInfo(notification.getAdditionalInfo())
                .setNotificationType(notification.getNotificationType())
                .setChangedAttributes(notification.getChangedAttributes())
                .setEventTimeStamp(notification.getEventTimeStamp())
                .setLayerProtocolName(notification.getLayerProtocolName())
                .setName(notification.getName())
                .setSequenceNumber(notification.getSequenceNumber())
                .setSourceIndicator(notification.getSourceIndicator())
                .setTargetObjectIdentifier(notification.getTargetObjectIdentifier())
                .setTargetObjectName(notification.getTargetObjectName())
                .setTargetObjectType(notification.getTargetObjectType())
                .setTcaInfo(notification.getTcaInfo())
                .setUuid(notification.getUuid())
                .build(), topic);
    }

    public void setPublishersServiceMap(Map<String, Publisher<NotificationProcessService>> publishersServiceMap) {
        this.publishersServiceMap = publishersServiceMap;
    }

    public void setPublishersAlarmMap(Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap) {
        this.publishersAlarmMap = publishersAlarmMap;
    }

    public void setTapiPublishersMap(Map<String, Publisher<Notification>> tapiPublishersMap) {
        this.tapiPublisherMap = tapiPublishersMap;
    }
}
