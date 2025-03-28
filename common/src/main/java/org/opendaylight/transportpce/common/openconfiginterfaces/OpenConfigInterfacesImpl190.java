/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.OpenconfigInterfacesData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.Interfaces;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.OpenconfigPlatformData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.Components;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.Component;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.ComponentKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenConfigInterfacesImpl190 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigInterfacesImpl190.class);

    private final DeviceTransactionManager deviceTransactionManager;

    public OpenConfigInterfacesImpl190(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public <T> void configureComponents(String nodeId, ComponentBuilder componentBuilder)
            throws OpenConfigInterfacesException {
        {
            Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.orElseThrow();
                } else {
                    throw new OpenConfigInterfacesException(String.format("Device transaction was"
                            + " not found for node %s!", nodeId));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenConfigInterfacesException(String.format("Failed to obtain device transaction for "
                        + "node %s!", nodeId), e);
            }
            DataObjectIdentifier<Component> identifier = DataObjectIdentifier
                    .builderOfInherited(OpenconfigPlatformData.class, Components.class)
                    .child(Component.class, new ComponentKey(componentBuilder.getName())).build();
            deviceTx.merge(LogicalDatastoreType.CONFIGURATION, identifier, componentBuilder.build());
            FluentFuture<? extends @NonNull CommitInfo> txSubmitFuture = deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT,
                    Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            final Thread current = Thread.currentThread();
            Thread timer = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(3000);
                        current.interrupt();
                    } catch (InterruptedException e) {
                        LOG.error("Timeout while configuring the component", e);
                    }
                }
            };
            try {
                txSubmitFuture.get();
                LOG.info("Successfully updated the component {} on node {}", componentBuilder.getName(), nodeId);
                timer.interrupt();
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenConfigInterfacesException(String.format("Failed to configure component %s on node %s!",
                        componentBuilder.getName(), nodeId), e);
            }
        }
    }

    public <T> void configureInterface(String nodeId, InterfaceBuilder interfaceBuilder)
            throws OpenConfigInterfacesException {
        {
            Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.orElseThrow();
                } else {
                    throw new OpenConfigInterfacesException(String.format("Device transaction was"
                            + " not found for node %s!", nodeId));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenConfigInterfacesException(String.format("Failed to obtain device transaction for "
                        + "node %s!", nodeId), e);
            }
            DataObjectIdentifier.WithKey<Interface, InterfaceKey> identifier = DataObjectIdentifier
                    .builderOfInherited(OpenconfigInterfacesData.class, Interfaces.class)
                    .child(Interface.class, new InterfaceKey(interfaceBuilder.getName())).build();
            deviceTx.merge(LogicalDatastoreType.CONFIGURATION, identifier, interfaceBuilder.build());
            FluentFuture<? extends @NonNull CommitInfo> txSubmitFuture =
                    deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            final Thread current = Thread.currentThread();
            Thread timer = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(3000);
                        current.interrupt();
                    } catch (InterruptedException e) {
                        LOG.error("Timeout while configuring the interface",e);
                    }
                }
            };
            try {
                txSubmitFuture.get();
                LOG.info("Successfully updated the interface {} on node {}", interfaceBuilder.getName(), nodeId);
                timer.interrupt();
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenConfigInterfacesException(String.format("Failed to configure interface %s on node %s",
                        interfaceBuilder.getName(), nodeId), e);
            }
        }

    }
}
