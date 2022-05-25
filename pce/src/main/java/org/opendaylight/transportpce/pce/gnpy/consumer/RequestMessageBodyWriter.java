/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestMessageBodyWriter implements MessageBodyWriter<Request> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestMessageBodyWriter.class);
    private final InstanceIdentifier<Request> idRequest = InstanceIdentifier.builder(Request.class).build();
    private final JsonStringConverter<Request> converter;

    public RequestMessageBodyWriter(JsonStringConverter<Request> converter) {
        this.converter = converter;
    }

    @Override
    @SuppressWarnings("java:S1872")
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return "org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request$$$codecImpl"
                .equals(type.getName())
                || "org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.RequestBuilder$RequestImpl"
                        .equals(type.getName());
    }

    @Override
    public void writeTo(Request request, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        // we have to use a string because GNPy does not support prefix
        String jsonStringFromDataObject = converter
                .createJsonStringFromDataObject(idRequest, request,
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02)
                .replace("gnpy-network-topology:", "");
        LOG.debug("Serialized request {}", jsonStringFromDataObject);
        entityStream.write(jsonStringFromDataObject
                .getBytes(StandardCharsets.UTF_8));
    }
}
