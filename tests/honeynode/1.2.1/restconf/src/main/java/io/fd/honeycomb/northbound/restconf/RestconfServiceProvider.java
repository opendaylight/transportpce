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
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.netconf.sal.restconf.impl.BrokerFacade;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.netconf.sal.restconf.impl.RestconfImpl;

public class RestconfServiceProvider extends ProviderTrait<RestconfImpl> {

    @Inject
    private ControllerContext controllerContext;
    @Inject
    private BrokerFacade brokerFacade;

    @Override
    protected RestconfImpl create() {
        return RestconfImpl.newInstance(brokerFacade, controllerContext);
    }
}
