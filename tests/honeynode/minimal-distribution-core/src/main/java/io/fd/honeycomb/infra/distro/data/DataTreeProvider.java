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
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

public abstract class DataTreeProvider extends ProviderTrait<DataTree> {

    @Inject
    private SchemaService schemaService;
    @Inject
    private HoneycombConfiguration config;

    @Override
    public TipProducingDataTree create() {
        TipProducingDataTree delegate = InMemoryDataTreeFactory.getInstance().create(getType());
        delegate.setSchemaContext(schemaService.getGlobalContext());
        return delegate;
    }

    public abstract TreeType getType();

    public static class ConfigDataTreeProvider extends DataTreeProvider {
        @Override
        public TreeType getType() {
            return TreeType.CONFIGURATION;
        }

    }

    public static class ContextDataTreeProvider extends DataTreeProvider {
        @Override
        public TreeType getType() {
            return TreeType.OPERATIONAL;
        }

    }
}
