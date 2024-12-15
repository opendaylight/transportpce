/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.Result;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultDeserializer  extends JsonDeserializer<Result> {

    private static final Logger LOG = LoggerFactory.getLogger(ResultDeserializer.class);
    private JsonStringConverter<Result> resultConverter;

    public ResultDeserializer(JsonStringConverter<Result> resultConverter) {
        this.resultConverter = resultConverter;
    }

    @Override
    public Result deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
        String value = parser.readValueAsTree().toString();
        LOG.info(value);
        return resultConverter.createDataObjectFromJsonString(
                YangInstanceIdentifier.of(Result.QNAME),
                value,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
    }

}
