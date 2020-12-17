/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.consumer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService;

public class SubscriberTest {

    @Mock
    private KafkaConsumer<String, String> consumer;

    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
        MockitoAnnotations.openMocks(this);
        Properties propsConsumer = NbiNotificationsUtils.loadProperties("subscriber.properties");
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, "groupId");
        propsConsumer.put(ConsumerConfig.CLIENT_ID_CONFIG, "ID");
        consumer = new KafkaConsumer<>(propsConsumer);
    }

    @Test
    public void subscribeServiceShouldBeSuccessful() {
        Subscriber subscriber = new Subscriber(consumer);
        List<NotificationService> result = subscriber.subscribeService("topicName");
        Assert.assertEquals(new ArrayList<NotificationService>(), result);
    }

}
