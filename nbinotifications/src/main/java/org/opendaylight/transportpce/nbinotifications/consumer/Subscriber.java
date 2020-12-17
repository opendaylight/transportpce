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
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationServiceBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Subscriber {
    private static final Logger LOG = LoggerFactory.getLogger(Subscriber.class);

    private final KafkaConsumer<String, String> consumer;
    private final JsonStringConverter<org.opendaylight.yang.gen.v1
        .nbi.notifications.rev201130.NotificationService> deserializer;

    public Subscriber(String id, String groupId, JsonStringConverter<org.opendaylight.yang.gen
            .v1.nbi.notifications.rev201130.NotificationService> deserializer) {
        this.deserializer = deserializer;
        Properties propsConsumer = NbiNotificationsUtils.loadProperties("subscriber.properties");
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        propsConsumer.put(ConsumerConfig.CLIENT_ID_CONFIG, id);
        Thread.currentThread().setContextClassLoader(null);
        consumer = new KafkaConsumer<>(propsConsumer);
    }

    public Subscriber(KafkaConsumer<String, String> consumer,
            JsonStringConverter<org.opendaylight.yang.gen.v1
            .nbi.notifications.rev201130.NotificationService> deserializer) {
        this.consumer = consumer;
        this.deserializer = deserializer;
    }

    public List<NotificationService> subscribeService(String topicName) {
        LOG.info("Subscribe request to topic '{}' ", topicName);
        consumer.subscribe(Collections.singleton(topicName));
        final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
        List<NotificationService> notificationServiceList = new ArrayList<>();
        YangInstanceIdentifier.of(NotificationService.QNAME);
        for (ConsumerRecord<String, String> record : consumerRecords) {
            NotificationService event = mapFromStringValue(record.value());
            if (event != null) {
                notificationServiceList.add(event);
            }
        }
        consumer.unsubscribe();
        consumer.close();
        return notificationServiceList;
    }

    private NotificationService mapFromStringValue(String value) {
        // The message published is
        // org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService
        // we have to map it to
        // org.opendaylight.yang.gen
        // .v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService
        org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService mappedString = deserializer
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(
                        org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService.QNAME),
                        value);
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
