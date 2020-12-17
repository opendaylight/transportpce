/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscriber {
    private static final Logger LOG = LoggerFactory.getLogger(Subscriber.class);

    private final KafkaConsumer<String, String> consumer;

    public Subscriber(String id, String groupId) {
        Properties propsConsumer = NbiNotificationsUtils.loadProperties("subscriber.properties");
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        propsConsumer.put(ConsumerConfig.CLIENT_ID_CONFIG, id);
        Thread.currentThread().setContextClassLoader(null);
        consumer = new KafkaConsumer<>(propsConsumer);
    }

    public Subscriber(KafkaConsumer<String, String> consumer) {
        this.consumer = consumer;
    }

    public List<NotificationService> subscribeService(String topicName) {
        LOG.info("Subscribe request to topic '{}' ", topicName);
        consumer.subscribe(Collections.singleton(topicName));
        final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
        List<NotificationService> notificationServiceList = new ArrayList<>();
        for (ConsumerRecord<String, String> record : consumerRecords) {
            notificationServiceList.add(NbiNotificationsUtils
                    .deserializeNotificationService(new JSONObject(record.value())));
        }
        consumer.unsubscribe();
        consumer.close();
        return notificationServiceList;
    }
}
