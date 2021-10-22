/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.AdditionalInfo;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.AdditionalInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.AdditionalInfoKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.AlarmInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.TargetObjectName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.TargetObjectNameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.TargetObjectNameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.TcaInfoBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNotificationDeserializer implements Deserializer<Notification> {
    private static final Logger LOG = LoggerFactory.getLogger(TapiNotificationDeserializer.class);
    private JsonStringConverter<NotificationTapiService> converter;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        LOG.info("Tapi Deserializer configuration {}", configs);
        if (configs.containsKey(ConfigConstants.CONVERTER)
            && configs.get(ConfigConstants.CONVERTER) instanceof JsonStringConverter<?>) {
            converter = (JsonStringConverter<NotificationTapiService>) configs.get(ConfigConstants.CONVERTER);
        }
    }

    @Override
    public Notification deserialize(String topic, byte[] data) {
        if (converter == null) {
            throw new IllegalArgumentException(
                "Converter should be configured through configure method of deserializer");
        }
        String value = new String(data, StandardCharsets.UTF_8);
        // The message published is
        // org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService
        // we have to map it to
        // org.opendaylight.yang.gen.v1
        // .urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.Notification
        NotificationTapiService mappedString = converter.createDataObjectFromJsonString(
            YangInstanceIdentifier.of(NotificationTapiService.QNAME), value, JSONCodecFactorySupplier.RFC7951);
        if (mappedString == null) {
            return null;
        }
        LOG.info("Reading Tapi event {}", mappedString);
        return transformNotificationTapiService(mappedString);
    }

    private Notification transformNotificationTapiService(NotificationTapiService mappedString) {
        LOG.info("Transforming TAPI notification for getNotificationList rpc");
        Map<AdditionalInfoKey, AdditionalInfo> addInfoMap = new HashMap<>();
        if (mappedString.getAdditionalInfo() != null) {
            for (org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.tapi.service.AdditionalInfo
                    addInfo:mappedString.getAdditionalInfo().values()) {
                AdditionalInfo transAddInfo = new AdditionalInfoBuilder()
                    .setValue(addInfo.getValue())
                    .setValueName(addInfo.getValueName())
                    .build();
                addInfoMap.put(transAddInfo.key(), transAddInfo);
            }
        }
        Map<ChangedAttributesKey, ChangedAttributes> changedAttMap = new HashMap<>();
        if (mappedString.getChangedAttributes() != null) {
            for (org.opendaylight.yang.gen.v1
                    .nbi.notifications.rev211013.notification.tapi.service.ChangedAttributes changedAtt:mappedString
                        .getChangedAttributes().values()) {
                ChangedAttributes transChangedAtt = new ChangedAttributesBuilder(changedAtt).build();
                changedAttMap.put(transChangedAtt.key(), transChangedAtt);
            }
        }
        Map<NameKey, Name> nameMap = new HashMap<>();
        if (mappedString.getName() != null) {
            for (Name name:mappedString.getName().values()) {
                Name transName = new NameBuilder(name).build();
                nameMap.put(transName.key(), transName);
            }
        }
        Map<TargetObjectNameKey, TargetObjectName> targetObjNameMap = new HashMap<>();
        if (mappedString.getTargetObjectName() != null) {
            for (org.opendaylight.yang.gen.v1
                    .nbi.notifications.rev211013.notification.tapi.service.TargetObjectName
                        targetObjectName:mappedString.getTargetObjectName().values()) {
                TargetObjectName transTargetObjName = new TargetObjectNameBuilder(targetObjectName).build();
                targetObjNameMap.put(transTargetObjName.key(), transTargetObjName);
            }
        }
        LOG.info("Notification uuid = {}", mappedString.getUuid().getValue());
        return new NotificationBuilder()
            .setAlarmInfo(mappedString.getAlarmInfo() == null ? null
                : new AlarmInfoBuilder(mappedString.getAlarmInfo()).build())
            .setAdditionalText(mappedString.getAdditionalText())
            .setAdditionalInfo(addInfoMap)
            .setNotificationType(mappedString.getNotificationType())
            .setChangedAttributes(changedAttMap)
            .setEventTimeStamp(mappedString.getEventTimeStamp())
            .setLayerProtocolName(mappedString.getLayerProtocolName())
            .setName(nameMap)
            .setSequenceNumber(mappedString.getSequenceNumber())
            .setSourceIndicator(mappedString.getSourceIndicator())
            .setTargetObjectIdentifier(mappedString.getTargetObjectIdentifier())
            .setTargetObjectName(targetObjNameMap)
            .setTargetObjectType(mappedString.getTargetObjectType())
            .setTcaInfo(mappedString.getTcaInfo() == null ? null
                : new TcaInfoBuilder(mappedString.getTcaInfo()).build())
            .setUuid(mappedString.getUuid())
            .build();
    }
}

