/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.resource.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationService;

// This class is a temporary workaround while waiting jackson
// support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
public class PublishNotificationServiceSerializer extends StdSerializer<PublishNotificationService> {
    private static final long serialVersionUID = 1L;

    public PublishNotificationServiceSerializer() {
        super(PublishNotificationService.class);
    }

    @Override
    public void serialize(PublishNotificationService value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value != null) {
            gen.writeStartObject();
            gen.writeStringField("common-id", value.getCommonId());
            gen.writeStringField("message", value.getMessage());
            gen.writeStringField("response-failed", value.getResponseFailed());
            gen.writeStringField("service-name", value.getServiceName());
            gen.writeStringField("topic", value.getTopic());
            if (value.getOperationalState() != null) {
                gen.writeStringField("operational-state", value.getOperationalState().getName());
            }
            gen.writeObjectField("service-a-end", value.getServiceAEnd());
            gen.writeObjectField("service-z-end", value.getServiceZEnd());
            gen.writeEndObject();
        }
    }

}
