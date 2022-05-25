/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220221.Result;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

public class ResultMessageBodyReader implements MessageBodyReader<Result> {
    private final JsonStringConverter<Result> converter;
    private final YangInstanceIdentifier yangId;

    public ResultMessageBodyReader(JsonStringConverter<Result> converter) {
        QName pathQname = Result.QNAME;
        yangId = YangInstanceIdentifier.of(pathQname);
        this.converter = converter;
    }

    @Override
    @SuppressWarnings("java:S1872")
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return "org.opendaylight.yang.gen.v1.gnpy.path.rev220221.Result"
                .equals(type.getName());
    }

    @Override
    public Result readFrom(Class<Result> type, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return converter.createDataObjectFromInputStream(yangId, entityStream,
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
    }
}
