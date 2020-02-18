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
import io.fd.honeycomb.rpc.HoneycombDOMRpcService;
import io.fd.honeycomb.rpc.RpcRegistry;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.dom.api.DOMRpcService;

public final class HoneycombDOMRpcServiceProvider extends ProviderTrait<DOMRpcService> {

    @Inject
    private BindingToNormalizedNodeCodec serializer;

    @Inject
    private RpcRegistry rpcRegistry;

    @Override
    protected DOMRpcService create() {
        return new HoneycombDOMRpcService(serializer, rpcRegistry);
    }
}
