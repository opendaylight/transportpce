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
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceSerializer;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NotificationService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class PublisherTest extends AbstractTest {
    private JsonStringConverter<NotificationService> converter;
    private Publisher publisher;
    private MockProducer<String, NotificationService> mockProducer;

    @Before
    public void setUp() {
        NotificationServiceSerializer serializer = new NotificationServiceSerializer();
        Map<String, Object> properties = Map.of(ConfigConstants.CONVERTER , serializer);
        serializer.configure(properties, false);
        mockProducer =  new MockProducer<>(true, new StringSerializer(), serializer);
        converter = new JsonStringConverter<NotificationService>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        publisher = new Publisher("test",mockProducer);
    }

    @Test
    public void sendEventShouldBeSuccessful() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/event.json"));
        NotificationService notificationService = converter
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        publisher.sendEvent(notificationService);
        assertEquals("We should have one message", 1, mockProducer.history().size());
        assertEquals("Key should be test", "test",mockProducer.history().get(0).key());
    }

}
