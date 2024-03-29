/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.alarm.service.output.NotificationsAlarmServiceBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationAlarmServiceDeserializer implements Deserializer<NotificationsAlarmService> {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationAlarmServiceDeserializer.class);
    private JsonStringConverter<NotificationAlarmService> converter;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        LOG.info("Deserializer configuration {}", configs);
        if (configs.containsKey(ConfigConstants.CONVERTER)
                && configs.get(ConfigConstants.CONVERTER) instanceof JsonStringConverter<?>) {
            converter = (JsonStringConverter<NotificationAlarmService>) configs.get(ConfigConstants.CONVERTER);
        }
    }

    @Override
    public NotificationsAlarmService deserialize(String topic, byte[] data) {
        if (converter == null) {
            throw new IllegalArgumentException(
                    "Converter should be configured through configure method of deserializer");
        }
        String value = new String(data, StandardCharsets.UTF_8);
        // The message published is
        // org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationAlarmService
        // we have to map it to
        // org.opendaylight.yang.gen
        // .v1.nbi.notifications.rev210813.get.notifications.service.output.NotificationsAlarmService
        NotificationAlarmService mappedString = converter.createDataObjectFromJsonString(
                YangInstanceIdentifier.of(NotificationAlarmService.QNAME), value, JSONCodecFactorySupplier.RFC7951);
        if (mappedString == null) {
            return null;
        }
        LOG.info("Reading event {}", mappedString);
        return new NotificationsAlarmServiceBuilder()
                .setConnectionType(mappedString.getConnectionType())
                .setMessage(mappedString.getMessage())
                .setOperationalState(mappedString.getOperationalState())
                .setServiceName(mappedString.getServiceName())
                .build();
    }
}
