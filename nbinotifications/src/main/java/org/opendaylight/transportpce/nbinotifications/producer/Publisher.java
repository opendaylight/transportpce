/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.producer;

import com.google.common.annotations.VisibleForTesting;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.serialization.ConfigConstants;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Publisher<T extends DataObject> {
    private static final Logger LOG = LoggerFactory.getLogger(Publisher.class);

    private final String id;
    private final Producer<String, T> producer;

    public Publisher(String id, String publisherServer, JsonStringConverter<T> serializer, Class<?> serializerConf) {
        Properties properties = NbiNotificationsUtils.loadProperties("publisher.properties");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, id);
        if (publisherServer != null && !publisherServer.isBlank()) {
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, publisherServer);
        }
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG , serializerConf);
        properties.put(ConfigConstants.CONVERTER , serializer);
        LOG.info("Creation publisher for id {} with properties {}", id, properties);
        producer = new KafkaProducer<>(properties);
        this.id = id;
    }

    @VisibleForTesting Publisher(String id, Producer<String, T> producer) {
        this.producer = producer;
        this.id = id;
    }

    public void close() {
        producer.close();
    }

    public void sendEvent(T notification, String topic) {
        LOG.info("SendEvent request to topic '{}' ", topic);
        producer.send(new ProducerRecord<>(topic, id, notification));
        producer.flush();
    }
}
