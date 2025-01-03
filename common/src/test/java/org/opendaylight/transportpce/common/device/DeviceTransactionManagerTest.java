/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

        Mockito.verify(rwTransactionMock, Mockito.times(1)).put(defaultDatastore, defaultIid, defaultData);
        Mockito.verify(rwTransactionMock, Mockito.times(1)).commit();
    }

    @Test
    void advancedPositiveTransactionTest() {
        try {
            Future<java.util.Optional<DeviceTransaction>> firstDeviceTxFuture =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            DeviceTransaction firstDeviceTx = firstDeviceTxFuture.get().orElseThrow();

            Future<java.util.Optional<DeviceTransaction>> secondDeviceTxFuture =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            assertFalse(secondDeviceTxFuture.isDone());

            Future<java.util.Optional<DeviceTransaction>> thirdDeviceTxFuture =
                    transactionManager.getDeviceTransaction(defaultDeviceId);
            assertFalse(thirdDeviceTxFuture.isDone());

            firstDeviceTx.put(defaultDatastore, defaultIid, defaultData);
            assertFalse(secondDeviceTxFuture.isDone());
            assertFalse(thirdDeviceTxFuture.isDone());
            Thread.sleep(200);
            assertFalse(secondDeviceTxFuture.isDone());
            assertFalse(thirdDeviceTxFuture.isDone());

            Future<java.util.Optional<DeviceTransaction>> anotherDeviceTxFuture =
                    transactionManager.getDeviceTransaction("another-id");
            Thread.sleep(50);
            assertTrue(anotherDeviceTxFuture.isDone());
            anotherDeviceTxFuture.get().orElseThrow().commit(defaultTimeout, defaultTimeUnit);

            firstDeviceTx.commit(defaultTimeout, defaultTimeUnit);
            Thread.sleep(200);
            assertTrue(secondDeviceTxFuture.isDone());
            assertFalse(thirdDeviceTxFuture.isDone());

            DeviceTransaction secondDeviceTx = secondDeviceTxFuture.get().orElseThrow();
            secondDeviceTx.put(defaultDatastore, defaultIid, defaultData);
            assertFalse(thirdDeviceTxFuture.isDone());

            secondDeviceTx.commit(defaultTimeout, defaultTimeUnit);
            Thread.sleep(200);
            assertTrue(thirdDeviceTxFuture.isDone());

            DeviceTransaction thirdDeviceTx = thirdDeviceTxFuture.get().orElseThrow();
            thirdDeviceTx.put(defaultDatastore, defaultIid, defaultData);
            thirdDeviceTx.commit(defaultTimeout, defaultTimeUnit);

            Mockito.verify(rwTransactionMock, Mockito.times(3)).put(defaultDatastore, defaultIid, defaultData);
            Mockito.verify(rwTransactionMock, Mockito.times(4)).commit();
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }
    }

    @Test
    void bigAmountOfTransactionsOnSameDeviceTest() {
        int numberOfTxs = 100;
        List<Future<java.util.Optional<DeviceTransaction>>> deviceTransactionFutures = new LinkedList<>();
        List<DeviceTransaction> deviceTransactions = new LinkedList<>();

        for (int i = 0; i < numberOfTxs; i++) {
            deviceTransactionFutures.add(transactionManager.getDeviceTransaction(defaultDeviceId));
        }

        try {
            for (Future<java.util.Optional<DeviceTransaction>> futureTx : deviceTransactionFutures) {
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

        try {
            Thread.sleep(transactionManager.getMaxDurationToSubmitTransaction() + 1000);
        } catch (InterruptedException e) {
            fail("Exception catched! " + e);
        }
        deviceTransactions.parallelStream()
                .forEach(deviceTransaction -> assertTrue(deviceTransaction.wasSubmittedOrCancelled().get()));
    }

    @Test
    void notSubmittedTransactionTest() {
        Future<java.util.Optional<DeviceTransaction>> deviceTxFuture =
                transactionManager.getDeviceTransaction(defaultDeviceId);
        try {
            deviceTxFuture.get();
            Thread.sleep(transactionManager.getMaxDurationToSubmitTransaction() + 1000);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }
        Mockito.verify(rwTransactionMock, Mockito.times(1)).cancel();

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        Mockito.verify(rwTransactionMock, Mockito.times(1)).cancel();
        Mockito.verify(rwTransactionMock, Mockito.times(1)).put(defaultDatastore, defaultIid, defaultData);
        Mockito.verify(rwTransactionMock, Mockito.times(1)).commit();
    }

    @Test
    void dataBrokerTimeoutTransactionTest() {
        Mockito.when(dataBrokerMock.newReadWriteTransaction()).then(invocation -> {
            Thread.sleep(transactionManager.getMaxDurationToSubmitTransaction() + 1000);
            return rwTransactionMock;
        });

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
        }

        Mockito.verify(rwTransactionMock, Mockito.times(1)).commit();

        Mockito.when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock); // remove sleep

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        Mockito.verify(rwTransactionMock, Mockito.times(2)).put(defaultDatastore, defaultIid, defaultData);
        Mockito.verify(rwTransactionMock, Mockito.times(2)).commit();
    }

    @Test
    void getFutureTimeoutTransactionTest() {
        Mockito.when(dataBrokerMock.newReadWriteTransaction()).then(invocation -> {
            Thread.sleep(3000);
            return rwTransactionMock;
        });

        Exception throwedException = null;

        Future<java.util.Optional<DeviceTransaction>> deviceTxFuture =
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

        Mockito.when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock); // remove sleep

        try {
            putAndSubmit(transactionManager, defaultDeviceId, defaultDatastore, defaultIid, defaultData);
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception catched! " + e);
            return;
        }

        Mockito.verify(rwTransactionMock, Mockito.times(1)).put(defaultDatastore, defaultIid, defaultData);
        Mockito.verify(rwTransactionMock, Mockito.times(1)).commit();
    }

    private <T extends DataObject> void putAndSubmit(DeviceTransactionManagerImpl deviceTxManager, String deviceId,
            LogicalDatastoreType store, DataObjectIdentifier<T> path, T data)
            throws ExecutionException, InterruptedException {
        Future<java.util.Optional<DeviceTransaction>> deviceTxFuture = deviceTxManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx = deviceTxFuture.get().orElseThrow();
        deviceTx.put(store, path, data);
        deviceTx.commit(defaultTimeout, defaultTimeUnit);
    }
}
