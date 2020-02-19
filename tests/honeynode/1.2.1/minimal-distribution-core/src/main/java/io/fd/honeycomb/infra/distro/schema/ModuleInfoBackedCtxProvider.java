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

import static io.fd.honeycomb.infra.distro.schema.YangModulesProvider.YangModules;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModuleInfoBackedCtxProvider extends ProviderTrait<ModuleInfoBackedContext> {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleInfoBackedCtxProvider.class);

    // optional in sense that list of modules inside can be empty if none was found
    @Inject
    private YangModules moduleInfos;

    @Override
    protected ModuleInfoBackedContext create() {
        ModuleInfoBackedContext create = ModuleInfoBackedContext.create();
        create.addModuleInfos(moduleInfos.getYangBindings().stream()
                .map(YangModelBindingProvider::getModuleInfo)
                .collect(Collectors.toList()));
        LOG.debug("ModuleInfoBackedContext created from {}", moduleInfos);
        return create;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("writerFactories", moduleInfos).toString();
    }
}
