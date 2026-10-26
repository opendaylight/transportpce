/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.mdsal.common.api.CommitInfo.emptyFluentFuture;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.crossconnect.ConnectionReader.Connection;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

/**
 * Tests the delete flow, which is the same for every OpenROADM version, with a mock {@link ConnectionReader}.
 */
@ExtendWith(MockitoExtension.class)
class CrossConnectDeleteTest {

    private static final String DEVICE = "deviceId";
    private static final String NAME = "1";
    // Any paths will do: the reader of each version decides where its cross connects are
    private static final DataObjectIdentifier<?> ROADM_PATH = new ConnectionReader221().roadmConnectionPath(NAME);
    private static final DataObjectIdentifier<?> ODU_PATH = new ConnectionReader221().oduConnectionPath(NAME);
    private static final Connection ROADM_CONNECTION =
        new Connection(ROADM_PATH, "SRG1-PP1-TXRX-nmc-761:768", "DEG1-TTP-TXRX-nmc-761:768");
    private static final Connection ODU_CONNECTION =
        new Connection(ODU_PATH, "XPDR1-CLIENT1-ODU2e", "XPDR1-NETWORK1-ODU2e");

    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private ConnectionReader connectionReader;
    @Mock
    private DeviceTransaction deviceTx;

    private CrossConnectDelete crossConnectDelete;

    @BeforeEach
    void setup() {
        crossConnectDelete = new CrossConnectDelete(deviceTransactionManager, connectionReader);
    }

    @Test
    void deleteReportsADeviceThatCannotBeReached() throws Exception {
        DeviceReadException failure = new DeviceReadException("Timed out waiting for device " + DEVICE);
        when(deviceTransactionManager.awaitDeviceTransaction(eq(DEVICE), anyLong(), anyLong(), any()))
            .thenThrow(failure);

        // A frozen device fails the delete instead of holding up the caller
        assertSame(failure, assertThrows(DeviceReadException.class,
            () -> crossConnectDelete.delete(DEVICE, NAME, true, false)));
    }

    @Test
    void deleteReportsADeviceThatCannotBeRead() throws Exception {
        givenTheTransaction();
        DeviceReadException failure = new DeviceReadException("Timed out reading from device " + DEVICE);
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenThrow(failure);

        // A device that cannot be read must not look like a device without the cross connect
        assertSame(failure, assertThrows(DeviceReadException.class,
            () -> crossConnectDelete.delete(DEVICE, NAME, true, false)));
        verify(deviceTx).cancel();
        verify(deviceTx, never()).commit(anyLong(), any());
    }

