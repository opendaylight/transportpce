/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import com.google.gson.stream.JsonReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
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
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JsonDataConverter extends AbstractDataConverter {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDataConverter.class);
    private final JSONCodecFactory codecFactory;
    private final ConstantAdapterContext codec;

    public JsonDataConverter(Set<YangModuleInfo> models) {
        super(models);
        this.codecFactory = JSONCodecFactorySupplier.RFC7951.createLazy(getBindingRuntimeContext().modelContext());
        this.codec = new ConstantAdapterContext(getBindingCodecContext());
    }

    @Override
    public String serialize(DataObjectIdentifier id, DataObject dataContainer) throws ProcessingException {
        LOG.debug("Calling writer for {}", dataContainer);
        try (Writer writer = new StringWriter();
            var jsonWriter = JsonWriterFactory.createJsonWriter(writer, 4);) {
            var jsonStreamWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(codecFactory, jsonWriter);
            try (var nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStreamWriter);) {
                nodeWriter.write(codec.currentSerializer().toNormalizedDataObject(id, dataContainer).node());
                nodeWriter.flush();
            }
            return writer.toString();
        } catch (IOException e) {
            throw new ProcessingException("Error serializing a DataObject to the output stream", e);
        }
    }

    @Override
    public void serializeToFile(DataObjectIdentifier id, DataObject dataContainer, String filename)
            throws ProcessingException {
        try (FileWriter fileWriter = new FileWriter(filename, StandardCharsets.UTF_8)) {
            String output = serialize(id, dataContainer);
            fileWriter.write(output);
        } catch (IOException e) {
            throw new ProcessingException("Error serializing a DataObject to the output file", e);
        }
    }

    @Override
    public DataObject deserialize(String jsonValue, QName object) throws ProcessingException {
        LOG.debug("Calling writer for {}", jsonValue);

        final NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(object).build();
        Inference schema = SchemaInferenceStack.of(codecFactory.modelContext()).toInference();

        try (JsonParserStream jsonParser = JsonParserStream.create(writer, codecFactory, schema)) {
            JsonReader reader = new JsonReader(new StringReader(jsonValue));
            jsonParser.parse(reader);
            DataObject result = getBindingCodecContext()
                    .fromNormalizedNode(path, resultHolder.getResult().data())
                    .getValue();
            return result == null ? null : result;
        } catch (IOException e) {
            throw new ProcessingException("Error deserializing a Json String to a DataObject", e);
        }
    }

    @Override
    public DataObject deserialize(Reader data, QName object) throws ProcessingException {
        JsonReader jsonReader = new JsonReader(data);
        NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        YangInstanceIdentifier path = YangInstanceIdentifier.of(object);
        Inference schema = SchemaInferenceStack.of(codecFactory.modelContext()).toInference();

        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
                JsonParserStream jsonParser = JsonParserStream.create(streamWriter, codecFactory, schema);) {
            jsonParser.parse(jsonReader);
            DataObject result = getBindingCodecContext()
                    .fromNormalizedNode(path, resultHolder.getResult().data())
                    .getValue();
            return result == null ? null : result;
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Cannot deserialize JSON ", e);
            return null;
        }
    }
}
