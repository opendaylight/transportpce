/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.core.compat.LegacyDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;

import com.google.inject.Inject;

import io.fd.honeycomb.binding.init.ProviderTrait;

/**
 * Provider for legacy {@linkDataBroker} used in BGP Module.
 */
public class LegacyBindingDataBrokerProvider extends ProviderTrait<DataBroker> {
    @Inject
    private DOMDataBroker domDataBroker;
    @Inject
    private BindingToNormalizedNodeCodec mappingService;
    @Inject
    private ModuleInfoBackedContext mibCtx;

    @Override
    protected BindingDOMDataBrokerAdapter create() {
        return new BindingDOMDataBrokerAdapter(new LegacyDOMDataBrokerAdapter(domDataBroker), mappingService);
    }

}
