/*
 * Copyright (c) 2016, 2017 Cisco and/or its affiliates.
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
import io.fd.honeycomb.impl.NorthboundFacadeHoneycombDOMBroker;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

final class HoneycombDOMBrokerProvider extends ProviderTrait<Broker> {

    @Inject
    @Named(HONEYCOMB_CONFIG)
    private DOMDataBroker domDataBroker;
    @Inject
    private SchemaService schemaService;
    @Inject
    private DOMNotificationRouter domNotificationService;
    @Inject
    private DOMRpcService domRpcService;
    @Inject
    private DOMMountPointService domMountPointService;

    @Override
    protected NorthboundFacadeHoneycombDOMBroker create() {
        return new NorthboundFacadeHoneycombDOMBroker(domDataBroker, schemaService, domNotificationService,
            domRpcService, domMountPointService);
    }
}
