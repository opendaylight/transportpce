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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.serialization.ConfigConstants;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscriber<T extends DataObject, D> {
    private static final Logger LOG = LoggerFactory.getLogger(Subscriber.class);

    private final Consumer<String, D> consumer;

    public Subscriber(String id, String groupId, String subscriberServer, JsonStringConverter<T> deserializer,
                      Class<?> deserializerConf) {
        Properties propsConsumer = NbiNotificationsUtils.loadProperties("subscriber.properties");
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        propsConsumer.put(ConsumerConfig.CLIENT_ID_CONFIG, id);
        propsConsumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsConsumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG , deserializerConf);
        propsConsumer.put(ConfigConstants.CONVERTER , deserializer);
        if (subscriberServer != null && !subscriberServer.isBlank()) {
            propsConsumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, subscriberServer);
        }
        LOG.info("Subscribing for group id {}, client config id {} with properties {}", groupId, id, propsConsumer);
        consumer = new KafkaConsumer<>(propsConsumer);
    }

    public List<D> subscribe(String topicName, @NonNull QName name) {
        LOG.info("Subscribe request to topic '{}' ", topicName);
        consumer.subscribe(Collections.singleton(topicName));
        final ConsumerRecords<String, D> consumerRecords = consumer.poll(Duration.ofMillis(1000));
        List<D> notificationServiceList = new ArrayList<>();
        YangInstanceIdentifier.of(name);
        for (ConsumerRecord<String, D> record : consumerRecords) {
            if (record.value() != null) {
                notificationServiceList.add(record.value());
            }
        }
        LOG.info("Getting records '{}' ", notificationServiceList);
        consumer.unsubscribe();
        consumer.close();
        return notificationServiceList;
    }

    @VisibleForTesting public Subscriber(Consumer<String, D> consumer) {
        this.consumer = consumer;
    }
}
