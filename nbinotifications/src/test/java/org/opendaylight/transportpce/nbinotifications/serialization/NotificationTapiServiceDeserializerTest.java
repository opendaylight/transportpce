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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.Notification;

public class NotificationTapiServiceDeserializerTest extends AbstractTest {

    @Test
    public void deserializeTest() throws IOException {
        JsonStringConverter<NotificationTapiService> converter = new JsonStringConverter<>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        TapiNotificationDeserializer deserializer = new TapiNotificationDeserializer();
        Map<String, Object> configs = Map.of(ConfigConstants.CONVERTER, converter);
        deserializer.configure(configs, false);
        Notification readEvent = deserializer.deserialize("76d8f07b-ead5-4132-8eb8-cf3fdef7e079",
                Files.readAllBytes(Paths.get("src/test/resources/tapi_event.json")));
        deserializer.close();
        assertEquals("Service uuid should be 76d8f07b-ead5-4132-8eb8-cf3fdef7e079",
            "76d8f07b-ead5-4132-8eb8-cf3fdef7e079", readEvent.getTargetObjectIdentifier().getValue());
    }
}
