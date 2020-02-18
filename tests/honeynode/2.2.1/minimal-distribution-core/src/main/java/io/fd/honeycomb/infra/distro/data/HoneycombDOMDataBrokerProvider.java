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
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.data.ReadableDataManager;
import io.fd.honeycomb.data.impl.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;

public final class HoneycombDOMDataBrokerProvider extends ProviderTrait<DOMDataBroker> {

    @Inject
    private ModifiableDataManager modDataManager;
    @Inject(optional = true)
    private ReadableDataManager readDataManager;

    @Override
    protected DataBroker create() {
        return readDataManager != null
                ? DataBroker.create(modDataManager, readDataManager)
                : DataBroker.create(modDataManager);
    }
}
