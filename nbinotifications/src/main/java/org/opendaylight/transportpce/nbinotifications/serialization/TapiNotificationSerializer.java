/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNotificationSerializer implements Serializer<NotificationTapiService> {
    private static final Logger LOG = LoggerFactory.getLogger(TapiNotificationSerializer.class);
    private JsonStringConverter<NotificationTapiService> converter;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        LOG.info("Deserializer configuration {}", configs);
        if (configs.containsKey(ConfigConstants.CONVERTER)
                && configs.get(ConfigConstants.CONVERTER) instanceof JsonStringConverter<?>) {
            converter = (JsonStringConverter<NotificationTapiService>) configs.get(ConfigConstants.CONVERTER);
        }
    }

    @Override
    public byte[] serialize(String topic, NotificationTapiService data) {
        if (converter == null) {
            throw new IllegalArgumentException(
                    "Converter should be configured through configure method of serializer");
        }
        if (data == null) {
            LOG.error("Notification data is empty");
            return new byte[0];
        }
        try {
            DataObjectIdentifier<NotificationTapiService> iid = DataObjectIdentifier
                    .builder(NotificationTapiService.class).build();
            String serialized = converter.createJsonStringFromDataObject(iid, data, JSONCodecFactorySupplier.RFC7951);
            LOG.info("Serialized event {}", serialized);
            return serialized.getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Event couldnt be serialized", e);
            return new byte[0];
        }
    }
}
