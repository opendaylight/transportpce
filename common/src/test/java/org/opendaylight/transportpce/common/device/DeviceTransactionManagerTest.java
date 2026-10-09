/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

@ExtendWith(MockitoExtension.class)
public class DeviceTransactionManagerTest {

    @Mock
    private MountPointService mountPointServiceMock;
    @Mock
    private MountPoint mountPointMock;
    @Mock
    private DataBroker dataBrokerMock;
    @Mock
    private ReadWriteTransaction rwTransactionMock;


    private DeviceTransactionManagerImpl transactionManager;
    private String defaultDeviceId = "device-id";
    private LogicalDatastoreType defaultDatastore = LogicalDatastoreType.OPERATIONAL;
    private DataObjectIdentifier<Network> defaultIid = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId("default-network")))
            .build();
    private Network defaultData;
    private long defaultTimeout = 1000;
    private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;

    @BeforeEach
    void before() {
        when(mountPointServiceMock.findMountPoint(any())).thenReturn(Optional.of(mountPointMock));
        when(mountPointMock.getService(any())).thenReturn(Optional.of(dataBrokerMock));
        when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock);
        lenient().when(rwTransactionMock.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
        NetworkId networkId =  new NetworkId("NETWORK1");
        defaultData = new NetworkBuilder().setNetworkId(networkId).build();
        this.transactionManager = new DeviceTransactionManagerImpl(mountPointServiceMock, 3000);
    }

    @AfterEach
    void after() {
        transactionManager.preDestroy();
    }

    @Test
    void basicPositiveTransactionTest() {
        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        verify(rwTransactionMock, times(1)).put(defaultDatastore, defaultIid, defaultData);
        verify(rwTransactionMock, times(1)).commit();
    }

    @Test
    void transactionsWaitingOnOneDeviceDoNotBlockOtherDevicesTest() throws Exception {
        // A dedicated manager with an explicit pool size, so the test does not depend on the defaults
        int listeningThreads = 4;
        long submitTimeoutMillis = 10_000;
        DeviceTransactionManagerImpl manager = new DeviceTransactionManagerImpl(mountPointServiceMock,
                submitTimeoutMillis, 3000, 4, listeningThreads);
        try {
            // The first transaction on the slow device is never committed, as if its device operation hangs
            final DeviceTransaction holder = manager.getDeviceTransaction(defaultDeviceId).get().orElseThrow();

            // More transactions queue up behind it than the manager has threads
            List<Future<Optional<DeviceTransaction>>> waiting = new LinkedList<>();
            for (int i = 0; i < listeningThreads + 1; i++) {
                waiting.add(manager.getDeviceTransaction(defaultDeviceId));
            }

            // A transaction on another device must not have to wait for the slow device
            Future<Optional<DeviceTransaction>> otherDevice = manager.getDeviceTransaction("other-device");
            await("transaction for another device").atMost(Duration.ofSeconds(1)).untilAsserted(() ->
                    assertTrue(otherDevice.isDone()));
            otherDevice.get().orElseThrow().commit(defaultTimeout, defaultTimeUnit);

            // The waiting transactions are still queued behind the one that holds the slow device
            waiting.forEach(future -> assertFalse(future.isDone()));

            holder.cancel();
            for (Future<Optional<DeviceTransaction>> future : waiting) {
                future.get(5, TimeUnit.SECONDS).orElseThrow().cancel();
            }
        } finally {
            manager.preDestroy();
        }
    }

    @Test
    void cancellingAWaitingTransactionDoesNotReleaseTheDeviceTest() throws Exception {
        // The first transaction holds the device
        final DeviceTransaction first = transactionManager.getDeviceTransaction(defaultDeviceId).get().orElseThrow();

        // The second one waits behind it, and its caller gives up
        Future<Optional<DeviceTransaction>> second = transactionManager.getDeviceTransaction(defaultDeviceId);
        assertFalse(second.isDone());
        assertTrue(second.cancel(false));

        // The first transaction still holds the device, so the third one must keep waiting
        Future<Optional<DeviceTransaction>> third = transactionManager.getDeviceTransaction(defaultDeviceId);
        await("third transaction keeps waiting").pollDelay(Duration.ofMillis(500)).untilAsserted(() ->
                assertFalse(third.isDone()));

        // Once the first transaction is committed, the third one gets the device
        first.commit(defaultTimeout, defaultTimeUnit);
        await("third transaction gets the device").atMost(Duration.ofSeconds(1)).untilAsserted(() ->
                assertTrue(third.isDone()));
        third.get().orElseThrow().commit(defaultTimeout, defaultTimeUnit);
        // The cancelled request must never create a transaction.
        verify(dataBrokerMock, times(2)).newReadWriteTransaction();
    }

    @Test
    void cancellationDuringCreationClosesTheTransactionBeforeStartingTheNextTest() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch finishCreation = new CountDownLatch(1);
        CountDownLatch cancelling = new CountDownLatch(1);
        CountDownLatch finishCancellation = new CountDownLatch(1);
        ReadWriteTransaction discarded = mock(ReadWriteTransaction.class);
        AtomicInteger creations = new AtomicInteger();
        when(dataBrokerMock.newReadWriteTransaction()).thenAnswer(invocation -> {
            if (creations.incrementAndGet() == 1) {
                creating.countDown();
                if (!finishCreation.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Transaction creation was not released");
                }
                return discarded;
            }
            return rwTransactionMock;
        });
        when(discarded.cancel()).thenAnswer(invocation -> {
            cancelling.countDown();
            if (!finishCancellation.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Transaction cancellation was not released");
            }
            return true;
        });

        try {
            Future<Optional<DeviceTransaction>> request = transactionManager.getDeviceTransaction(defaultDeviceId);
            assertTrue(creating.await(5, TimeUnit.SECONDS));
            assertTrue(request.cancel(false));
            Future<Optional<DeviceTransaction>> next = transactionManager.getDeviceTransaction(defaultDeviceId);

            // The caller has given up, but creation still owns this position in the device queue.
            assertThrows(TimeoutException.class, () -> next.get(100, TimeUnit.MILLISECONDS));
            finishCreation.countDown();
            assertTrue(cancelling.await(5, TimeUnit.SECONDS));

            // Even after creation finishes, the device remains held until the underlying cancel returns.
            assertThrows(TimeoutException.class, () -> next.get(100, TimeUnit.MILLISECONDS));
            finishCancellation.countDown();
            next.get(5, TimeUnit.SECONDS).orElseThrow().cancel();
            verify(discarded).cancel();
            verify(discarded, never()).commit();
        } finally {
            finishCreation.countDown();
            finishCancellation.countDown();
        }
    }

    @Test
    void advancedPositiveTransactionTest() {
        try {
            Future<Optional<DeviceTransaction>> firstDeviceTxFuture =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            DeviceTransaction firstDeviceTx = firstDeviceTxFuture.get().orElseThrow();

            Future<Optional<DeviceTransaction>> secondDeviceTxFuture =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            assertFalse(secondDeviceTxFuture.isDone());

            Future<Optional<DeviceTransaction>> thirdDeviceTxFuture =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            assertFalse(thirdDeviceTxFuture.isDone());

            firstDeviceTx.put(defaultDatastore, defaultIid, defaultData);
            await("simply wait...").pollDelay(Duration.ofMillis(400)).untilAsserted(() -> {
                assertFalse(secondDeviceTxFuture.isDone());
                assertFalse(thirdDeviceTxFuture.isDone());
            });

            Future<Optional<DeviceTransaction>> anotherDeviceTxFuture =
                    transactionManager.getDeviceTransaction("another-id");
            await("wait transaction for another-device").atMost(Duration.ofMillis(200)).untilAsserted(() -> {
                assertTrue(anotherDeviceTxFuture.isDone());
                anotherDeviceTxFuture.get().orElseThrow().commit(defaultTimeout, defaultTimeUnit);
            });

            firstDeviceTx.commit(defaultTimeout, defaultTimeUnit);
            await("wait second transaction for default device").atMost(Duration.ofMillis(600)).untilAsserted(() -> {
                assertTrue(secondDeviceTxFuture.isDone());
                assertFalse(thirdDeviceTxFuture.isDone());
            });

            DeviceTransaction secondDeviceTx = secondDeviceTxFuture.get().orElseThrow();
            secondDeviceTx.put(defaultDatastore, defaultIid, defaultData);
            assertFalse(thirdDeviceTxFuture.isDone());

            secondDeviceTx.commit(defaultTimeout, defaultTimeUnit);
            await("wait third transaction for default device").atMost(Duration.ofMillis(600)).untilAsserted(() -> {
                assertTrue(thirdDeviceTxFuture.isDone());
            });

            DeviceTransaction thirdDeviceTx = thirdDeviceTxFuture.get().orElseThrow();
            thirdDeviceTx.put(defaultDatastore, defaultIid, defaultData);
            thirdDeviceTx.commit(defaultTimeout, defaultTimeUnit);

            verify(rwTransactionMock, times(3)).put(defaultDatastore, defaultIid, defaultData);
            verify(rwTransactionMock, times(4)).commit();
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }
    }

    @Test
    void bigAmountOfTransactionsOnSameDeviceTest() {
        int numberOfTxs = 100;
        List<Future<Optional<DeviceTransaction>>> deviceTransactionFutures = new LinkedList<>();
        List<DeviceTransaction> deviceTransactions = new LinkedList<>();

        for (int i = 0; i < numberOfTxs; i++) {
            deviceTransactionFutures.add(transactionManager.getDeviceTransaction(defaultDeviceId));
        }

        try {
            for (Future<Optional<DeviceTransaction>> futureTx : deviceTransactionFutures) {
                DeviceTransaction deviceTx = futureTx.get().orElseThrow();
                deviceTx.commit(defaultTimeout, defaultTimeUnit);
                deviceTransactions.add(deviceTx);
            }
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }

        for (DeviceTransaction deviceTx : deviceTransactions) {
            assertTrue(deviceTx.wasSubmittedOrCancelled().get());
        }
    }

    @Test
    void bigAmountOfTransactionsOnDifferentDevicesTest() {
        int numberOfTxs = 1000;
        List<DeviceTransaction> deviceTransactions = new LinkedList<>();

        try {
            for (int i = 0; i < numberOfTxs; i++) {
                deviceTransactions.add(transactionManager.getDeviceTransaction(defaultDeviceId + " " + i).get()
                    .orElseThrow());
            }
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }

        deviceTransactions.parallelStream()
                .forEach(deviceTransaction -> deviceTransaction.commit(defaultTimeout, defaultTimeUnit));

        deviceTransactions.parallelStream()
                .forEach(deviceTransaction -> assertTrue(deviceTransaction.wasSubmittedOrCancelled().get()));
    }

    @Test
    void bigAmountOfTransactionsOnDifferentDevicesWithoutSubmitTest() {
        int numberOfTxs = 1000;
        List<DeviceTransaction> deviceTransactions = new LinkedList<>();

        try {
            for (int i = 0; i < numberOfTxs; i++) {
                deviceTransactions.add(transactionManager.getDeviceTransaction(defaultDeviceId + " " + i).get()
                    .orElseThrow());
            }
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }

        await().pollDelay(Duration.ofMillis(transactionManager.getMaxDurationToSubmitTransaction() + 1000))
            .untilAsserted(() -> {
                deviceTransactions.parallelStream()
                    .forEach(deviceTransaction -> assertTrue(deviceTransaction.wasSubmittedOrCancelled().get()));
            });
    }

    @Test
    void blockingListenerDoesNotDelayTheSubmitTimeoutTest() throws Exception {
        CountDownLatch listenerAdded = new CountDownLatch(1);
        CountDownLatch releaseListener = new CountDownLatch(1);
        when(dataBrokerMock.newReadWriteTransaction()).thenAnswer(invocation -> {
            // Hold creation until the caller's listener is in place, so the listener runs on completion
            if (!listenerAdded.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Listener was not added");
            }
            return rwTransactionMock;
        });

        try {
            ListenableFuture<Optional<DeviceTransaction>> request =
                    transactionManager.getDeviceTransaction(defaultDeviceId, 100, TimeUnit.MILLISECONDS);
            request.addListener(() -> {
                try {
                    releaseListener.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, MoreExecutors.directExecutor());
            listenerAdded.countDown();

            // The transaction is cancelled by its timeout while the listener is still blocked
            verify(rwTransactionMock, timeout(1000)).cancel();
        } finally {
            releaseListener.countDown();
        }
    }

    @Test
    void shutdownDuringCreationFailsTheRequestAndCancelsTheTransactionTest() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch finishCreation = new CountDownLatch(1);
        when(dataBrokerMock.newReadWriteTransaction()).thenAnswer(invocation -> {
            creating.countDown();
            if (!finishCreation.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Transaction creation was not released");
            }
            return rwTransactionMock;
        });

        try {
            final Future<Optional<DeviceTransaction>> request =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            assertTrue(creating.await(5, TimeUnit.SECONDS));

            // The submit timeout can no longer be scheduled once creation finishes
            transactionManager.preDestroy();
            finishCreation.countDown();

            ExecutionException thrown =
                    assertThrows(ExecutionException.class, () -> request.get(5, TimeUnit.SECONDS));
            assertInstanceOf(RejectedExecutionException.class, thrown.getCause());
            verify(rwTransactionMock).cancel();
        } finally {
            finishCreation.countDown();
        }
    }

    @Test
    void shutdownDuringCreationFailsTheRequestEvenIfCancellingFailsTest() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch finishCreation = new CountDownLatch(1);
        when(dataBrokerMock.newReadWriteTransaction()).thenAnswer(invocation -> {
            creating.countDown();
            if (!finishCreation.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Transaction creation was not released");
            }
            return rwTransactionMock;
        });
        when(rwTransactionMock.cancel()).thenThrow(new IllegalStateException("cancel failed"));

        try {
            final Future<Optional<DeviceTransaction>> request =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            assertTrue(creating.await(5, TimeUnit.SECONDS));

            // The submit timeout can no longer be scheduled once creation finishes
            transactionManager.preDestroy();
            finishCreation.countDown();

            ExecutionException thrown =
                    assertThrows(ExecutionException.class, () -> request.get(5, TimeUnit.SECONDS));
            assertInstanceOf(RejectedExecutionException.class, thrown.getCause());
            verify(rwTransactionMock).cancel();
        } finally {
            finishCreation.countDown();
        }
    }

    @Test
    void notSubmittedTransactionTest() {
        Future<Optional<DeviceTransaction>> deviceTxFuture =
                transactionManager.getDeviceTransaction(defaultDeviceId);

        try {
            deviceTxFuture.get();
            await().pollDelay(Duration.ofMillis(transactionManager.getMaxDurationToSubmitTransaction() + 1000))
                .untilAsserted(() -> {
                    verify(rwTransactionMock, times(1)).cancel();
                });
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        verify(rwTransactionMock, times(1)).cancel();
        verify(rwTransactionMock, times(1)).put(defaultDatastore, defaultIid, defaultData);
        verify(rwTransactionMock, times(1)).commit();
    }

    @Test
    void dataBrokerTimeoutTransactionTest() {
        when(dataBrokerMock.newReadWriteTransaction()).then(invocation -> {
            await().pollDelay(Duration.ofMillis(transactionManager.getMaxDurationToSubmitTransaction() + 1000))
                .untilAsserted(() -> {
                });
            return rwTransactionMock;
        });

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }

        verify(rwTransactionMock, times(1)).commit();

        when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock); // remove sleep

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        verify(rwTransactionMock, times(2)).put(defaultDatastore, defaultIid, defaultData);
        verify(rwTransactionMock, times(2)).commit();
    }

    @Test
    void getFutureTimeoutTransactionTest() {
        when(dataBrokerMock.newReadWriteTransaction()).then(invocation -> {
            await().pollDelay(Duration.ofSeconds(3)).untilAsserted(() -> {
            });
            return rwTransactionMock;
        });

        Exception throwedException = null;

        Future<Optional<DeviceTransaction>> deviceTxFuture =
                transactionManager.getDeviceTransaction(defaultDeviceId);
        try {
            deviceTxFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        } catch (TimeoutException e) {
            throwedException = e;
        }

        if (throwedException == null) {
            fail("TimeoutException should be thrown!");
            return;
        }

        when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock); // remove sleep

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        verify(rwTransactionMock, times(1)).put(defaultDatastore, defaultIid, defaultData);
        verify(rwTransactionMock, times(1)).commit();
    }

    private <T extends DataObject> void putAndSubmit(DeviceTransactionManagerImpl deviceTxManager, String deviceId,
            LogicalDatastoreType store, DataObjectIdentifier<T> path, T data)
            throws ExecutionException, InterruptedException {
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTxManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx = deviceTxFuture.get().orElseThrow();
        deviceTx.put(store, path, data);
        deviceTx.commit(defaultTimeout, defaultTimeUnit);
    }
}
