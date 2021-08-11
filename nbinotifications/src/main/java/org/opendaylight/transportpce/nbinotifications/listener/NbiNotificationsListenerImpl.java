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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.Notification;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.AdditionalInfo;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.AdditionalInfoBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.AdditionalInfoKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.AlarmInfoBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.Name;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.NameBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.NameKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.TargetObjectName;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.TargetObjectNameBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.TargetObjectNameKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.TcaInfoBuilder;
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
        Publisher<Notification> publisher = tapiPublisherMap.get(topic);
        publisher.sendEvent(new NotificationBuilder(transformTapiNotification(notification))
                .build(), topic);
    }

    private Notification transformTapiNotification(PublishTapiNotificationService notification) {
        Map<AdditionalInfoKey, AdditionalInfo> addInfoMap = new HashMap<>();
        if (notification.getAdditionalInfo() != null) {
            for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.AdditionalInfo
                addInfo:notification.getAdditionalInfo().values()) {
                AdditionalInfo transAddInfo = new AdditionalInfoBuilder(addInfo).build();
                addInfoMap.put(transAddInfo.key(), transAddInfo);
            }
        }
        Map<ChangedAttributesKey, ChangedAttributes> changedAttMap = new HashMap<>();
        if (notification.getChangedAttributes() != null) {
            for (org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributes
                    changedAtt:notification.getChangedAttributes().values()) {
                ChangedAttributes transChangedAtt = new ChangedAttributesBuilder(changedAtt).build();
                changedAttMap.put(transChangedAtt.key(), transChangedAtt);
            }
        }
        Map<NameKey, Name> nameMap = new HashMap<>();
        if (notification.getName() != null) {
            for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.Name
                    name:notification.getName().values()) {
                Name transName = new NameBuilder(name).build();
                nameMap.put(transName.key(), transName);
            }
        }
        Map<TargetObjectNameKey, TargetObjectName> targetObjNameMap = new HashMap<>();
        if (notification.getTargetObjectName() != null) {
            for (org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.notification.rev181210.notification.TargetObjectName
                    targetObjectName:notification.getTargetObjectName().values()) {
                TargetObjectName transTargetObjName = new TargetObjectNameBuilder(targetObjectName).build();
                targetObjNameMap.put(transTargetObjName.key(), transTargetObjName);
            }
        }
        return new NotificationBuilder()
            .setAlarmInfo(notification.getAlarmInfo() == null ? null
                : new AlarmInfoBuilder(notification.getAlarmInfo()).build())
            .setAdditionalText(notification.getAdditionalText())
            .setAdditionalInfo(addInfoMap)
            .setNotificationType(notification.getNotificationType())
            .setChangedAttributes(changedAttMap)
            .setEventTimeStamp(notification.getEventTimeStamp())
            .setLayerProtocolName(notification.getLayerProtocolName())
            .setName(nameMap)
            .setSequenceNumber(notification.getSequenceNumber())
            .setSourceIndicator(notification.getSourceIndicator())
            .setTargetObjectIdentifier(notification.getTargetObjectIdentifier())
            .setTargetObjectName(targetObjNameMap)
            .setTargetObjectType(notification.getTargetObjectType())
            .setTcaInfo(notification.getTcaInfo() == null ? null
                : new TcaInfoBuilder(notification.getTcaInfo()).build())
            .setUuid(notification.getUuid())
            .build();
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
