/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.AdditionalInfo;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.AdditionalInfoBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.AdditionalInfoKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.AlarmInfoBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.ChangedAttributes;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.TargetObjectName;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.TargetObjectNameBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.TargetObjectNameKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.TcaInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NbiNotificationsUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsUtils.class);

    private NbiNotificationsUtils() {
    }

    public static Properties loadProperties(String propertyFileName) {
        Properties props = new Properties();
        InputStream inputStream = NbiNotificationsUtils.class.getClassLoader().getResourceAsStream(propertyFileName);
        try {
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                LOG.warn("Kafka property file '{}' is empty", propertyFileName);
            }
        } catch (IOException e) {
            LOG.error("Kafka property file '{}' was not found in the classpath", propertyFileName, e);
        }
        return props;
    }

    public static NotificationTapiService transformTapiNotification(PublishTapiNotificationService notification) {
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
            for (Name name:notification.getName().values()) {
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
        LOG.info("Notification uuid = {}", notification.getUuid());
        return new NotificationTapiServiceBuilder()
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
}
