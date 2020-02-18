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
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;

final class ControllerContextProvider extends ProviderTrait<ControllerContext> {

    @Inject
    private DOMSchemaService schemaService;
    @Inject
    private DOMMountPointService mountPointService;

    @Override
    protected ControllerContext create() {
        ControllerContext controllerCtx =
                ControllerContext.newInstance(schemaService, mountPointService, schemaService);
        return controllerCtx;
    }
}
