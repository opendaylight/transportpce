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

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public final class SchemaServiceProvider extends ProviderTrait<SchemaService> {

    @Inject
    private ModuleInfoBackedContext mibCtx;

    @Override
    public StaticSchemaService create() {
        return new StaticSchemaService(mibCtx.getSchemaContext());
    }

    /**
     * Static schema context provider service.
     */
    private static final class StaticSchemaService implements SchemaService {
        private final SchemaContext schemaContext;

        StaticSchemaService(SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
        }

        @Override
        public void addModule(final Module module) {
            throw new UnsupportedOperationException("Static service");
        }

        @Override
        public void removeModule(final Module module) {
            throw new UnsupportedOperationException("Static service");
        }

        @Override
        public SchemaContext getSessionContext() {
            return schemaContext;
        }

        @Override
        public SchemaContext getGlobalContext() {
            return schemaContext;
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                final SchemaContextListener listener) {
            listener.onGlobalContextUpdated(schemaContext);
            return new ListenerRegistration<SchemaContextListener>() {
                @Override
                public void close() {}

                @Override
                public SchemaContextListener getInstance() {
                    return listener;
                }

            };
        }
    }
}
