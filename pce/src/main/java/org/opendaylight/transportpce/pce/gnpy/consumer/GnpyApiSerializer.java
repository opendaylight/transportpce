/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//This class is a temporary workaround while waiting jackson
//support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "temporary class")
public class GnpyApiSerializer extends StdSerializer<GnpyApi> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(GnpyApiSerializer.class);
    private JsonStringConverter<GnpyApi> converter;
    private InstanceIdentifier<GnpyApi> idGnpyApi = InstanceIdentifier.builder(GnpyApi.class).build();

    public GnpyApiSerializer(JsonStringConverter<GnpyApi> converter) {
        super(GnpyApi.class);
        this.converter = converter;
    }

    @Override
    public void serialize(GnpyApi value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String requestStr = converter
                .createJsonStringFromDataObject(idGnpyApi, value,
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
        requestStr =  requestStr.replace("gnpy-eqpt-config:", "")
                .replace("gnpy-path-computation-simplified:", "").replace("gnpy-network-topology:", "");
        LOG.info("Serialized request {}", requestStr);
        gen.writeRaw(requestStr);
    }

}