    @Test
    void deleteReturnsNullWhenTheCrossConnectDoesNotExist() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.empty());

        // Nothing to delete is still not an error, so deleting twice stays harmless
        assertNull(crossConnectDelete.delete(DEVICE, NAME, true, false));
        verify(deviceTx).cancel();
        verify(deviceTx, never()).commit(anyLong(), any());
    }

    @Test
    void deleteReadsAndDeletesInOneTransaction() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.of(ROADM_CONNECTION));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        // The nmc interfaces, and the mc interfaces under them
        assertEquals(
            List.of(
                "SRG1-PP1-TXRX-nmc-761:768",
                "DEG1-TTP-TXRX-nmc-761:768",
                "SRG1-PP1-TXRX-mc-761:768",
                "DEG1-TTP-TXRX-mc-761:768"),
            crossConnectDelete.delete(DEVICE, NAME, true, false));
        verify(deviceTx).delete(LogicalDatastoreType.CONFIGURATION, ROADM_PATH);
        verify(deviceTx).commit(anyLong(), any());
        verify(deviceTx, never()).cancel();
        verify(deviceTransactionManager, never()).getDeviceTransaction(any());
    }

    @Test
    void deleteReleasesTheDeviceWhenTheDeleteFails() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.of(ROADM_CONNECTION));
        // For example, the mount point was torn down after the read
        doThrow(new IllegalStateException("Mount point torn down")).when(deviceTx).delete(any(), any());

        // A failed delete must not look like a cross connect that does not exist
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> crossConnectDelete.delete(DEVICE, NAME, true, false));
        assertTrue(thrown.getMessage().contains("Mount point torn down"), thrown.getMessage());
        verify(deviceTx).cancel();
        verify(deviceTx, never()).commit(anyLong(), any());
    }

    @Test
    void deleteReportsAFailedCommit() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.of(ROADM_CONNECTION));
        doReturn(FluentFutures.immediateFailedFluentFuture(new IllegalStateException("device rejected the delete")))
            .when(deviceTx).commit(anyLong(), any());

        // A failed delete must not look like a cross connect that does not exist
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> crossConnectDelete.delete(DEVICE, NAME, true, false));
        assertTrue(thrown.getMessage().contains("device rejected the delete"), thrown.getMessage());
    }

    @Test
    void deleteReportsACancelledCommit() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.of(ROADM_CONNECTION));
        doReturn(FluentFuture.from(Futures.immediateCancelledFuture())).when(deviceTx).commit(anyLong(), any());

        // A cancelled commit must be reported as a failed delete, not escape as an unchecked exception
        assertThrows(OpenRoadmInterfaceException.class, () -> crossConnectDelete.delete(DEVICE, NAME, true, false));
    }

    @Test
    void deleteReadsOnlyTheOduConnectionWhenNotLookingForARoadmConnection() throws Exception {
        givenTheTransaction();
        when(connectionReader.readOduConnection(deviceTx, NAME)).thenReturn(Optional.of(ODU_CONNECTION));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        assertEquals(List.of("XPDR1-CLIENT1-ODU2e", "XPDR1-NETWORK1-ODU2e"),
            crossConnectDelete.delete(DEVICE, NAME, false, true));
        verify(connectionReader, never()).readRoadmConnection(any(), any());
        verify(deviceTx).delete(LogicalDatastoreType.CONFIGURATION, ODU_PATH);
    }

    @Test
    void deleteSkipsAMissingOduConnectionInterface() throws Exception {
        givenTheTransaction();
        // As a device may report it: without src-if
        when(connectionReader.readOduConnection(deviceTx, NAME))
            .thenReturn(Optional.of(new Connection(ODU_PATH, null, "XPDR1-NETWORK1-ODU2e")));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        assertEquals(List.of("XPDR1-NETWORK1-ODU2e"), crossConnectDelete.delete(DEVICE, NAME, false, true));
        verify(deviceTx).delete(LogicalDatastoreType.CONFIGURATION, ODU_PATH);
    }

    @Test
    void deleteSkipsAMissingRoadmConnectionInterface() throws Exception {
        givenTheTransaction();
        // As a device may report it: without dst-if
        when(connectionReader.readRoadmConnection(deviceTx, NAME))
            .thenReturn(Optional.of(new Connection(ROADM_PATH, "SRG1-PP1-TXRX-nmc-761:768", null)));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        assertEquals(List.of("SRG1-PP1-TXRX-nmc-761:768", "SRG1-PP1-TXRX-mc-761:768"),
            crossConnectDelete.delete(DEVICE, NAME, true, false));
        verify(deviceTx).delete(LogicalDatastoreType.CONFIGURATION, ROADM_PATH);
    }

    @Test
    void deleteRoadmOrOduDeletesTheOduConnectionInTheSameTransaction() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.empty());
        when(connectionReader.readOduConnection(deviceTx, NAME)).thenReturn(Optional.of(ODU_CONNECTION));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        assertEquals(List.of("XPDR1-CLIENT1-ODU2e", "XPDR1-NETWORK1-ODU2e"),
            crossConnectDelete.delete(DEVICE, NAME, true, true));
        // One wait for the device, not one per connection type
        verify(deviceTransactionManager, times(1)).awaitDeviceTransaction(eq(DEVICE), anyLong(), anyLong(), any());
        verify(deviceTx).delete(LogicalDatastoreType.CONFIGURATION, ODU_PATH);
    }

    @Test
    void deleteRoadmOrOduPrefersTheRoadmConnection() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.of(ROADM_CONNECTION));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        assertEquals(
            List.of(
                "SRG1-PP1-TXRX-nmc-761:768",
                "DEG1-TTP-TXRX-nmc-761:768",
                "SRG1-PP1-TXRX-mc-761:768",
                "DEG1-TTP-TXRX-mc-761:768"),
            crossConnectDelete.delete(DEVICE, NAME, true, true));
        verify(connectionReader, never()).readOduConnection(any(), any());
    }

    @Test
    void deleteRoadmOrOduReturnsNullWhenNeitherExists() throws Exception {
        givenTheTransaction();
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenReturn(Optional.empty());
        when(connectionReader.readOduConnection(deviceTx, NAME)).thenReturn(Optional.empty());

        assertNull(crossConnectDelete.delete(DEVICE, NAME, true, true));
        verify(deviceTx).cancel();
        verify(deviceTx, never()).commit(anyLong(), any());
    }

    @Test
    void deleteRoadmOrOduReadsTheOduConnectionWhenTheDeviceRejectsTheRoadmRead() throws Exception {
        givenTheTransaction();
        // As from a transponder that rejects the roadm-connection read
        when(connectionReader.readRoadmConnection(deviceTx, NAME))
            .thenThrow(new DeviceReadException("Reading failed", new ExecutionException(new IllegalStateException())));
        when(connectionReader.readOduConnection(deviceTx, NAME)).thenReturn(Optional.of(ODU_CONNECTION));
        doReturn(emptyFluentFuture()).when(deviceTx).commit(anyLong(), any());

        assertEquals(List.of("XPDR1-CLIENT1-ODU2e", "XPDR1-NETWORK1-ODU2e"),
            crossConnectDelete.delete(DEVICE, NAME, true, true));
        verify(deviceTx).delete(LogicalDatastoreType.CONFIGURATION, ODU_PATH);
    }

    @Test
    void deleteRoadmOrOduReportsTheRoadmReadFailureWhenThereIsNoOduConnection() throws Exception {
        givenTheTransaction();
        DeviceReadException failure =
            new DeviceReadException("Reading failed", new ExecutionException(new IllegalStateException()));
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenThrow(failure);
        when(connectionReader.readOduConnection(deviceTx, NAME)).thenReturn(Optional.empty());

        // The roadm-connection that could not be read may exist, so this is not "nothing to delete"
        assertSame(failure, assertThrows(DeviceReadException.class,
            () -> crossConnectDelete.delete(DEVICE, NAME, true, true)));
        verify(deviceTx).cancel();
        verify(deviceTx, never()).commit(anyLong(), any());
    }

    @Test
    void deleteRoadmOrOduDoesNotReadAgainAfterATimeout() throws Exception {
        givenTheTransaction();
        DeviceReadException failure = new DeviceReadException("Timed out", new TimeoutException());
        when(connectionReader.readRoadmConnection(deviceTx, NAME)).thenThrow(failure);

        // A device that did not answer one read is unlikely to answer the next, so do not wait for it twice
        assertSame(failure, assertThrows(DeviceReadException.class,
            () -> crossConnectDelete.delete(DEVICE, NAME, true, true)));
        verify(connectionReader, never()).readOduConnection(any(), any());
        verify(deviceTx).cancel();
    }

    private void givenTheTransaction() throws DeviceReadException {
        when(deviceTransactionManager.awaitDeviceTransaction(eq(DEVICE), anyLong(), anyLong(), any()))
            .thenReturn(deviceTx);
    }
}
