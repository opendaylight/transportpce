/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.producer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.serialization.ConfigConstants;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceSerializer;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class PublisherTest extends AbstractTest {
    private JsonStringConverter<NotificationService> converterService;
    private JsonStringConverter<NotificationAlarmService> converterAlarm;
    private Publisher<NotificationService> publisherService;
    private Publisher<NotificationAlarmService> publisherAlarm;
    private MockProducer<String, NotificationService> mockProducer;
    private MockProducer<String, NotificationAlarmService> mockAlarmProducer;

    @Before
    public void setUp() {
        NotificationServiceSerializer serializerService = new NotificationServiceSerializer();
        NotificationAlarmServiceSerializer serializerAlarm = new NotificationAlarmServiceSerializer();
        Map<String, Object> properties = Map.of(ConfigConstants.CONVERTER, serializerService);
        Map<String, Object> propertiesAlarm = Map.of(ConfigConstants.CONVERTER, serializerAlarm);
        serializerService.configure(properties, false);
        serializerAlarm.configure(propertiesAlarm, false);
        mockProducer = new MockProducer<>(true, new StringSerializer(), serializerService);
        mockAlarmProducer = new MockProducer<>(true, new StringSerializer(), serializerAlarm);
        converterService = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterAlarm = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        publisherService = new Publisher<>("test", mockProducer);
        publisherAlarm = new Publisher<>("test", mockAlarmProducer);
    }

    @Test
    public void sendEventServiceShouldBeSuccessful() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/event.json"));
        NotificationService notificationService = converterService
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        publisherService.sendEvent(notificationService, notificationService.getConnectionType().name());
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
}
