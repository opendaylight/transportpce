/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JSONDataObjectConverter extends AbstractDataObjectConverter {

    private static final Logger LOG = LoggerFactory.getLogger(JSONDataObjectConverter.class);

    private JSONDataObjectConverter(EffectiveModelContext schemaContext,
            BindingNormalizedNodeSerializer codecRegistry) {
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
                dataStoreContextUtil.getBindingDOMCodecServices());
    }

    /**
     * extracts codec and schema context (?).
     *
     * @param schemaContext schema context for converter
     * @param codecRegistry codec registry used for converting
     * @return converter
     */
    public static DataObjectConverter createWithSchemaContext(@Nonnull EffectiveModelContext schemaContext,
            @Nonnull BindingNormalizedNodeSerializer codecRegistry) {
        return new JSONDataObjectConverter(schemaContext, codecRegistry);
    }

    /**
     * Transforms the JSON input stream into normalized nodes.
     *
     * @param inputStream of the given JSON
     * @return {@link Optional} instance of {@link NormalizedNode}.
     */
    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull InputStream inputStream) {
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return parseInputJSON(reader);
    }

    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull Reader inputReader, SchemaNode parentSchema) {
        throw new UnsupportedOperationException("Not Implemented yet");
    }

    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull Reader inputReader) {
        JsonReader reader = new JsonReader(inputReader);
        return parseInputJSON(reader);
    }

    @Override
    public <T extends DataObject> Writer writerFromDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType) {
        Writer writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        JSONCodecFactory jsonCodecFactory =
            JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.createLazy(getSchemaContext());
        NormalizedNodeStreamWriter create =
            JSONNormalizedNodeStreamWriter.createExclusiveWriter(jsonCodecFactory,
            (SchemaPath)null, null, jsonWriter);

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
    private Optional<NormalizedNode> parseInputJSON(
            JsonReader reader) {
        NormalizedNodeResult result = new NormalizedNodeResult();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
            JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                JSONCodecFactorySupplier.RFC7951.getShared(getSchemaContext()))) {
            jsonParser.parse(reader);
        } catch (IOException e) {
            LOG.warn("An error occured during parsing Json input stream", e);
            return Optional.empty();
        }
        return Optional.ofNullable(result.getResult());
    }

}
