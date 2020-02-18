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

package io.fd.honeycomb.infra.distro.initializer;

import static io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG;
import static io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule.HONEYCOMB_CONTEXT;
import static io.fd.honeycomb.infra.distro.initializer.InitializerPipelineModule.HONEYCOMB_INITIALIZER;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.InitializerRegistry;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;

public final class InitializerRegistryAdapterProvider extends ProviderTrait<InitializerRegistry> {

    @Inject
    @Named(HONEYCOMB_CONTEXT)
    private DataTreeInitializer contextInitializer;
    @Inject
    @Named(HONEYCOMB_CONFIG)
    private DataTreeInitializer configInitializer;
    @Inject
    private ReaderRegistry initRegistry;
    @Inject
    @Named(HONEYCOMB_INITIALIZER)
    private DataBroker noopConfigDataBroker;
    @Inject
    @Named(HONEYCOMB_CONTEXT)
    private MappingContext realtimeMappingContext;

    @Override
    protected InitializerRegistryAdapter create() {
        return new InitializerRegistryAdapter(configInitializer, contextInitializer, initRegistry,
                noopConfigDataBroker, realtimeMappingContext);
    }
}
