/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.get.notifications.service.output.NotificationService;

public class NotificationServiceDeserializerTest extends AbstractTest {

    @Test
    public void deserializeTest() throws IOException {
        JsonStringConverter<org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.NotificationService> converter =
                new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        NotificationServiceDeserializer deserializer = new NotificationServiceDeserializer();
        Map<String, Object> configs = Map.of(ConfigConstants.CONVERTER, converter);
        deserializer.configure(configs, false);
        NotificationService readEvent = deserializer.deserialize("Test",
                Files.readAllBytes(Paths.get("src/test/resources/event.json")));
        deserializer.close();
        assertEquals("Service name should be service1", "service1", readEvent.getServiceName());
    }
}
