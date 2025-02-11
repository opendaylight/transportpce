/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.skyscreamer.jsonassert.JSONAssert;

public class NotificationTapiServiceSerializerTest extends AbstractTest {

    @Test
    void serializeTest() throws IOException, JSONException {
        JsonStringConverter<NotificationTapiService> converter =
                new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        String json = Files.readString(Path.of("src/test/resources/tapi_event.json"));
        NotificationTapiService notificationService = converter
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationTapiService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        TapiNotificationSerializer serializer = new TapiNotificationSerializer();
        Map<String, Object> configs = Map.of(ConfigConstants.CONVERTER, converter);
        serializer.configure(configs, false);
        byte[] data = serializer.serialize("test", notificationService);
        serializer.close();
        assertNotNull(data, "Serialized data should not be null");
        String expectedJson = Files.readString(Path.of("src/test/resources/expected_tapi_event.json"));
        // Minify the json string
        expectedJson = new ObjectMapper().readValue(expectedJson, JsonNode.class).toString();
        JSONAssert.assertEquals(expectedJson, new String(data, StandardCharsets.UTF_8), true);
    }
}
