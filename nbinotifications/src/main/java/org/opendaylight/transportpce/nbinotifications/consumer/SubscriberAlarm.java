/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
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
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.get.notifications.alarm.service.output.NotificationAlarmService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriberAlarm {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriberAlarm.class);

    private final Consumer<String, NotificationAlarmService> consumer;

    public SubscriberAlarm(String id, String groupId, String subscriberServer,
                           JsonStringConverter<org.opendaylight.yang.gen.v1
                .nbi.notifications.rev210628.NotificationAlarmService> deserializer) {
        Properties propsConsumer = NbiNotificationsUtils.loadProperties("subscriber.properties");
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        propsConsumer.put(ConsumerConfig.CLIENT_ID_CONFIG, id);
        propsConsumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsConsumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG , NotificationAlarmServiceDeserializer.class);
        propsConsumer.put(ConfigConstants.CONVERTER , deserializer);
        if (subscriberServer != null && !subscriberServer.isBlank()) {
            propsConsumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, subscriberServer);
        }
        LOG.info("Subscribing for group id {}, client config id {} with properties {}", groupId, id, propsConsumer);
        consumer = new KafkaConsumer<>(propsConsumer);
    }

    public List<NotificationAlarmService> subscribeAlarm(String topicName) {
        LOG.info("Subscribe request to topic '{}' ", topicName);
        consumer.subscribe(Collections.singleton(topicName));
        final ConsumerRecords<String, NotificationAlarmService> consumerRecords = consumer
                .poll(Duration.ofMillis(1000));
        List<NotificationAlarmService> notificationAlarmServiceList = new ArrayList<>();
        YangInstanceIdentifier.of(NotificationAlarmService.QNAME);
        for (ConsumerRecord<String, NotificationAlarmService> record : consumerRecords) {
            if (record.value() != null) {
                notificationAlarmServiceList.add(record.value());
            }
        }
        LOG.info("Getting records '{}' ", notificationAlarmServiceList);
        consumer.unsubscribe();
        consumer.close();
        return notificationAlarmServiceList;
    }

    @VisibleForTesting public SubscriberAlarm(Consumer<String, NotificationAlarmService> consumer) {
        this.consumer = consumer;
    }
}
