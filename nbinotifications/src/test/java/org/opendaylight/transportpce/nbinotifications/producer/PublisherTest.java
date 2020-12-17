/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.producer;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationServiceBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class PublisherTest extends AbstractTest {

    @Mock
    private KafkaProducer<String, String> producer;

    @Mock
    private AdminClient client;

    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;
    private JsonStringConverter<NotificationService> converter;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
        MockitoAnnotations.openMocks(this);
        Properties propsProducer = NbiNotificationsUtils.loadProperties("publisher.properties");
        propsProducer.put(ProducerConfig.CLIENT_ID_CONFIG, "test");
        producer = new KafkaProducer<>(propsProducer);
        Properties propsClient = new Properties();
        propsClient.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                propsProducer.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        client = KafkaAdminClient.create(propsClient);
        converter = new JsonStringConverter<NotificationService>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
    }

    @Test(expected = NullPointerException.class)
    public void sendEventShouldBeFailedWithEmptyInput() {
        Publisher publisher = new Publisher("test", producer, client, converter);
        publisher.sendEvent(new NotificationServiceBuilder().build());
    }

    @Test
    public void sendEventShouldBeSuccessful() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/event.json"));
        NotificationService notificationService = converter
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        Publisher publisher = new Publisher("test", producer, client, converter);
        publisher.sendEvent(notificationService);
    }

}
