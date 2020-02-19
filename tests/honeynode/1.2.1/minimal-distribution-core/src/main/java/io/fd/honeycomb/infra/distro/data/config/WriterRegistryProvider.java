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

package io.fd.honeycomb.infra.distro.data.config;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.HashSet;
import java.util.Set;

public final class WriterRegistryProvider extends ProviderTrait<WriterRegistry> {

    @Inject(optional = true)
    private Set<WriterFactory> writerFactories = new HashSet<>();

    @Override
    protected WriterRegistry create() {
        final FlatWriterRegistryBuilder builder = new FlatWriterRegistryBuilder(new YangDAG());
        writerFactories
                .stream()
                .forEach(it -> it.init(builder));
        return builder.build();
    }
}
