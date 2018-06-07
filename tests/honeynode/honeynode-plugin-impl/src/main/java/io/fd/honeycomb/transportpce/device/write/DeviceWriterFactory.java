/*
 * Copyright (c) 2018 Orange and/or its affiliates.
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
package io.fd.honeycomb.transportpce.device.write;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.translate.util.write.BindingBrokerWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DeviceWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<OrgOpenroadmDevice> DEVICE_CONTAINER_ID = InstanceIdentifier
            .create(OrgOpenroadmDevice.class);


    @Inject
    @Named("device-databroker")
    private DataBroker deviceDataBroker;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.wildcardedSubtreeAdd(new BindingBrokerWriter<>(DEVICE_CONTAINER_ID, deviceDataBroker));
    }
}
