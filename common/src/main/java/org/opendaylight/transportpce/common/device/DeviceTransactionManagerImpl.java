/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeviceTransactionManagerImpl implements DeviceTransactionManager {

    // TODO cache device data brokers
    // TODO remove disconnected devices from maps

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTransactionManagerImpl.class);
    private static final int NUMBER_OF_THREADS = 4;
    private static final long GET_DATA_SUBMIT_TIMEOUT = 3000;
    private static final TimeUnit GET_DATA_SUBMIT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final MountPointService mountPointService;
    private final ScheduledExecutorService checkingExecutor;
    private final ListeningExecutorService listeningExecutor;
    private final ConcurrentMap<String, CountDownLatch> deviceLocks;
    private final long maxDurationToSubmitTransaction; // TODO set reasonable value in blueprint
    private final TimeUnit maxDurationToSubmitTransactionTimeUnit = TimeUnit.MILLISECONDS;

    public DeviceTransactionManagerImpl(MountPointService mountPointService, long maxDurationToSubmitTransaction) {
        this.mountPointService = mountPointService;
        this.maxDurationToSubmitTransaction = maxDurationToSubmitTransaction;
        this.deviceLocks = new ConcurrentHashMap<>();
        this.checkingExecutor = Executors.newScheduledThreadPool(NUMBER_OF_THREADS);
        this.listeningExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
    }

    @Override
    public Future<Optional<DeviceTransaction>> getDeviceTransaction(String deviceId) {
        return getDeviceTransaction(deviceId, maxDurationToSubmitTransaction, maxDurationToSubmitTransactionTimeUnit);
    }

    @Override
    public Future<Optional<DeviceTransaction>> getDeviceTransaction(String deviceId, long timeoutToSubmit,
            TimeUnit timeUnit) {
        CountDownLatch newLock = new CountDownLatch(1);
        ListenableFuture<Optional<DeviceTransaction>> future = listeningExecutor.submit(() -> {
            LOG.debug("Starting creation of transaction for device {}.", deviceId);
            // get current lock from device and set new lock
            CountDownLatch actualLock = swapActualLock(deviceId, newLock);
            if (actualLock != null) {
                // if lock was present on device wait until it unlocks
                actualLock.await();
            }

            Optional<DataBroker> deviceDataBrokerOpt = getDeviceDataBroker(deviceId);
            DataBroker deviceDataBroker;
            if (deviceDataBrokerOpt.isPresent()) {
                deviceDataBroker = deviceDataBrokerOpt.get();
            } else {
                newLock.countDown();
                return Optional.empty();
            }
            LOG.debug("Created transaction for device {}.", deviceId);
            return Optional.of(new DeviceTransaction(deviceDataBroker.newReadWriteTransaction(), newLock));
        });

        Futures.addCallback(future, new FutureCallback<Optional<DeviceTransaction>>() {
            @Override
            public void onSuccess(Optional<DeviceTransaction> deviceTransactionOptional) {
                // creates timeout for transaction to submit right after transaction is created
                // if time will run out and transaction was not closed then it will be cancelled (and unlocked)
                checkingExecutor.schedule(() -> {
                    if (deviceTransactionOptional.isPresent()) {
                        DeviceTransaction deviceTx = deviceTransactionOptional.get();
                        LOG.debug("Timeout to submit transaction run out! Transaction was {} submitted or canceled.",
                                deviceTx.wasSubmittedOrCancelled().get() ? "" : "not");
                        if (!deviceTx.wasSubmittedOrCancelled().get()) {
                            LOG.error(String.format("Transaction for node %s was not submitted or canceled after %s"
                                            + " milliseconds! Cancelling transaction!", deviceId,
                                    timeoutToSubmit));
                            deviceTx.cancel();
                        }
                    }
                }, timeoutToSubmit, timeUnit);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Exception thrown while getting device transaction for device {}! Unlocking device.",
                        deviceId, t);
                newLock.countDown();
            }
        }, checkingExecutor);

        return future;
    }

    private synchronized CountDownLatch swapActualLock(String deviceId, CountDownLatch newLock) {
        return deviceLocks.put(deviceId, newLock);
    }

    private Optional<DataBroker> getDeviceDataBroker(String deviceId) {
        Optional<MountPoint> netconfNode = getDeviceMountPoint(deviceId);
        if (netconfNode.isPresent()) {
            return netconfNode.get().getService(DataBroker.class).toJavaUtil();
        } else {
            LOG.error("Device mount point not found for : {}", deviceId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MountPoint> getDeviceMountPoint(String deviceId) {
        InstanceIdentifier<Node> netconfNodeIID = InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class,
                new NodeKey(new NodeId(deviceId)));
        return mountPointService.getMountPoint(netconfNodeIID).toJavaUtil();
    }

    @Override
    public <T extends DataObject> Optional<T> getDataFromDevice(String deviceId,
            LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<T> path, long timeout, TimeUnit timeUnit) {
        Optional<DeviceTransaction> deviceTxOpt;
        try {
            deviceTxOpt = getDeviceTransaction(deviceId, timeout, timeUnit).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception thrown while getting transaction for device {}!", deviceId, e);
            return Optional.empty();
        }
        if (deviceTxOpt.isPresent()) {
            DeviceTransaction deviceTx = deviceTxOpt.get();
            try {
                return deviceTx.read(logicalDatastoreType, path).get(timeout, timeUnit).toJavaUtil();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Exception thrown while reading data from device {}! IID: {}", deviceId, path, e);
            } finally {
                deviceTx.submit(GET_DATA_SUBMIT_TIMEOUT, GET_DATA_SUBMIT_TIME_UNIT);
            }
        } else {
            LOG.error("Could not obtain transaction for device {}!", deviceId);
        }
        return Optional.empty();
    }

    @Override
    public boolean isDeviceMounted(String deviceId) {
        return getDeviceDataBroker(deviceId).isPresent();
    }

    public void preDestroy() {
        checkingExecutor.shutdown();
        listeningExecutor.shutdown();
    }

    public long getMaxDurationToSubmitTransaction() {
        return maxDurationToSubmitTransaction;
    }
}
