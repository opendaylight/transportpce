/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.get.notifications.tapi.service.output.NotificationsTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.get.notifications.tapi.service.output.NotificationsTapiServiceBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNotificationDeserializer implements Deserializer<NotificationsTapiService> {
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
    public NotificationsTapiService deserialize(String topic, byte[] data) {
        if (converter == null) {
            throw new IllegalArgumentException(
                "Converter should be configured through configure method of deserializer");
        }
        String value = new String(data, StandardCharsets.UTF_8);
        // The message published is
        // org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.NotificationTapiService
        // we have to map it to
        // org.opendaylight.yang.gen
        // .v1.nbi.notifications.rev210813.get.notifications.tapi.service.output.NotificationTapiService
        NotificationTapiService mappedString = converter.createDataObjectFromJsonString(
            YangInstanceIdentifier.of(NotificationTapiService.QNAME), value, JSONCodecFactorySupplier.RFC7951);
        if (mappedString == null) {
            return null;
        }
        LOG.info("Reading Tapi event {}", mappedString);
        return new NotificationsTapiServiceBuilder()
            .setName(mappedString.getName())
            .setAdditionalInfo(mappedString.getAdditionalInfo())
            .setUuid(mappedString.getUuid())
            .setAdditionalText(mappedString.getAdditionalText())
            .setAlarmInfo(mappedString.getAlarmInfo())
            .setChangedAttributes(mappedString.getChangedAttributes())
            .setNotificationType(mappedString.getNotificationType())
            .setEventTimeStamp(mappedString.getEventTimeStamp())
            .setLayerProtocolName(mappedString.getLayerProtocolName())
            .setTargetObjectIdentifier(mappedString.getTargetObjectIdentifier())
            .setSequenceNumber(mappedString.getSequenceNumber())
            .setSourceIndicator(mappedString.getSourceIndicator())
            .setTargetObjectName(mappedString.getTargetObjectName())
            .setTargetObjectType(mappedString.getTargetObjectType())
            .setTcaInfo(mappedString.getTcaInfo())
            .build();
    }
}

