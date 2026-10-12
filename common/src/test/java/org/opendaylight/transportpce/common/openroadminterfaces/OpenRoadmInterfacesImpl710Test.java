/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;

@ExtendWith(MockitoExtension.class)
class OpenRoadmInterfacesImpl710Test {

    private static final String NODE = "XPDR-A2";
    private static final String FROZEN_NODE = "XPDR-C2";
    private static final String INTERFACE = "XPDR2-NETWORK1-ODUC4";

    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private PortMapping portMapping;

    private OpenRoadmInterfacesImpl710 openRoadmInterfaces;

    @BeforeEach
    void setup() {
        openRoadmInterfaces = new OpenRoadmInterfacesImpl710(deviceTransactionManager, portMapping);
    }

    @Test
    void getSupportedInterfaceReturnsTheSupportingInterface() throws Exception {
        deviceHas(new InterfaceBuilder()
            .setName(INTERFACE)
            .setSupportingInterfaceList(Set.of("XPDR2-NETWORK1-OTUC4"))
            .build());

        assertEquals("XPDR2-NETWORK1-OTUC4", openRoadmInterfaces.getSupportedInterface(NODE, INTERFACE));
    }

    @Test
    void getSupportedInterfaceIsNullForAnInterfaceWithoutSupportingInterface() throws Exception {
        deviceHas(new InterfaceBuilder().setName(INTERFACE).build());

        assertNull(openRoadmInterfaces.getSupportedInterface(NODE, INTERFACE));
    }

    @Test
    void getSupportedInterfaceIsNullForAnEmptySupportingInterfaceList() throws Exception {
        deviceHas(new InterfaceBuilder().setName(INTERFACE).setSupportingInterfaceList(Set.of()).build());

        assertNull(openRoadmInterfaces.getSupportedInterface(NODE, INTERFACE));
    }

    private void deviceHas(Interface intf) throws Exception {
        when(deviceTransactionManager.readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
                anyLong(), any()))
            .thenReturn(Optional.of(intf));
    }

    @Test
    void deleteInterfaceOnANodeThatDoesNotAnswerDoesNotHoldUpOtherNodes() throws Exception {
        CountDownLatch frozenReadStarted = new CountDownLatch(1);
        CountDownLatch releaseFrozenRead = new CountDownLatch(1);
        when(deviceTransactionManager.readFromDevice(eq(FROZEN_NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
                anyLong(), any()))
            .thenAnswer(invocation -> {
                frozenReadStarted.countDown();
                releaseFrozenRead.await();
                return Optional.empty();
            });
        when(deviceTransactionManager.readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
                anyLong(), any()))
            .thenReturn(Optional.empty());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<?> frozenDelete = executor.submit(() -> {
                openRoadmInterfaces.deleteInterface(FROZEN_NODE, INTERFACE);
                return null;
            });
            assertTrue(frozenReadStarted.await(10, TimeUnit.SECONDS));

            // Only deletes on the same node wait for each other
            assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> openRoadmInterfaces.deleteInterface(NODE, INTERFACE));

            releaseFrozenRead.countDown();
            frozenDelete.get(10, TimeUnit.SECONDS);
        } finally {
            releaseFrozenRead.countDown();
            executor.shutdownNow();
        }
    }
}
