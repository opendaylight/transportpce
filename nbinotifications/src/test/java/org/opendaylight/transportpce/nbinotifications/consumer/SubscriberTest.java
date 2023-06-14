/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.consumer;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.Notification;

public class SubscriberTest extends AbstractTest {
    private static final String TOPIC = "topic";
    private static final int PARTITION = 0;
    private MockConsumer<String, NotificationsProcessService> mockConsumer;
    private MockConsumer<String, NotificationsAlarmService> mockConsumerAlarm;
    private MockConsumer<String, Notification> mockConsumerTapi;
    private Subscriber<NotificationProcessService, NotificationsProcessService> subscriberService;
    private Subscriber<NotificationAlarmService, NotificationsAlarmService> subscriberAlarmService;
    private Subscriber<NotificationTapiService, Notification> subscriberTapiService;

    @BeforeEach
    void setUp() {
        mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        mockConsumerAlarm = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        mockConsumerTapi = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        subscriberService = new Subscriber<>(mockConsumer);
        subscriberAlarmService = new Subscriber<>(mockConsumerAlarm);
        subscriberTapiService = new Subscriber<>(mockConsumerTapi);
    }

    @Test
    void subscribeServiceShouldBeSuccessful() {
        // from https://www.baeldung.com/kafka-mockconsumer
        ConsumerRecord<String, NotificationsProcessService> record = new ConsumerRecord<>(
                TOPIC, PARTITION, 0L, "key", NotificationServiceDataUtils.buildReceivedEvent());
        mockConsumer.schedulePollTask(() -> {
            mockConsumer.rebalance(Collections.singletonList(new TopicPartition(TOPIC, PARTITION)));
            mockConsumer.addRecord(record);
        });

        Map<TopicPartition, Long> startOffsets = new HashMap<>();
        TopicPartition tp = new TopicPartition(TOPIC, PARTITION);
        startOffsets.put(tp, 0L);
        mockConsumer.updateBeginningOffsets(startOffsets);
        List<NotificationsProcessService> result = subscriberService.subscribe(TOPIC,
                NotificationsProcessService.QNAME);
        assertEquals(1, result.size(), "There should be 1 record");
        assertTrue(mockConsumer.closed(), "Consumer should be closed");
    }

    @Test
    void subscribeAlarmShouldBeSuccessful() {
        // from https://www.baeldung.com/kafka-mockconsumer
        ConsumerRecord<String, NotificationsAlarmService> record = new ConsumerRecord<>(
                TOPIC, PARTITION, 0L, "key", NotificationServiceDataUtils.buildReceivedAlarmEvent());
        mockConsumerAlarm.schedulePollTask(() -> {
            mockConsumerAlarm.rebalance(Collections.singletonList(new TopicPartition(TOPIC, PARTITION)));
            mockConsumerAlarm.addRecord(record);
        });

        Map<TopicPartition, Long> startOffsets = new HashMap<>();
        TopicPartition tp = new TopicPartition(TOPIC, PARTITION);
        startOffsets.put(tp, 0L);
        mockConsumerAlarm.updateBeginningOffsets(startOffsets);
        List<NotificationsAlarmService> result = subscriberAlarmService.subscribe(TOPIC,
                NotificationsAlarmService.QNAME);
        assertEquals(1, result.size(), "There should be 1 record");
        assertTrue(mockConsumerAlarm.closed(), "Consumer should be closed");
    }

    @Test
    void subscribeTapiAlarmShouldBeSuccessful() {
        // from https://www.baeldung.com/kafka-mockconsumer
        ConsumerRecord<String, Notification> record = new ConsumerRecord<>(
            TOPIC, PARTITION, 0L, "key", NotificationServiceDataUtils.buildReceivedTapiAlarmEvent());
        mockConsumerTapi.schedulePollTask(() -> {
            mockConsumerTapi.rebalance(Collections.singletonList(new TopicPartition(TOPIC, PARTITION)));
            mockConsumerTapi.addRecord(record);
        });

        Map<TopicPartition, Long> startOffsets = new HashMap<>();
        TopicPartition tp = new TopicPartition(TOPIC, PARTITION);
        startOffsets.put(tp, 0L);
        mockConsumerTapi.updateBeginningOffsets(startOffsets);
        List<Notification> result = subscriberTapiService.subscribe(TOPIC,
            NotificationTapiService.QNAME);
        assertEquals(1, result.size(), "There should be 1 record");
        assertTrue(mockConsumerTapi.closed(), "Consumer should be closed");
    }
}
