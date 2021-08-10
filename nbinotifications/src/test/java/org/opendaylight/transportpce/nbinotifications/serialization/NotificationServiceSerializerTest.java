/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class NotificationServiceSerializerTest extends AbstractTest {

    @Test
    public void serializeTest() throws IOException {
        JsonStringConverter<NotificationService> converter =
                new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        String json = Files.readString(Paths.get("src/test/resources/event.json"));
        NotificationService notificationService = converter
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        NotificationServiceSerializer serializer = new NotificationServiceSerializer();
        Map<String, Object> configs = Map.of(ConfigConstants.CONVERTER, converter);
        serializer.configure(configs, false);
        byte[] data = serializer.serialize("test", notificationService);
        serializer.close();
        assertNotNull("Serialized data should not be null", data);
        String expectedJson = Files.readString(Paths.get("src/test/resources/expected_event.json"));
        // Minify the json string
        expectedJson = new ObjectMapper().readValue(expectedJson, JsonNode.class).toString();
        assertEquals("The event should be equals", expectedJson, new String(data, StandardCharsets.UTF_8));
    }
}
