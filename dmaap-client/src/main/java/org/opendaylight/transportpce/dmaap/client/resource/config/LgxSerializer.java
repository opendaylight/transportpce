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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.lgx.Lgx;

// This class is a temporary workaround while waiting jackson
// support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
public class LgxSerializer extends StdSerializer<Lgx> {
    private static final long serialVersionUID = 1L;

    public LgxSerializer() {
        super(Lgx.class);
    }

    @Override
    public void serialize(Lgx value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {
            gen.writeStartObject();
            gen.writeStringField("lgx-port-rack", value.getLgxPortRack());
            gen.writeStringField("lgx-port-shelf", value.getLgxPortShelf());
            gen.writeStringField("lgx-device-name", value.getLgxDeviceName());
            gen.writeStringField("lgx-port-name", value.getLgxPortName());
            gen.writeEndObject();
        }
    }
}
