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

import javax.annotation.Nonnull;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev170708.terminal.device.top.TerminalDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.translate.util.write.BindingBrokerWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;

public class OcTerminalDeviceWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<TerminalDevice> TERMINAL_DEVICE_CONTAINER_ID = InstanceIdentifier
            .create(TerminalDevice.class);


    @Inject
    @Named("device-databroker")
    private DataBroker deviceDataBroker;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.wildcardedSubtreeAdd(new BindingBrokerWriter<>(TERMINAL_DEVICE_CONTAINER_ID, deviceDataBroker));
        deviceDataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                TERMINAL_DEVICE_CONTAINER_ID), new TerminalDeviceChangeListener(deviceDataBroker));
    }
}
