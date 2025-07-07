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
import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.api.AbstractBindingRuntimeContext;
import org.opendaylight.yangtools.binding.runtime.api.BindingRuntimeGenerator;
import org.opendaylight.yangtools.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.yangtools.binding.runtime.spi.ModuleInfoSnapshotResolver;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
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
    public static DataObjectConverter createWithDataStoreUtil(@NonNull DataStoreContext dataStoreContextUtil) {
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
    public static DataObjectConverter createWithSchemaContext(@NonNull EffectiveModelContext schemaContext,
            @NonNull BindingNormalizedNodeSerializer codecRegistry) {
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
            @   NonNull InputStream inputStream) {
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return parseInputJSON(reader, null);
    }

    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @NonNull Reader inputReader, SchemaNode parentSchema) {
        throw new UnsupportedOperationException("Not Implemented yet");
    }

    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @NonNull Reader inputReader) {
        JsonReader reader = new JsonReader(inputReader);
        return parseInputJSON(reader, null);
    }

    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(@NonNull Reader inputReader,
            @NonNull Set<YangModuleInfo> models) {
        JsonReader reader = new JsonReader(inputReader);
        return parseInputJSON(reader, models);
    }

    @Override
    public <T extends DataObject> Writer writerFromDataObject(@NonNull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType) {
        Writer writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        JSONCodecFactory jsonCodecFactory =
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.createLazy(getSchemaContext());
        EffectiveStatementInference rootNode = SchemaInferenceStack.of(getSchemaContext()).toInference();
        NormalizedNodeStreamWriter create = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                jsonCodecFactory, rootNode, EffectiveModelContext.NAME.getNamespace(), jsonWriter);
        try (NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(create);) {
            normalizedNodeWriter
                    .write(convertType.toNormalizedNodes(dataObjectClass.cast(object), dataObjectClass).orElseThrow());
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        return writer;
    }

    @Override
    public <T extends DataObject> Writer writerFromRpcDataObject(@NonNull DataObject object, Class<T> dataObjectClass,
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
            JsonReader reader, Set<YangModuleInfo> models) {

        NormalizationResultHolder result = new NormalizationResultHolder();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
             JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                     JSONCodecFactorySupplier.RFC7951.createLazy(
                             createBindingRuntimeContext(models).modelContext()))) {
            jsonParser.parse(reader);
        } catch (IOException e) {
            LOG.warn("An error occured during parsing Json input stream", e);
            return Optional.empty();
        }
        return Optional.ofNullable(result.getResult().data());
    }

    private AbstractBindingRuntimeContext createBindingRuntimeContext(Set<YangModuleInfo> models) {
        final YangXPathParserFactory xpathFactory = new AntlrXPathParserFactory();
        DefaultYangParserFactory yangParserFactory = new DefaultYangParserFactory(xpathFactory);
        var snapshotResolver = new ModuleInfoSnapshotResolver("binding-dom-codec", yangParserFactory);
        snapshotResolver.registerModuleInfos(loadModuleInfos(models));
        var moduleInfoSnapshot = snapshotResolver.takeSnapshot();
        final BindingRuntimeGenerator bindingRuntimeGenerator = new DefaultBindingRuntimeGenerator();
        final var bindingRuntimeTypes = bindingRuntimeGenerator
                .generateTypeMapping(moduleInfoSnapshot.modelContext());
        return new DefaultBindingRuntimeContext(bindingRuntimeTypes, moduleInfoSnapshot);
    }

    /**
     * Helper method for loading {@link YangModuleInfo}s from the classpath.
     *
     * @return {@link List} of loaded {@link YangModuleInfo}
     */
    private Set<YangModuleInfo> loadModuleInfos(Set<YangModuleInfo> models) {
        if (models == null) {
            Set<YangModuleInfo> moduleInfos = new HashSet<>();
            ServiceLoader<YangModelBindingProvider> yangproviderLoader =
                    ServiceLoader.load(YangModelBindingProvider.class);
            for (YangModelBindingProvider yangModelBindingProvider : yangproviderLoader) {
                moduleInfos.add(yangModelBindingProvider.getModuleInfo());
            }
            LOG.info("moduleInfos =  {}", moduleInfos);
            return moduleInfos;
        } else {
            return models;
        }
    }
}
