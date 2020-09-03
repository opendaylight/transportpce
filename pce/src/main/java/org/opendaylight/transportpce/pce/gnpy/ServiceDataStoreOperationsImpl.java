/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {

    private static final JsonParser PARSER = new JsonParser();
    private BindingDOMCodecServices bindingDOMCodecServices;

    public ServiceDataStoreOperationsImpl(BindingDOMCodecServices bindingDOMCodecServices) throws GnpyException {
        this.bindingDOMCodecServices = bindingDOMCodecServices;
    }

    @Override
    public String createJsonStringFromDataObject(final InstanceIdentifier<GnpyApi> id, GnpyApi object)
        throws GnpyException {
        final SchemaPath scPath = SchemaPath.create(FluentIterable
                .from(id.getPathArguments())
                .transform(input -> BindingReflections.findQName(input.getType())), true);
        /*
         * This function needs : - context - scPath.getParent() -
         * scPath.getLastComponent().getNamespace(), -
         * JsonWriterFactory.createJsonWriter(writer)
         */

        JSONCodecFactory codecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .getShared(bindingDOMCodecServices.getRuntimeContext().getEffectiveModelContext());
        try (Writer writer = new StringWriter();
                JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer, 2);) {
            NormalizedNodeStreamWriter jsonStreamWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                    codecFactory, scPath.getParent(), scPath.getLastComponent().getNamespace(), jsonWriter);
            try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStreamWriter)) {
                nodeWriter.write(bindingDOMCodecServices.toNormalizedNode(id, object).getValue());
                nodeWriter.flush();
            }
            JsonObject asJsonObject = PARSER.parse(writer.toString()).getAsJsonObject();
            return new Gson().toJson(asJsonObject);
        } catch (IOException e) {
            throw new GnpyException("Cannot convert data to Json string", e);
        }
    }

    // Write the json as a string in a file
    @Override
    public void writeStringFile(String jsonString, String fileName) throws GnpyException {
        try (FileWriter file = new FileWriter(fileName,StandardCharsets.UTF_8)) {
            file.write(jsonString);
        } catch (IOException e) {
            throw new GnpyException("In ServiceDataStoreOperationsImpl : exception during file writing",e);
        }
    }
}
