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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.notification.service.ServiceAEnd;

// This class is a temporary workaround while waiting jackson
// support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
public class ServiceAEndSerializer extends StdSerializer<ServiceAEnd> {
    private static final long serialVersionUID = 1L;

    public ServiceAEndSerializer() {
        super(ServiceAEnd.class);
    }

    @Override
    public void serialize(ServiceAEnd value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {
            gen.writeStartObject();
            gen.writeStringField("clli", value.getClli());
            if (value.getServiceFormat() != null) {
                gen.writeStringField("service-format", value.getServiceFormat().getName());
            }
            if (value.getNodeId() != null) {
                gen.writeStringField("node-id", value.getNodeId().getValue());
            }
            if (value.getServiceRate() != null) {
                gen.writeNumberField("service-rate", value.getServiceRate().intValue());
            }
            if (value.getOpticType() != null) {
                gen.writeStringField("optic-type", value.getOpticType().getName());
            }
            if (value.getTxDirection() != null) {
                gen.writeObjectField("tx-direction", value.getTxDirection());
            }
            if (value.getRxDirection() != null) {
                gen.writeObjectField("rx-direction", value.getRxDirection());
            }
            gen.writeEndObject();
        }
    }
}
