/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javax.ws.rs.ext.ContextResolver;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev201022.Result;

public class JsonConfigurator implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JsonConfigurator(JsonStringConverter<Request> requestConverter,
            JsonStringConverter<Result> resultConverter) {
        mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new GnpyApiModule(requestConverter, resultConverter));
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

}
