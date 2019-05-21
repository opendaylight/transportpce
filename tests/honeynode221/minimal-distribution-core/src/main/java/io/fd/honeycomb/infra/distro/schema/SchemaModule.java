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

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;

public class SchemaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ModuleInfoBackedContext.class).toProvider(ModuleInfoBackedCtxProvider.class).in(Singleton.class);
        bind(SchemaService.class).toProvider(SchemaServiceProvider.class).in(Singleton.class);
        bind(BindingToNormalizedNodeCodec.class).toProvider(SerializerProvider.class).in(Singleton.class);
    }

}
