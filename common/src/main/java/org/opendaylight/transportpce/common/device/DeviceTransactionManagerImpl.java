/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import static java.util.Objects.requireNonNull;

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
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = DeviceTransactionManagerImpl.Configuration.class)
public final class DeviceTransactionManagerImpl implements DeviceTransactionManager {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition(description = "Minimum number of threads in the checking pool", min = "0")
        int checkingMinThreads() default DEFAULT_CHECKING_MIN_THREADS;
        @AttributeDefinition(description = "Number of threads in the listening pool", min = "1")
        int listeningThreads() default DEFAULT_LISTENING_THREADS;
        @AttributeDefinition(description = "Maximum time to wait for transaction submit, in milliseconds", min = "0")
        long maxDurationToSubmit() default DEFAULT_MAX_DURATION_TO_SUBMIT;
        @AttributeDefinition(description = "Maximum time to wait for get-data submit, in milliseconds", min = "0")
        long maxDurationToGetData() default DEFAULT_MAX_DURATION_TO_GET_DATA;
    }

    // TODO cache device data brokers
    // TODO remove disconnected devices from maps

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTransactionManagerImpl.class);
    private static final long DEFAULT_MAX_DURATION_TO_GET_DATA = 3000;
    private static final long DEFAULT_MAX_DURATION_TO_SUBMIT = 15000;
    private static final int DEFAULT_CHECKING_MIN_THREADS = 4;
    private static final int DEFAULT_LISTENING_THREADS = 4;

    private final MountPointService mountPointService;
    private final ScheduledExecutorService checkingExecutor;
    private final ListeningExecutorService listeningExecutor;
    private final ConcurrentMap<String, CountDownLatch> deviceLocks = new ConcurrentHashMap<>();
    private final long maxDurationToSubmitTransaction;
    private final long maxDurationToGetData;

    @Activate
    public DeviceTransactionManagerImpl(@Reference MountPointService mountPointService, Configuration configuration) {
        this(mountPointService, configuration.maxDurationToSubmit(), configuration.maxDurationToGetData(),
            configuration.checkingMinThreads(), configuration.listeningThreads());
    }

    public DeviceTransactionManagerImpl(MountPointService mountPointService, long maxDurationToSubmitTransaction) {
        this(mountPointService, maxDurationToSubmitTransaction, DEFAULT_MAX_DURATION_TO_GET_DATA,
            DEFAULT_CHECKING_MIN_THREADS, DEFAULT_LISTENING_THREADS);
    }

    public DeviceTransactionManagerImpl(MountPointService mountPointService, long maxDurationToSubmitTransaction,
            long maxDurationToGetData, int checkingPoolMinThreads, int listeningPoolThreads) {
        this.mountPointService = requireNonNull(mountPointService);
        this.maxDurationToSubmitTransaction = maxDurationToSubmitTransaction;
        this.maxDurationToGetData = maxDurationToGetData;
        this.checkingExecutor = Executors.newScheduledThreadPool(checkingPoolMinThreads);
        this.listeningExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(listeningPoolThreads));
    }

    @Override
    public Future<Optional<DeviceTransaction>> getDeviceTransaction(String deviceId) {
        return getDeviceTransaction(deviceId, maxDurationToSubmitTransaction, TimeUnit.MILLISECONDS);
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
                deviceDataBroker = deviceDataBrokerOpt.orElseThrow();
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
                        DeviceTransaction deviceTx = deviceTransactionOptional.orElseThrow();
                        LOG.debug("Timeout to submit transaction run out! Transaction was {} submitted or canceled.",
                                deviceTx.wasSubmittedOrCancelled().get() ? "" : "not");
                        if (!deviceTx.wasSubmittedOrCancelled().get()) {
                            LOG.error(
                                "Transaction for node {} not submitted/canceled after {} ms. Cancelling transaction.",
                                deviceId, timeoutToSubmit);
                            deviceTx.cancel();
                        }
                    }
                }, timeoutToSubmit, timeUnit);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Exception thrown while getting device transaction for device {}! Unlocking device.",
                        deviceId, throwable);
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
            return netconfNode.orElseThrow().getService(DataBroker.class);
        } else {
            LOG.error("Device mount point not found for : {}", deviceId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MountPoint> getDeviceMountPoint(String deviceId) {
        DataObjectIdentifier<Node> netconfNodeIID = InstanceIdentifiers.NETCONF_TOPOLOGY_II
                .toBuilder()
                .child(Node.class, new NodeKey(new NodeId(deviceId)))
                .build();
        return mountPointService.findMountPoint(netconfNodeIID);
    }

    @Override
    public <T extends DataObject> Optional<T> getDataFromDevice(String deviceId,
            LogicalDatastoreType logicalDatastoreType, DataObjectIdentifier<T> path, long timeout, TimeUnit timeUnit) {
        Optional<DeviceTransaction> deviceTxOpt;
        try {
            deviceTxOpt = getDeviceTransaction(deviceId, timeout, timeUnit).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception thrown while getting transaction for device {}!", deviceId, e);
            return Optional.empty();
        }
        if (deviceTxOpt.isPresent()) {
            DeviceTransaction deviceTx = deviceTxOpt.orElseThrow();
            try {
                return deviceTx.read(logicalDatastoreType, path).get(timeout, timeUnit);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Exception thrown while reading data from device {}! IID: {}", deviceId, path, e);
            } finally {
                deviceTx.commit(maxDurationToGetData, TimeUnit.MILLISECONDS);
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

    @Deactivate
    public void preDestroy() {
        checkingExecutor.shutdown();
        listeningExecutor.shutdown();
    }

    public long getMaxDurationToSubmitTransaction() {
        return maxDurationToSubmitTransaction;
    }
}
