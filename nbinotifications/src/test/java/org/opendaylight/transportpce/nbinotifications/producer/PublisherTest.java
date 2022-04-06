/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.producer;

import static org.junit.Assert.assertEquals;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsImpl;
import org.opendaylight.transportpce.nbinotifications.serialization.ConfigConstants;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.TapiNotificationSerializer;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.create.notification.subscription.service.input.SubscriptionFilter;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.create.notification.subscription.service.input.SubscriptionFilterBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class PublisherTest extends AbstractTest {
    private JsonStringConverter<NotificationProcessService> converterService;
    private JsonStringConverter<NotificationAlarmService> converterAlarm;
    private JsonStringConverter<NotificationTapiService> converterTapiService;
    private Publisher<NotificationProcessService> publisherService;
    private Publisher<NotificationAlarmService> publisherAlarm;
    private Publisher<NotificationTapiService> publisherTapiService;
    private MockProducer<String, NotificationProcessService> mockProducer;
    private MockProducer<String, NotificationAlarmService> mockAlarmProducer;
    private MockProducer<String, NotificationTapiService> mockTapiProducer;
    private NbiNotificationsImpl nbiNotificationsImpl;
    private TopicManager topicManager;

    public static NetworkTransactionService networkTransactionService;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        topicManager = TopicManager.getInstance();
        converterService = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterAlarm = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterTapiService = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        NotificationServiceSerializer serializerService = new NotificationServiceSerializer();
        NotificationAlarmServiceSerializer serializerAlarm = new NotificationAlarmServiceSerializer();
        TapiNotificationSerializer serializerTapi = new TapiNotificationSerializer();
        Map<String, Object> properties = Map.of(ConfigConstants.CONVERTER, converterService);
        Map<String, Object> propertiesAlarm = Map.of(ConfigConstants.CONVERTER, converterAlarm);
        Map<String, Object> propertiesTapi = Map.of(ConfigConstants.CONVERTER, converterTapiService);
        serializerService.configure(properties, false);
        serializerAlarm.configure(propertiesAlarm, false);
        serializerTapi.configure(propertiesTapi, false);
        mockProducer = new MockProducer<>(true, new StringSerializer(), serializerService);
        mockAlarmProducer = new MockProducer<>(true, new StringSerializer(), serializerAlarm);
        mockTapiProducer = new MockProducer<>(true, new StringSerializer(), serializerTapi);
        publisherService = new Publisher<>("test", mockProducer);
        publisherAlarm = new Publisher<>("test", mockAlarmProducer);
        publisherTapiService = new Publisher<>("test", mockTapiProducer);
        MockitoAnnotations.openMocks(this);
        networkTransactionService = new NetworkTransactionImpl(
            new RequestProcessor(getDataStoreContextUtil().getDataBroker()));
        topicManager.setTapiConverter(converterTapiService);
        NotificationServiceDataUtils.createTapiContext(networkTransactionService);
        nbiNotificationsImpl = new NbiNotificationsImpl(converterService, converterAlarm, converterTapiService,
            "localhost:8080", networkTransactionService, topicManager);
    }

    @Test
    public void sendEventServiceShouldBeSuccessful() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/event.json"));
        NotificationProcessService notificationProcessService = converterService
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationProcessService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        publisherService.sendEvent(notificationProcessService, notificationProcessService.getConnectionType().name());
        assertEquals("We should have one message", 1, mockProducer.history().size());
        assertEquals("Key should be test", "test", mockProducer.history().get(0).key());
    }

    @Test
    public void sendEventAlarmShouldBeSuccessful() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/event_alarm_service.json"));
        NotificationAlarmService notificationAlarmService = converterAlarm
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationAlarmService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        publisherAlarm.sendEvent(notificationAlarmService, "alarm"
                + notificationAlarmService.getConnectionType().getName());
        assertEquals("We should have one message", 1, mockAlarmProducer.history().size());
        assertEquals("Key should be test", "test", mockAlarmProducer.history().get(0).key());
    }

    @Test
    public void sendTapiEventShouldBeSuccessful() throws IOException {
        CreateNotificationSubscriptionServiceInputBuilder builder
            = NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder();
        SubscriptionFilter subscriptionFilter = new SubscriptionFilterBuilder(builder.getSubscriptionFilter())
            .setRequestedObjectIdentifier(List.of(new Uuid("76d8f07b-ead5-4132-8eb8-cf3fdef7e079"))).build();
        builder.setSubscriptionFilter(subscriptionFilter);
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
            nbiNotificationsImpl.createNotificationSubscriptionService(builder.build());
        String json = Files.readString(Paths.get("src/test/resources/tapi_event.json"));
        NotificationTapiService notificationTapiService = converterTapiService
            .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationTapiService.QNAME),
                json, JSONCodecFactorySupplier.RFC7951);
        publisherTapiService.sendEvent(notificationTapiService, "");
        assertEquals("We should have one message", 1, mockTapiProducer.history().size());
        assertEquals("Key should be test", "test", mockTapiProducer.history().get(0).key());
    }
}
