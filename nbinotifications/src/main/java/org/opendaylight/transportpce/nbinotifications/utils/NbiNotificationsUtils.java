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
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationTapiServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.AdditionalInfo;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.AdditionalInfoBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.AdditionalInfoKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.AlarmInfoBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.ChangedAttributes;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.TargetObjectName;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.TargetObjectNameBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.TargetObjectNameKey;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.tapi.service.TcaInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
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
        Map<AdditionalInfoKey, AdditionalInfo> addInfoMap = notification.nonnullAdditionalInfo().values().stream()
            .collect(Collectors.toMap(
                    e -> new AdditionalInfoKey(e.getValueName()),
                    e -> new AdditionalInfoBuilder(e).build()));
        Map<ChangedAttributesKey, ChangedAttributes> changedAttMap = notification.nonnullChangedAttributes().values()
                .stream().collect(Collectors.toMap(
                        e -> new ChangedAttributesKey(e.getValueName()),
                        e -> new ChangedAttributesBuilder(e).build()));
        Map<NameKey, Name> nameMap = notification.nonnullName().values().stream()
                .collect(Collectors.toMap(Name::key, Function.identity()));
        Map<TargetObjectNameKey, TargetObjectName> targetObjNameMap = notification.nonnullTargetObjectName().values()
                .stream().collect(Collectors.toMap(
                        e -> new TargetObjectNameKey(e.getValueName()),
                        e -> new TargetObjectNameBuilder(e).build()));
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
