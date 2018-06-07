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

package io.fd.honeycomb.northbound.restconf;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.netconf.sal.rest.api.RestConnector;
import org.opendaylight.netconf.sal.restconf.impl.RestconfProviderImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;

final class RestconfProvider extends ProviderTrait<RestConnector> {

    @Inject
    private RestconfConfiguration cfg;
    @Inject
//    @Named(HONEYCOMB_CONFIG)
    //Modified in order to connect TPCE to device config datastore
    @Named("device-databroker")
    private DOMDataBroker domDataBroker;
    @Inject
    private SchemaService schemaService;
    @Inject
    private DOMRpcService rpcService;
    @Inject
    private DOMNotificationRouter notificationService;
    @Inject
    private ShutdownHandler shutdownHandler;
    @Inject
    private DOMMountPointService mountPointService;

    @Override
    protected RestconfProviderImpl create() {
        final RestconfProviderImpl instance = new RestconfProviderImpl(domDataBroker, schemaService, rpcService,
            notificationService, mountPointService,
            IpAddressBuilder.getDefaultInstance(cfg.restconfWebsocketAddress.get()),
            new PortNumber(cfg.restconfWebsocketPort.get()));

        // Required to properly initialize restconf (broker, schema ctx, etc.).
        // Without that restconf would fail with 503 (service not available).
        instance.start();

        shutdownHandler.register(instance.getClass().getCanonicalName(), instance);
        return instance;
    }
}
