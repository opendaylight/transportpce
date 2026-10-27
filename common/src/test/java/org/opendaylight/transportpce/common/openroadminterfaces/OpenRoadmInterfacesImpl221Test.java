/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
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
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;

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
    void deleteInterfaceReportsADeviceThatCannotBeRead() throws Exception {
        DeviceReadException failure = new DeviceReadException("Timed out waiting for device " + NODE);
        when(deviceTransactionManager.readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
                anyLong(), any()))
            .thenThrow(failure);

        // A device that cannot be read must not look like a device without the interface
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.deleteInterface(NODE, INTERFACE));
        assertSame(failure, thrown.getCause());
        assertTrue(thrown.getMessage().contains(INTERFACE), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("Timed out"), thrown.getMessage());
        verify(deviceTransactionManager, never()).getDeviceTransaction(any());
    }

    @Test
    void deleteInterfaceDoesNothingWhenTheInterfaceDoesNotExist() throws Exception {
        when(deviceTransactionManager.readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
                anyLong(), any()))
            .thenReturn(Optional.empty());

        // Nothing to delete is still not an error, so deleting twice stays harmless
        assertDoesNotThrow(() -> openRoadmInterfaces.deleteInterface(NODE, INTERFACE));
        verify(deviceTransactionManager, never()).getDeviceTransaction(any());
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

    @Test
    void readExistingInterfacesReadsThemAllInOneTransaction() throws Exception {
        DeviceTransaction deviceTx = mock(DeviceTransaction.class);
        when(deviceTransactionManager.awaitDeviceTransaction(eq(NODE), anyLong(), anyLong(), any()))
            .thenReturn(deviceTx);
        doReturn(List.of(Optional.empty(), Optional.of(mock(Interface.class)), Optional.of(mock(Interface.class))))
            .when(deviceTx).readAll(eq(LogicalDatastoreType.CONFIGURATION), anyList(), anyLong(), any());

        assertEquals(List.of("B", "C"), openRoadmInterfaces.readExistingInterfaces(NODE, List.of("A", "B", "C")));
        // Only reads, so nothing to commit
        verify(deviceTx).cancel();
        verify(deviceTx, never()).commit(anyLong(), any());
    }

    @Test
    void readExistingInterfacesReportsADeviceThatCannotBeReached() throws Exception {
        DeviceReadException failure = new DeviceReadException("Timed out waiting for device " + NODE);
        when(deviceTransactionManager.awaitDeviceTransaction(eq(NODE), anyLong(), anyLong(), any()))
            .thenThrow(failure);

        // A device that cannot be read must not look like a device without the interfaces
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.readExistingInterfaces(NODE, List.of("A")));
        assertSame(failure, thrown.getCause());
        assertTrue(thrown.getMessage().contains("Timed out"), thrown.getMessage());
    }

    @Test
    void readExistingInterfacesReleasesTheDeviceWhenAReadFails() throws Exception {
        DeviceTransaction deviceTx = mock(DeviceTransaction.class);
        when(deviceTransactionManager.awaitDeviceTransaction(eq(NODE), anyLong(), anyLong(), any()))
            .thenReturn(deviceTx);
        DeviceReadException failure = new DeviceReadException("Reading A from device " + NODE + " failed");
        when(deviceTx.readAll(eq(LogicalDatastoreType.CONFIGURATION), anyList(), anyLong(), any())).thenThrow(failure);

        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.readExistingInterfaces(NODE, List.of("A")));
        assertSame(failure, thrown.getCause());
        verify(deviceTx).cancel();
    }

    @Test
    void readInterfaceReturnsTheInterface() throws Exception {
        Interface found = mock(Interface.class);
        doReturn(Optional.of(found)).when(deviceTransactionManager)
            .readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(), anyLong(), any());

        assertEquals(Optional.of(found), openRoadmInterfaces.readInterface(NODE, INTERFACE));
    }

    @Test
    void readInterfaceReturnsNothingForAMissingInterface() throws Exception {
        doReturn(Optional.empty()).when(deviceTransactionManager)
            .readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(), anyLong(), any());

        assertEquals(Optional.empty(), openRoadmInterfaces.readInterface(NODE, INTERFACE));
    }

    @Test
    void readInterfaceReportsADeviceThatCannotBeRead() throws Exception {
        DeviceReadException failure = new DeviceReadException("Timed out waiting for device " + NODE);
        when(deviceTransactionManager.readFromDevice(eq(NODE), eq(LogicalDatastoreType.CONFIGURATION), any(),
                anyLong(), any()))
            .thenThrow(failure);

        // A device that cannot be read must not look like a device without the interface
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.readInterface(NODE, INTERFACE));
        assertSame(failure, thrown.getCause());
        assertTrue(thrown.getMessage().contains(INTERFACE), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(NODE), thrown.getMessage());
    }
}
