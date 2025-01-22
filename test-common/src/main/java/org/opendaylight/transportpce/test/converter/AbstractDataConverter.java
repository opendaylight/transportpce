/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingCodecContext;
import org.opendaylight.yangtools.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.api.AbstractBindingRuntimeContext;
import org.opendaylight.yangtools.binding.runtime.api.BindingRuntimeGenerator;
import org.opendaylight.yangtools.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.yangtools.binding.runtime.spi.ModuleInfoSnapshotResolver;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class which may be helpful while manipulating JSON and XML converters.
 * @param <T> Type of the DataObject to serialize / deserialize
 */
public abstract class AbstractDataConverter<T extends DataObject> implements DataConverter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataConverter.class);
    private final AbstractBindingRuntimeContext runtimeContext;
    private final BindingCodecContext bindingCodecContext;


    protected AbstractDataConverter(Set<YangModuleInfo> models) {
        this.runtimeContext = createBindingRuntimeContext(models);
        this.bindingCodecContext = new BindingCodecContext(runtimeContext);
    }

    protected final BindingCodecContext getBindingCodecContext() {
        return this.bindingCodecContext;
    }

    protected final AbstractBindingRuntimeContext getBindingRuntimeContext() {
        return this.runtimeContext;
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

    /**
     * Helper method to create a {@link BindingCodecContext}.
     * @return {@link BindingCodecContext} of loaded {@link YangModuleInfo}
     */
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

}
