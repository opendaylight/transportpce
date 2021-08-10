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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.process.service.output.NotificationsProcessServiceBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationServiceDeserializer implements Deserializer<NotificationsProcessService> {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceDeserializer.class);
    private JsonStringConverter<NotificationProcessService> converter;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        LOG.info("Deserializer configuration {}", configs);
        if (configs.containsKey(ConfigConstants.CONVERTER)
                && configs.get(ConfigConstants.CONVERTER) instanceof JsonStringConverter<?>) {
            converter = (JsonStringConverter<NotificationProcessService>) configs.get(ConfigConstants.CONVERTER);
        }
    }

    @Override
    public NotificationsProcessService deserialize(String topic, byte[] data) {
        if (converter == null) {
            throw new IllegalArgumentException(
                    "Converter should be configured through configure method of deserializer");
        }
        String value = new String(data, StandardCharsets.UTF_8);
        // The message published is
        // org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessService
        // we have to map it to
        // org.opendaylight.yang.gen
        // .v1.nbi.notifications.rev210813.get.notifications.service.output.NotificationService
        NotificationProcessService mappedString = converter.createDataObjectFromJsonString(
                YangInstanceIdentifier.of(NotificationProcessService.QNAME), value, JSONCodecFactorySupplier.RFC7951);
        if (mappedString == null) {
            return null;
        }
        LOG.info("Reading event {}", mappedString);
        return new NotificationsProcessServiceBuilder()
                .setCommonId(mappedString.getCommonId())
                .setConnectionType(mappedString.getConnectionType())
                .setMessage(mappedString.getMessage())
                .setOperationalState(mappedString.getOperationalState())
                .setResponseFailed(mappedString.getResponseFailed())
                .setServiceName(mappedString.getServiceName())
                .setServiceAEnd(mappedString.getServiceAEnd())
                .setServiceZEnd(mappedString.getServiceZEnd())
                .build();
    }

}
