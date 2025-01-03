/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.converter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonStringConverter<T extends DataObject> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonStringConverter.class);

    private BindingDOMCodecServices bindingDOMCodecServices;

    public JsonStringConverter(BindingDOMCodecServices bindingDOMCodecServices) {
        this.bindingDOMCodecServices = bindingDOMCodecServices;
    }

    /**
     * Create a json string from dataobject T.
     * @param id InstanceIdentifier
     * @param dataObject T
     * @param supplier RFC7951 or DRAFT_LHOTKA_NETMOD_YANG_JSON_02
     * @return Json string representation of the object
     * @throws IOException if something went wrong.
     */
    public String createJsonStringFromDataObject(final DataObjectIdentifier<T> id, T dataObject,
            JSONCodecFactorySupplier supplier) throws IOException {
        /*
         * This function needs : - context - scPath.getParent() -
         * scPath.getLastComponent().getNamespace(), -
         * JsonWriterFactory.createJsonWriter(writer)
         */

        JSONCodecFactory codecFactory = supplier
                .getShared(bindingDOMCodecServices.getRuntimeContext().modelContext());
        try (Writer writer = new StringWriter();
                JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer, 4)) {
            EffectiveStatementInference rootNode = SchemaInferenceStack
                .of(bindingDOMCodecServices.getRuntimeContext().modelContext())
                .toInference();
            rootNode.modelContext();
            NormalizedNodeStreamWriter jsonStreamWriter = JSONNormalizedNodeStreamWriter
                .createExclusiveWriter(codecFactory, rootNode, EffectiveModelContext.NAME.getNamespace(), jsonWriter);
            try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStreamWriter)) {
                nodeWriter.write(bindingDOMCodecServices.toNormalizedDataObject(id, dataObject).node());
                nodeWriter.flush();
            }
            JsonObject asJsonObject = JsonParser.parseString(writer.toString()).getAsJsonObject();
            return new Gson().toJson(asJsonObject);
        } catch (IOException e) {
            LOG.error("Cannot convert object {} to string ", dataObject);
            throw e;
        }
    }

    /**
     * Create a dataObject of T type from json string.
     * @param path YangInstanceIdentifier
     * @param jsonString String
     * @param supplier RFC7951 or DRAFT_LHOTKA_NETMOD_YANG_JSON_02
     * @return T the created object.
     */
    @SuppressWarnings("unchecked")
    public T createDataObjectFromJsonString(YangInstanceIdentifier path, String jsonString,
            JSONCodecFactorySupplier supplier) {
        return createDataObjectFromReader(path, new StringReader(jsonString), supplier);
    }

    public T createDataObjectFromInputStream(YangInstanceIdentifier path, InputStream jsonStream,
                                            JSONCodecFactorySupplier supplier) {
        return createDataObjectFromReader(path, new InputStreamReader(jsonStream, StandardCharsets.UTF_8), supplier);
    }

    /**
     * Create a dataObject of T type from Reader.
     * @param path YangInstanceIdentifier
     * @param inputReader Reader (could be all class implementing Reader) containing Json data.
     * @param supplier RFC7951 or DRAFT_LHOTKA_NETMOD_YANG_JSON_02
     * @return the created object.
     */
    @SuppressWarnings("unchecked")
    private T createDataObjectFromReader(YangInstanceIdentifier path, Reader inputReader,
                                         JSONCodecFactorySupplier supplier) {

        NormalizationResultHolder result = new NormalizationResultHolder();
        try (JsonReader reader = new JsonReader(inputReader);
             NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
             JsonParserStream jsonParser = JsonParserStream
                     .create(
                             streamWriter,
                             supplier.getShared(bindingDOMCodecServices
                                     .getRuntimeContext().modelContext()))) {
            jsonParser.parse(reader);
            return (T) bindingDOMCodecServices.fromNormalizedNode(path, result.getResult().data()).getValue();
        } catch (IOException e) {
            LOG.warn("An error occured during parsing input reader", e);
            return null;
        }
    }
}
