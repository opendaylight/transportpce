/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util.test;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public final class JsonUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);
    private static JsonUtil instance;

    private SchemaContext schemaCtx;

    private BindingNormalizedNodeCodecRegistry codecRegistry;

    private JsonUtil() {
        List<YangModuleInfo> moduleInfos = new LinkedList<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
        }
        /* Create the schema context for loaded models */
        ModuleInfoBackedContext moduleInfoBackedCntxt = ModuleInfoBackedContext.create();
        moduleInfoBackedCntxt.addModuleInfos(moduleInfos);
        schemaCtx = moduleInfoBackedCntxt.getSchemaContext();
        if (schemaCtx == null) {
            throw new IllegalStateException("Failed to load schema context");
        }
        // Create the binding binding normalized node codec registry
        BindingRuntimeContext bindingRuntimeContext = BindingRuntimeContext.create(moduleInfoBackedCntxt, schemaCtx);
        codecRegistry = new BindingNormalizedNodeCodecRegistry(bindingRuntimeContext);
    }

    public static JsonUtil getInstance() {
        if (instance == null) {
            instance = new JsonUtil();
        }
        return instance;
    }

    public DataObject getDataObjectFromJson(JsonReader reader, QName pathQname) {
        NormalizedNodeResult result = new NormalizedNodeResult();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
                JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                        JSONCodecFactorySupplier.RFC7951.getShared(schemaCtx), schemaCtx);) {
            jsonParser.parse(reader);
            YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
            if (codecRegistry.fromNormalizedNode(yangId, result.getResult()) != null) {
                return codecRegistry.fromNormalizedNode(yangId, result.getResult()).getValue();
            } else {
                return null;
            }
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Cannot deserialize JSON ", e);
            return null;
        }

    }
}
