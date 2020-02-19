/*
 * Copyright (c) 2018 AT&T and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.binding.converter;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import io.fd.honeycomb.transportpce.binding.converter.api.DataObjectConverter;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;

import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JSONDataObjectConverter extends AbstractDataObjectConverter {

    private static final Logger LOG = LoggerFactory.getLogger(JSONDataObjectConverter.class);

    private JSONDataObjectConverter(SchemaContext schemaContext, BindingNormalizedNodeCodecRegistry codecRegistry) {
        super(schemaContext, codecRegistry);
    }

    /**
     * extracts codec and schema context (?).
     *
     * @param dataStoreContextUtil datastore context util used to extract codec and schema context
     * @return {@link AbstractDataObjectConverter}
     */
    public static DataObjectConverter createWithDataStoreUtil(@Nonnull DataStoreContext dataStoreContextUtil) {
        return new JSONDataObjectConverter(dataStoreContextUtil.getSchemaContext(),
                dataStoreContextUtil.getBindingToNormalizedNodeCodec());
    }

    /**
     * extracts codec and schema context (?).
     *
     * @param schemaContext schema context for converter
     * @param codecRegistry codec registry used for converting
     * @return converter
     */
    public static DataObjectConverter createWithSchemaContext(@Nonnull SchemaContext schemaContext,
            @Nonnull BindingNormalizedNodeCodecRegistry codecRegistry) {
        return new JSONDataObjectConverter(schemaContext, codecRegistry);
    }

    /**
     * Transforms the JSON input stream into normalized nodes.
     *
     * @param inputStream of the given JSON
     * @return {@link Optional} instance of {@link NormalizedNode}.
     */
    @Override
    public Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> transformIntoNormalizedNode(
            @Nonnull InputStream inputStream) {
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            return parseInputJSON(reader);
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> transformIntoNormalizedNode(
            @Nonnull Reader inputReader, SchemaNode parentSchema) {
        throw new UnsupportedOperationException("Not Implemented yet");
    }

    @Override
    public Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> transformIntoNormalizedNode(
            @Nonnull Reader inputReader) {
        JsonReader reader = new JsonReader(inputReader);
        return parseInputJSON(reader);
    }

    @Override
    public <T extends DataObject> Writer writerFromDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType) {
        Writer writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        JSONCodecFactory jsonCodecFactory = JSONCodecFactory.createLazy(getSchemaContext());
        NormalizedNodeStreamWriter create =
                JSONNormalizedNodeStreamWriter.createExclusiveWriter(jsonCodecFactory, null, null, jsonWriter);

        try (NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(create);) {
            normalizedNodeWriter
                    .write(convertType.toNormalizedNodes(dataObjectClass.cast(object), dataObjectClass).get());
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        return writer;
    }

    @Override
    public <T extends DataObject> Writer writerFromRpcDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType, QName rpcOutputQName, String rpcName) {
        return null;
    }

    /**
     * Parses the input json with concrete implementation of {@link JsonParserStream}.
     *
     * @param reader of the given JSON
     *
     */
    private Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> parseInputJSON(
            JsonReader reader) {
        NormalizedNodeResult result = new NormalizedNodeResult();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
            JsonParserStream jsonParser = JsonParserStream.create(streamWriter, getSchemaContext(),
                getSchemaContext())) {
            jsonParser.parse(reader);
        } catch (IOException e) {
            LOG.warn("An error {} occured during parsing Json input stream", e.getMessage(), e);
            return Optional.empty();
        }
        return Optional.ofNullable(result.getResult());
    }

}
