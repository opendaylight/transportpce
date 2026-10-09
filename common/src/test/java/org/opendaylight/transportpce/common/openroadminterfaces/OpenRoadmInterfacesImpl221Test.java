/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class OpenRoadmInterfacesImpl221Test {

    private static final String NODE = "ROADM-A1";
    private static final String FROZEN_NODE = "ROADM-C1";
    private static final String INTERFACE = "DEG1-TTP-TXRX-nmc-761:768";

    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private PortMapping portMapping;

    private OpenRoadmInterfacesImpl221 openRoadmInterfaces;

    @BeforeEach
    void setup() {
        openRoadmInterfaces = new OpenRoadmInterfacesImpl221(deviceTransactionManager, portMapping);
    }

    @Test
    void deleteInterfaceOnANodeThatDoesNotAnswerDoesNotHoldUpOtherNodes() throws Exception {
        CountDownLatch frozenReadStarted = new CountDownLatch(1);
        CountDownLatch releaseFrozenRead = new CountDownLatch(1);
        when(deviceTransactionManager.getDataFromDevice(eq(FROZEN_NODE), eq(LogicalDatastoreType.CONFIGURATION),
                any(), anyLong(), any()))
            .thenAnswer(invocation -> {
                frozenReadStarted.countDown();
                releaseFrozenRead.await();
                return Optional.empty();
            });
        when(deviceTransactionManager.getDataFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
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
