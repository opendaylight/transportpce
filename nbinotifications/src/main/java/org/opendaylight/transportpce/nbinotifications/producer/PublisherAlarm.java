/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
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
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NotificationAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublisherAlarm {
    private static final Logger LOG = LoggerFactory.getLogger(PublisherAlarm.class);

    private final String id;
    private final Producer<String, NotificationAlarmService> producer;

    public PublisherAlarm(String id, String publisherServer, JsonStringConverter<NotificationAlarmService> serializer) {
        Properties properties = NbiNotificationsUtils.loadProperties("publisher.properties");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, id);
        if (publisherServer != null && !publisherServer.isBlank()) {
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, publisherServer);
        }
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG , NotificationAlarmServiceSerializer.class);
        properties.put(ConfigConstants.CONVERTER , serializer);
        LOG.info("Creationg publisher for id {} with properties {}", id, properties);
        producer = new KafkaProducer<>(properties);
        this.id = id;
    }

    @VisibleForTesting
    PublisherAlarm(String id, Producer<String, NotificationAlarmService> producer) {
        this.producer = producer;
        this.id = id;
    }

    public void close() {
        producer.close();
    }

    public void sendEvent(NotificationAlarmService notificationAlarmService) {
        LOG.info("SendEvent request to topic '{}' ", notificationAlarmService.getConnectionType().getName());
        producer.send(new ProducerRecord<>("alarm" + notificationAlarmService.getConnectionType().getName(),
                id, notificationAlarmService));
        producer.flush();
    }
}
