/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.netconf.sal.restconf.impl.BrokerFacade;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;



final class BrokerFacadeProvider extends ProviderTrait<BrokerFacade> {

    @Inject
    // @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG)
    // Modified in order to connect TPCE to device config datastore
    @Named("device-databroker")
    private DOMDataBroker domDataBroker;
    @Inject
    private DOMRpcService rpcService;
    @Inject
    private DOMNotificationRouter notificationService;
    @Inject
    private ControllerContext controllerContext;

    @Override
    protected BrokerFacade create() {
        return BrokerFacade.newInstance(rpcService, domDataBroker, notificationService, controllerContext);
    }
}
