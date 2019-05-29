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

import static io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;

/**
 * Provides binding adapter for {@link io.fd.honeycomb.data.impl.DataBroker}.
 */
final class HoneycombBindingDataBrokerProvider extends ProviderTrait<DataBroker> {

    @Inject
    @Named(HONEYCOMB_CONFIG)
    private DOMDataBroker domDataBroker;
    @Inject
    private BindingToNormalizedNodeCodec mappingService;

    @Override
    protected BindingDOMDataBrokerAdapter create() {

        return new BindingDOMDataBrokerAdapter(domDataBroker, mappingService);
    }
}
