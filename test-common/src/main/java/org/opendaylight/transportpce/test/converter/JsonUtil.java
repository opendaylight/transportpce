/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import org.opendaylight.mdsal.binding.dom.adapter.CurrentAdapterSerializer;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultBindingDOMCodecFactory;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);
    private static JsonUtil instance;

    private EffectiveModelContext schemaCtx;
    private BindingNormalizedNodeSerializer codecRegistry;

    private JsonUtil() {
        List<YangModuleInfo> moduleInfos = new LinkedList<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
        }
        /* Create the schema context for loaded models */
        schemaCtx = BindingRuntimeHelpers.createEffectiveModel(moduleInfos);
        if (schemaCtx == null) {
            throw new IllegalStateException("Failed to load schema context");
        }
        // Create the binding binding normalized node codec registry
        codecRegistry = new CurrentAdapterSerializer(
            new DefaultBindingDOMCodecFactory()
                .createBindingDOMCodec(BindingRuntimeHelpers.createRuntimeContext()));
    }

    public static synchronized JsonUtil getInstance() {
        if (instance == null) {
            instance = new JsonUtil();
        }
        return instance;
    }

    public DataObject getDataObjectFromJson(JsonReader reader, QName pathQname) {
        NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
                JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                        JSONCodecFactorySupplier.RFC7951.getShared(schemaCtx));) {
            jsonParser.parse(reader);
            YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
            Entry<DataObjectReference<?>, DataObject> entry =
                codecRegistry.fromNormalizedNode(yangId, resultHolder.getResult().data());
            return entry == null ? null : entry.getValue();
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Cannot deserialize JSON ", e);
            return null;
        }
    }
}
