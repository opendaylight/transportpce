/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

class DeviceTransactionTest {

    private static final LogicalDatastoreType STORE = LogicalDatastoreType.CONFIGURATION;

    private final ReadWriteTransaction rwTx = mock(ReadWriteTransaction.class);

    private final DeviceTransaction deviceTx = new DeviceTransaction("node1", rwTx, SettableFuture.create());

    private final DataObjectIdentifier<Interface> first = interfaceIID("first");

    private final DataObjectIdentifier<Interface> second = interfaceIID("second");

    @AfterEach
    void cleanup() {
        deviceTx.cancel();
    }

    @Test
    void testReadsReturnTheDataInTheOrderOfThePaths() throws Exception {
        Interface intf = mock(Interface.class);
        doReturn(FluentFutures.immediateFluentFuture(Optional.empty())).when(rwTx).read(STORE, first);
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(intf))).when(rwTx).read(STORE, second);

        assertEquals(List.of(Optional.empty(), Optional.of(intf)),
            deviceTx.readAll(STORE, List.of(first, second), 5, TimeUnit.SECONDS));
    }

    @Test
    void testReadsShareOneDeadline() {
        SettableFuture<Optional<Interface>> slow = SettableFuture.create();
        doReturn(FluentFuture.from(slow)).when(rwTx).read(STORE, first);
        doReturn(FluentFuture.from(SettableFuture.create())).when(rwTx).read(STORE, second);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> slow.set(Optional.empty()), 500, TimeUnit.MILLISECONDS);

        Duration timeout = Duration.ofMillis(1000);
        long start = System.nanoTime();
        DeviceReadException thrown;
        try {
            thrown = assertThrows(DeviceReadException.class,
                () -> deviceTx.readAll(STORE, List.of(first, second), timeout.toMillis(), TimeUnit.MILLISECONDS));
        } finally {
            scheduler.shutdownNow();
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        // With a timeout for each read, the second read would only give up 1500 ms after the start
        assertTrue(elapsed.compareTo(Duration.ofMillis(1400)) < 0, "readAll() took " + elapsed);
        assertTrue(thrown.getMessage().startsWith("Timed out"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("second"), thrown.getMessage());
    }

    @Test
    void testAFailedReadNamesItsPathAndCancelsTheOthers() {
        SettableFuture<Optional<Interface>> pending = SettableFuture.create();
        doReturn(FluentFutures.immediateFailedFluentFuture(new IllegalStateException("rejected")))
            .when(rwTx).read(STORE, first);
        doReturn(FluentFuture.from(pending)).when(rwTx).read(STORE, second);

        DeviceReadException thrown = assertThrows(DeviceReadException.class,
            () -> deviceTx.readAll(STORE, List.of(first, second), 5, TimeUnit.SECONDS));

        assertTrue(thrown.getMessage().contains("first"), thrown.getMessage());
        // Every read was started before waiting for any of them, and the ones left are not waited for
        assertTrue(pending.isCancelled());
    }

    @Test
    void testAReadThatCannotBeStartedIsReported() {
        SettableFuture<Optional<Interface>> started = SettableFuture.create();
        doReturn(FluentFuture.from(started)).when(rwTx).read(STORE, first);
        // For example, the mount point was torn down
        doThrow(new IllegalStateException("Mount point torn down")).when(rwTx).read(STORE, second);

        DeviceReadException thrown = assertThrows(DeviceReadException.class,
            () -> deviceTx.readAll(STORE, List.of(first, second), 5, TimeUnit.SECONDS));

        assertTrue(thrown.getMessage().contains("second"), thrown.getMessage());
        assertTrue(started.isCancelled());
    }

    private static DataObjectIdentifier<Interface> interfaceIID(String name) {
        return DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Interface.class, new InterfaceKey(name))
            .build();
    }
}
