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

package io.fd.honeycomb.infra.distro.data;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.data.impl.ModifiableDataTreeDelegator;
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

public final class ModifiableDTDelegProvider extends ProviderTrait<ModifiableDataManager> {

    @Inject
    private BindingToNormalizedNodeCodec serializer;
    @Inject
    private DOMSchemaService schemaService;
    @Inject
    @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG)
    private DataTree dataTree;
    @Inject
    private WriterRegistry registry;
    @Inject
    @Named(ContextPipelineModule.HONEYCOMB_CONTEXT)
    private DataBroker contextBroker;

    @Override
    protected ModifiableDataTreeDelegator create() {
        return new ModifiableDataTreeDelegator(serializer, dataTree, schemaService.getGlobalContext(),
            registry, contextBroker);
    }
}
