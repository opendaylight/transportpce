/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.alarm.service.output.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.service.output.NotificationService;

public class SubscriberTest extends AbstractTest {
    private static final String TOPIC = "topic";
    private static final int PARTITION = 0;
    private MockConsumer<String, NotificationService> mockConsumer;
    private MockConsumer<String, NotificationAlarmService> mockConsumerAlarm;
    private Subscriber<org.opendaylight.yang.gen.v1
            .nbi.notifications.rev210813.NotificationService, NotificationService> subscriberService;
    private Subscriber<org.opendaylight.yang.gen.v1
            .nbi.notifications.rev210813.NotificationAlarmService, NotificationAlarmService> subscriberAlarmService;

    @Before
    public void setUp() {
        mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        mockConsumerAlarm = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        subscriberService = new Subscriber<>(mockConsumer);
        subscriberAlarmService = new Subscriber<>(mockConsumerAlarm);
    }

    @Test
    public void subscribeServiceShouldBeSuccessful() {
        // from https://www.baeldung.com/kafka-mockconsumer
        ConsumerRecord<String, NotificationService> record = new ConsumerRecord<>(
                TOPIC, PARTITION, 0L, "key", NotificationServiceDataUtils.buildReceivedEvent());
        mockConsumer.schedulePollTask(() -> {
            mockConsumer.rebalance(Collections.singletonList(new TopicPartition(TOPIC, PARTITION)));
            mockConsumer.addRecord(record);
        });

        Map<TopicPartition, Long> startOffsets = new HashMap<>();
        TopicPartition tp = new TopicPartition(TOPIC, PARTITION);
        startOffsets.put(tp, 0L);
        mockConsumer.updateBeginningOffsets(startOffsets);
        List<NotificationService> result = subscriberService.subscribe(TOPIC, NotificationService.QNAME);
        assertEquals("There should be 1 record", 1, result.size());
        assertTrue("Consumer should be closed", mockConsumer.closed());
    }

    @Test
    public void subscribeAlarmShouldBeSuccessful() {
        // from https://www.baeldung.com/kafka-mockconsumer
        ConsumerRecord<String, NotificationAlarmService> record = new ConsumerRecord<>(
                TOPIC, PARTITION, 0L, "key", NotificationServiceDataUtils.buildReceivedAlarmEvent());
        mockConsumerAlarm.schedulePollTask(() -> {
            mockConsumerAlarm.rebalance(Collections.singletonList(new TopicPartition(TOPIC, PARTITION)));
            mockConsumerAlarm.addRecord(record);
        });

        Map<TopicPartition, Long> startOffsets = new HashMap<>();
        TopicPartition tp = new TopicPartition(TOPIC, PARTITION);
        startOffsets.put(tp, 0L);
        mockConsumerAlarm.updateBeginningOffsets(startOffsets);
        List<NotificationAlarmService> result = subscriberAlarmService.subscribe(TOPIC, NotificationAlarmService.QNAME);
        assertEquals("There should be 1 record", 1, result.size());
        assertTrue("Consumer should be closed", mockConsumerAlarm.closed());
    }
}
