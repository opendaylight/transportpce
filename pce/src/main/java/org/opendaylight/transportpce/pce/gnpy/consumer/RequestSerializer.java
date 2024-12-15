/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class RequestSerializer extends JsonSerializer<Request> {
    private JsonStringConverter<Request> requestConverter;

    public RequestSerializer(JsonStringConverter<Request> requestConverter) {
        this.requestConverter = requestConverter;
    }
    @Override
    public void serialize(Request value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeRaw(requestConverter.createJsonStringFromDataObject(
                InstanceIdentifier.create(Request.class),
                value,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02)
                .replace("gnpy-network-topology:", ""));
    }

}
