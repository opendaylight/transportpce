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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationServiceBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationServiceDeserializer implements Deserializer<NotificationService> {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceDeserializer.class);
    private JsonStringConverter<org.opendaylight.yang.gen.v1
        .nbi.notifications.rev201130.NotificationService> converter;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        LOG.info("Deserializer configuration {}", configs);
        if (configs.containsKey(ConfigConstants.CONVERTER)
                && configs.get(ConfigConstants.CONVERTER) instanceof JsonStringConverter<?>) {
            converter = (JsonStringConverter<org.opendaylight.yang.gen.v1
                    .nbi.notifications.rev201130.NotificationService>) configs
                    .get(ConfigConstants.CONVERTER);
        }
    }

    @Override
    public NotificationService deserialize(String topic, byte[] data) {
        if (converter == null) {
            throw new IllegalArgumentException(
                    "Converter should be configured through configure method of deserializer");
        }
        String value = new String(data, StandardCharsets.UTF_8);
        // The message published is
        // org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService
        // we have to map it to
        // org.opendaylight.yang.gen
        // .v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService
        org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService mappedString = converter
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(
                        org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService.QNAME),
                        value,
                        JSONCodecFactorySupplier.RFC7951);
        if (mappedString != null) {
            LOG.info("Reading event {}", mappedString);
            return new NotificationServiceBuilder().setCommonId(mappedString.getCommonId())
                    .setConnectionType(mappedString.getConnectionType()).setMessage(mappedString.getMessage())
                    .setOperationalState(mappedString.getOperationalState())
                    .setResponseFailed(mappedString.getResponseFailed()).setServiceName(mappedString.getServiceName())
                    .setServiceAEnd(mappedString.getServiceAEnd()).setServiceZEnd(mappedString.getServiceZEnd())
                    .build();
        }
        return null;
    }

}
