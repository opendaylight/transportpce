/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro.schema;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import javassist.ClassPool;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;

public class LegacySerializerProvider extends ProviderTrait<BindingToNormalizedNodeCodec> {

    @Inject
    private ModuleInfoBackedContext mibCtx;

    @Override
    protected BindingToNormalizedNodeCodec create() {
        final DataObjectSerializerGenerator serializerGenerator =
            StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));

        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(serializerGenerator);
        BindingRuntimeContext ctx = BindingRuntimeContext.create(mibCtx, mibCtx.getSchemaContext());
        codecRegistry.onBindingRuntimeContextUpdated(ctx);
        BindingToNormalizedNodeCodec codec = new BindingToNormalizedNodeCodec(mibCtx, codecRegistry);
        codec.onGlobalContextUpdated(mibCtx.getSchemaContext());
        return codec;
    }
}
