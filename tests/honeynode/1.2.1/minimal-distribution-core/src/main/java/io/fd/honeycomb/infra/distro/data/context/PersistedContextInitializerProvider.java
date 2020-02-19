/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro.data.context;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.RestoringInitializer;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import java.nio.file.Paths;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

final class PersistedContextInitializerProvider extends ProviderTrait<RestoringInitializer> {

    @Inject
    private DOMSchemaService schemaService;
    @Inject
    protected HoneycombConfiguration cfgAttributes;
    // injects data broker from within context of private module
    @Inject
    private DOMDataBroker domDataBroker;

    @Override
    public RestoringInitializer create() {
        return new RestoringInitializer(schemaService, Paths.get(cfgAttributes.peristContextPath), domDataBroker,
                RestoringInitializer.RestorationType.valueOf(cfgAttributes.persistedContextRestorationType),
                LogicalDatastoreType.OPERATIONAL);
    }
}
