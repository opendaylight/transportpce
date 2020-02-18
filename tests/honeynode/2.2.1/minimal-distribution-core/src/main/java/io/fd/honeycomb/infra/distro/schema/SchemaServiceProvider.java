/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro.schema;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

public final class SchemaServiceProvider extends ProviderTrait<DOMSchemaService> {

    @Inject
    private ModuleInfoBackedContext mibCtx;

    @Override
    public StaticSchemaService create() {
        return new StaticSchemaService(mibCtx);
    }

    /**
     * Static schema context provider service.
     */
    private static final class StaticSchemaService implements DOMSchemaService, DOMYangTextSourceProvider {
        private final ModuleInfoBackedContext moduleInfoBackedContext;

        StaticSchemaService(ModuleInfoBackedContext moduleInfoBackedContext) {
            this.moduleInfoBackedContext = moduleInfoBackedContext;
        }

        @Override
        public SchemaContext getSessionContext() {
            return moduleInfoBackedContext.getSchemaContext();
        }

        @Override
        public SchemaContext getGlobalContext() {
            return moduleInfoBackedContext.getSchemaContext();
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                final SchemaContextListener listener) {
            listener.onGlobalContextUpdated(moduleInfoBackedContext.getSchemaContext());
            return new ListenerRegistration<SchemaContextListener>() {
                @Override
                public void close() {}

                @Override
                public SchemaContextListener getInstance() {
                    return listener;
                }

            };
        }

        @Override
        public ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
            return ImmutableClassToInstanceMap.of(DOMYangTextSourceProvider.class, this);
        }

        @Override
        public ListenableFuture<? extends YangTextSchemaSource> getSource(final SourceIdentifier sourceIdentifier) {
            return moduleInfoBackedContext.getSource(sourceIdentifier);
        }
    }
}
