/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.oc200.mapping;

import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesVersionService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.OpenconfigInterfacesData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.interfaces.top.Interfaces;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.interfaces.top.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.interfaces.top.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev221025.interfaces.top.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.OpenconfigPlatformData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.top.Components;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.top.components.Component;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.top.components.ComponentKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

@org.osgi.service.component.annotations.Component(
        service = {OpenConfigInterfaces.class, OpenConfigInterfacesVersionService.class},
        property = "openconfig.version=2.0.0")
public final class OpenConfigInterfacesOC200 implements OpenConfigInterfacesVersionService {
    private final DeviceTransactionManager deviceTransactionManager;

    @Activate
    public OpenConfigInterfacesOC200(@Reference DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    @Override
    public String getVersion() {
        return "openconfig-200";
    }

    @Override
    public <T> void configureComponent(String nodeId, T builder) throws OpenConfigInterfacesException {
        if (!(builder instanceof ComponentBuilder componentBuilder)) {
            throw new OpenConfigInterfacesException("Expected an OpenConfig 2.0 component builder");
        }
        var identifier = DataObjectIdentifier.builderOfInherited(OpenconfigPlatformData.class, Components.class)
                .child(Component.class, new ComponentKey(componentBuilder.getName())).build();
        write(nodeId, identifier, componentBuilder.build());
    }

    @Override
    public <T> void configureInterface(String nodeId, T builder) throws OpenConfigInterfacesException {
        if (!(builder instanceof InterfaceBuilder interfaceBuilder)) {
            throw new OpenConfigInterfacesException("Expected an OpenConfig 2.0 interface builder");
        }
        var identifier = DataObjectIdentifier.builderOfInherited(OpenconfigInterfacesData.class, Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceBuilder.getName())).build();
        write(nodeId, identifier, interfaceBuilder.build());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private <T extends DataObject> void write(String nodeId, DataObjectIdentifier<T> identifier, T data)
            throws OpenConfigInterfacesException {
        DeviceTransaction transaction;
        try {
            transaction = deviceTransactionManager.getDeviceTransaction(nodeId).get()
                    .orElseThrow(() -> new OpenConfigInterfacesException(
                            "Device transaction was not found for node %s".formatted(nodeId)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenConfigInterfacesException(
                    "Interrupted obtaining transaction for node %s".formatted(nodeId), e);
        } catch (ExecutionException e) {
            throw new OpenConfigInterfacesException("Failed to obtain transaction for node %s".formatted(nodeId), e);
        }
        try {
            transaction.merge(LogicalDatastoreType.CONFIGURATION, identifier, data);
        } catch (RuntimeException e) {
            transaction.cancel();
            throw new OpenConfigInterfacesException("Failed to prepare configuration for node %s".formatted(nodeId), e);
        }
        try {
            transaction.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenConfigInterfacesException("Interrupted configuring node %s".formatted(nodeId), e);
        } catch (ExecutionException e) {
            throw new OpenConfigInterfacesException("Failed to configure node %s".formatted(nodeId), e);
        }
    }
}
