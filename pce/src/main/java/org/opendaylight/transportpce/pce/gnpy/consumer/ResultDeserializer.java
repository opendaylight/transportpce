/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Result;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

//This class is a temporary workaround while waiting jackson
//support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "temporary class")
public class ResultDeserializer extends StdDeserializer<Result> {

    private static final long serialVersionUID = 1L;
    private  JsonStringConverter<Result> converter;
    private  YangInstanceIdentifier yangId;

    public ResultDeserializer(JsonStringConverter<Result> converter) {
        super(Result.class);
        this.converter = converter;
        QName pathQname = QName.create("gnpy:path", "2020-09-09", "result");
        yangId = YangInstanceIdentifier.of(pathQname);
    }

    @Override
    public Result deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        return converter
                .createDataObjectFromJsonString(yangId,
                        parser.readValueAsTree().toString(),
                        JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
    }

}
