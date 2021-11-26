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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.port.Port;

// This class is a temporary workaround while waiting jackson
// support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
public class PortSerializer extends StdSerializer<Port> {
    private static final long serialVersionUID = 1L;

    public PortSerializer() {
        super(Port.class);
    }

    @Override
    public void serialize(Port value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {
            gen.writeStartObject();
            gen.writeStringField("port-rack", value.getPortRack());
            gen.writeStringField("port-shelf", value.getPortShelf());
            gen.writeStringField("port-device-name", value.getPortDeviceName());
            gen.writeStringField("port-name", value.getPortName());
            gen.writeStringField("port-type", value.getPortType());
            gen.writeEndObject();
        }
    }
}
