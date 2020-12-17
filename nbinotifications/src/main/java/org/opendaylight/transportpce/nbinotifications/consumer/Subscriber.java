/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.consumer;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.serialization.ConfigConstants;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscriber {
    private static final Logger LOG = LoggerFactory.getLogger(Subscriber.class);

    private final Consumer<String, NotificationService> consumer;

    public Subscriber(String id, String groupId, String suscriberServer,
            JsonStringConverter<org.opendaylight.yang.gen.v1
                .nbi.notifications.rev201130.NotificationService> deserializer) {
        Properties propsConsumer = NbiNotificationsUtils.loadProperties("subscriber.properties");
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        propsConsumer.put(ConsumerConfig.CLIENT_ID_CONFIG, id);
        propsConsumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsConsumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG , NotificationServiceDeserializer.class);
        propsConsumer.put(ConfigConstants.CONVERTER , deserializer);
        if (suscriberServer != null && !suscriberServer.isBlank()) {
            propsConsumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, suscriberServer);
        }
        LOG.info("Suscribing for group id {}, client config id {} with properties {}", groupId, id, propsConsumer);
        consumer = new KafkaConsumer<>(propsConsumer);
    }

    public List<NotificationService> subscribeService(String topicName) {
        LOG.info("Subscribe request to topic '{}' ", topicName);
        consumer.subscribe(Collections.singleton(topicName));
        final ConsumerRecords<String, NotificationService> consumerRecords = consumer.poll(Duration.ofMillis(1000));
        List<NotificationService> notificationServiceList = new ArrayList<>();
        YangInstanceIdentifier.of(NotificationService.QNAME);
        for (ConsumerRecord<String, NotificationService> record : consumerRecords) {
            if (record.value() != null) {
                notificationServiceList.add(record.value());
            }
        }
        LOG.info("Getting records '{}' ", notificationServiceList);
        consumer.unsubscribe();
        consumer.close();
        return notificationServiceList;
    }

    @VisibleForTesting public Subscriber(Consumer<String, NotificationService> consumer) {
        this.consumer = consumer;
    }
}
