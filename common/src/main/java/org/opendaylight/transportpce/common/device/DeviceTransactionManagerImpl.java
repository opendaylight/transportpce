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
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
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
    private final ConcurrentMap<String, ListenableFuture<Void>> deviceQueues = new ConcurrentHashMap<>();
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
    public ListenableFuture<Optional<DeviceTransaction>> getDeviceTransaction(String deviceId) {
        return getDeviceTransaction(deviceId, maxDurationToSubmitTransaction, TimeUnit.MILLISECONDS);
    }

    @Override
    public ListenableFuture<Optional<DeviceTransaction>> getDeviceTransaction(
            String deviceId,
            long timeoutToSubmit,
            TimeUnit unit) {

        final SettableFuture<Void> released = SettableFuture.create(); // Completed by commit()/cancel()
        // When this transaction is closed and nobody has queued behind it, forget the device
        released.addListener(() -> deviceQueues.remove(deviceId, released), MoreExecutors.directExecutor());

        final ListenableFuture<Void> previous = enqueue(deviceId, released); // atomic swap of the device's tail
        // Caller cancellation must not discard a transaction while it is being created.
        final SettableFuture<Optional<DeviceTransaction>> result = SettableFuture.create();
        ListenableFuture<Optional<DeviceTransaction>> creation = Futures.transform(
                Futures.nonCancellationPropagating(previous),
                ignored -> {
                    if (result.isCancelled()) {
                        released.set(null);
                        return Optional.<DeviceTransaction>empty();
                    }
                    return createTransaction(deviceId, released);
                },
                listeningExecutor);

        Futures.addCallback(creation, new FutureCallback<Optional<DeviceTransaction>>() {
            @Override
            public void onSuccess(Optional<DeviceTransaction> deviceTx) {
                if (deviceTx.isPresent()) {
                    try {
                        // Before set(): it runs the caller's direct listeners, which may block. The timeout does
                        // nothing if the transaction is already closed, so it is harmless when cancellation wins the
                        // race below.
                        scheduleSubmitTimeout(deviceId, deviceTx.orElseThrow(), timeoutToSubmit, unit);
                    } catch (RejectedExecutionException e) {
                        // preDestroy() has stopped the scheduler. Do not hand out a transaction without a timeout.
                        LOG.warn("Could not schedule the submit timeout for device {}, cancelling its transaction",
                                deviceId, e);
                        try {
                            deviceTx.orElseThrow().cancel();
                        } finally {
                            // Even if cancelling fails, the caller must not be left waiting
                            result.setException(e);
                        }
                        return;
                    }
                }
                if (!result.set(deviceTx)) {
                    // Cancellation won the race with creation. Close the transaction before releasing the device.
                    deviceTx.ifPresent(DeviceTransaction::cancel);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                if (throwable instanceof CancellationException) {
                    LOG.debug("Transaction request for device {} was cancelled", deviceId);
                } else {
                    LOG.error("Could not create transaction for device {}! Unlocking device.", deviceId, throwable);
                }
                // Keep our place in the queue until the transaction in front of us is done, then let the next one go.
                // setFuture copies previous's result, so this relies on released never failing: it is only ever
                // completed with set(null) or by mirroring another released.
                released.setFuture(previous);
                result.setException(throwable);
            }
        }, MoreExecutors.directExecutor());

        return result;
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
        try {
            // Existing callers keep waiting for the device without a limit, as before
            return read(deviceId, logicalDatastoreType, path, timeout, timeUnit, false);
        } catch (DeviceReadException e) {
            LOG.error("Could not read {} from device {}", path, deviceId, e);
            return Optional.empty();
        }
    }

    @Override
    public <T extends DataObject> Optional<T> readFromDevice(
            String deviceId,
            LogicalDatastoreType type,
            DataObjectIdentifier<T> path,
            long timeout,
            TimeUnit unit) throws DeviceReadException {
        return read(deviceId, type, path, timeout, unit, true);
    }

    private <T extends DataObject> Optional<T> read(
            String deviceId,
            LogicalDatastoreType type,
            DataObjectIdentifier<T> path,
            long timeout,
            TimeUnit unit,
            boolean boundedWait) throws DeviceReadException {

        // The transaction's own timeout is only a safety net: the read closes the transaction when it is done or
        // times out, so the safety net must not fire before that
        DeviceTransaction tx = awaitTransaction(deviceId, timeout, unit, boundedWait,
                unit.toMillis(timeout) + maxDurationToGetData);
        try {
            return tx.read(type, path, timeout, unit);
        } finally {
            tx.commit(maxDurationToGetData, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public DeviceTransaction awaitDeviceTransaction(String deviceId, long waitTimeout, long useTimeout,
            TimeUnit unit) throws DeviceReadException {
        return awaitTransaction(deviceId, waitTimeout, unit, true, unit.toMillis(useTimeout));
    }

    /**
     * Waits for this request's turn on the device and returns its transaction.
     *
     * @param boundedWait if true, gives up after the timeout; otherwise waits as long as it takes
     * @param submitTimeoutMillis after how long the transaction is cancelled if it is still open
     */
    private DeviceTransaction awaitTransaction(String deviceId, long timeout, TimeUnit unit, boolean boundedWait,
            long submitTimeoutMillis) throws DeviceReadException {
        ListenableFuture<Optional<DeviceTransaction>> txFuture =
                getDeviceTransaction(deviceId, submitTimeoutMillis, TimeUnit.MILLISECONDS);
        Optional<DeviceTransaction> tx;
        try {
            tx = boundedWait ? txFuture.get(timeout, unit) : txFuture.get();
        } catch (TimeoutException e) {
            giveUp(txFuture);
            throw new DeviceReadException("Timed out after " + timeout + " " + unit + " waiting for device "
                    + deviceId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            giveUp(txFuture);
            throw new DeviceReadException("Interrupted while waiting for device " + deviceId, e);
        } catch (ExecutionException e) {
            throw new DeviceReadException("Could not open a transaction on device " + deviceId, e);
        }
        return tx.orElseThrow(() -> new DeviceReadException("Device " + deviceId + " is not mounted"));
    }

    /**
     * Gives up a request for a device transaction without disturbing the device's queue.
     */
    private static void giveUp(ListenableFuture<Optional<DeviceTransaction>> txFuture) {
        if (!txFuture.cancel(false)) {
            // The transaction was created just as we gave up: close it, so the next one in line gets the device
            try {
                Futures.getDone(txFuture).ifPresent(DeviceTransaction::cancel);
            } catch (ExecutionException | CancellationException e) {
                LOG.debug("No transaction to close after giving up", e);
            }
        }
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

    private ListenableFuture<Void> enqueue(String deviceId, SettableFuture<Void> released) {
        ListenableFuture<Void> previous = deviceQueues.put(deviceId, released);

        //Nobody in line: there is nothing to wait for
        return previous != null ? previous : Futures.immediateVoidFuture();
    }

    private Optional<DeviceTransaction> createTransaction(String deviceId, SettableFuture<Void> released) {
        Optional<DataBroker> dataBroker = getDeviceDataBroker(deviceId);
        if (dataBroker.isEmpty()) {
            // No transaction will ever hold the device, so let the next one in line go now
            released.set(null);
            return Optional.empty();
        }
        LOG.debug("Created transaction for device {}", deviceId);
        return Optional.of(
                new DeviceTransaction(deviceId, dataBroker.orElseThrow().newReadWriteTransaction(), released));
    }

    private void scheduleSubmitTimeout(
            String deviceId,
            DeviceTransaction deviceTx,
            long timeoutToSubmit,
            TimeUnit timeUnit) {

        checkingExecutor.schedule(() -> {
            LOG.debug("Timeout to submit transaction run out! Transaction was {} submitted or canceled.",
                    deviceTx.wasSubmittedOrCancelled().get() ? "" : "not");
            if (!deviceTx.wasSubmittedOrCancelled().get()) {
                LOG.error(
                        "Transaction for node {} not submitted/canceled after {} ms. Cancelling transaction.",
                        deviceId, timeoutToSubmit);
                deviceTx.cancel();
            }
        }, timeoutToSubmit, timeUnit);
    }
}
