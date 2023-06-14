/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.get.notifications.alarm.service.output.NotificationsAlarmService;

public class NotificationAlarmServiceDeserializerTest extends AbstractTest {

    @Test
    void deserializeTest() throws IOException {
        JsonStringConverter<NotificationAlarmService> converter =
                new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        NotificationAlarmServiceDeserializer deserializer = new NotificationAlarmServiceDeserializer();
        Map<String, Object> configs = Map.of(ConfigConstants.CONVERTER, converter);
        deserializer.configure(configs, false);
        NotificationsAlarmService readEvent = deserializer.deserialize("Test",
                Files.readAllBytes(Paths.get("src/test/resources/event_alarm_service.json")));
        deserializer.close();
        assertEquals("service1", readEvent.getServiceName(), "Service name should be service1");
        assertEquals("The service is now inService", readEvent.getMessage(),
            "message should be The service is now inService");
    }
}
