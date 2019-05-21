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

package io.fd.honeycomb.northbound.netconf;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.northbound.NetconfConfiguration;
import io.netty.channel.nio.NioEventLoopGroup;

public final class NettyThreadGroupProvider extends ProviderTrait<NioEventLoopGroup> {

    @Inject
    private NetconfConfiguration cfgAttributes;

    @Override
    protected NioEventLoopGroup create() {
        return new NioEventLoopGroup(cfgAttributes.netconfNettyThreads,
                new ThreadFactoryBuilder().setNameFormat("netconf-netty-%d").build());
    }
}
