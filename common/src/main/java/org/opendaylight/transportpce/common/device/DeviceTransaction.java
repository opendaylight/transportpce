/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents read-write transaction on netconf device.
 * This transaction can be obtained by {@link DeviceTransactionManager}.
 *
 * <p>WARNING: Only one transaction can be opened at the same time on device!
 * It's important to close (cancel/submit) transaction when work is done with it
 * (so others can access the device).
 */
public class DeviceTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTransaction.class);

    private final String deviceId;
    private final ReadWriteTransaction rwTx;
    private final SettableFuture<Void> released;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean wasSubmittedOrCancelled = new AtomicBoolean(false);

    DeviceTransaction(String deviceId, ReadWriteTransaction rwTx, SettableFuture<Void> released) {
        this.deviceId = deviceId;
        this.rwTx = rwTx;
        this.released = released;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        LOG.debug("Device transaction created. Lock: {}", released);
    }

    public <T extends DataObject> ListenableFuture<Optional<T>> read(LogicalDatastoreType store,
            DataObjectIdentifier<T> path) {
        return rwTx.read(store, path);
    }

    /**
     * Reads data in this transaction, telling "no data" apart from "could not read", like
     * {@link DeviceTransactionManager#readFromDevice(String, LogicalDatastoreType, DataObjectIdentifier, long,
     * TimeUnit)}.
     *
     * <p>The transaction stays open; the caller still commits or cancels it.
     *
     * @param store datastore to read from
     * @param path path of the data in the device's datastore
     * @param timeout maximum time for the read
     * @param unit unit of {@code timeout}
     * @param <T> type of the data
     * @return the data, or an empty Optional if the device has no data at this path
     * @throws DeviceReadException if the read failed or did not complete in time
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T extends DataObject> Optional<T> read(LogicalDatastoreType store, DataObjectIdentifier<T> path,
            long timeout, TimeUnit unit) throws DeviceReadException {
        try {
            return rwTx.read(store, path).get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeviceReadException("Interrupted while reading " + path + " from device " + deviceId, e);
        } catch (TimeoutException e) {
            throw new DeviceReadException("Timed out after " + timeout + " " + unit + " reading " + path
                    + " from device " + deviceId, e);
        } catch (CancellationException e) {
            throw new DeviceReadException("Reading " + path + " from device " + deviceId + " was cancelled", e);
        } catch (ExecutionException e) {
            throw new DeviceReadException("Reading " + path + " from device " + deviceId + " failed", e);
        } catch (RuntimeException e) {
            // Thrown by the read itself, for example when the mount point is torn down while the read is issued
            throw new DeviceReadException("Reading " + path + " from device " + deviceId + " failed", e);
        }
    }

    /**
     * Reads several paths in this transaction at once, telling "no data" apart from "could not read", like
     * {@link #read(LogicalDatastoreType, DataObjectIdentifier, long, TimeUnit)}.
     *
     * <p>All reads are started before waiting for any of them, and they share a single deadline, so reading many
     * paths takes about as long as reading one. The transaction stays open; the caller still commits or cancels it.
     *
     * @param store datastore to read from
     * @param paths paths of the data in the device's datastore
     * @param timeout maximum time for all the reads together
     * @param unit unit of {@code timeout}
     * @param <T> type of the data
     * @return the data at each path, in the order of {@code paths}, with an empty Optional where the device has no
     *     data
     * @throws DeviceReadException if any of the reads failed or did not complete in time, naming its path
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T extends DataObject> List<Optional<T>> readAll(
            LogicalDatastoreType store,
            List<DataObjectIdentifier<T>> paths,
            long timeout,
            TimeUnit unit) throws DeviceReadException {

        long deadline = System.nanoTime() + unit.toNanos(timeout);
        List<ListenableFuture<Optional<T>>> reads = new ArrayList<>(paths.size());
        for (DataObjectIdentifier<T> path : paths) {
            try {
                reads.add(rwTx.read(store, path));
            } catch (RuntimeException e) {
                // Thrown by the read itself, for example when the mount point is torn down while the read is issued
                reads.forEach(read -> read.cancel(false));
                throw new DeviceReadException("Reading " + path + " from device " + deviceId + " failed", e);
            }
        }
        List<Optional<T>> results = new ArrayList<>(paths.size());
        try {
            for (int i = 0; i < reads.size(); i++) {
                results.add(await(reads.get(i), paths.get(i), deadline, timeout, unit));
            }
        } finally {
            // Only the reads after a failed one are still running; there is no point in waiting for them
            reads.forEach(read -> read.cancel(false));
        }
        return results;
    }

    private <T extends DataObject> Optional<T> await(
            ListenableFuture<Optional<T>> read,
            DataObjectIdentifier<T> path,
            long deadline,
            long timeout,
            TimeUnit unit) throws DeviceReadException {

        try {
            return read.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeviceReadException("Interrupted while reading " + path + " from device " + deviceId, e);
        } catch (TimeoutException e) {
            throw new DeviceReadException("Timed out after " + timeout + " " + unit + " reading " + path
                    + " from device " + deviceId, e);
        } catch (CancellationException e) {
            throw new DeviceReadException("Reading " + path + " from device " + deviceId + " was cancelled", e);
        } catch (ExecutionException e) {
            throw new DeviceReadException("Reading " + path + " from device " + deviceId + " failed", e);
        }
    }

    public <T extends DataObject> void put(LogicalDatastoreType store, DataObjectIdentifier<T> path, T data) {
        rwTx.put(store, path, data);
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store, DataObjectIdentifier<T> path, T data) {
        rwTx.merge(store, path, data);
    }


    public void delete(LogicalDatastoreType store, DataObjectIdentifier<?> path) {
        rwTx.delete(store, path);
    }

    /**
     * Cancels transaction and unlocks it.
     *
     * @return true if cancel was successful.
     */
    public boolean cancel() {
        // Atomic, so the submit timeout cannot cancel a transaction that is being committed
        if (!wasSubmittedOrCancelled.compareAndSet(false, true)) {
            LOG.warn("Transaction was already submitted or canceled!");
            return false;
        }

        LOG.debug("Transaction cancelled. Lock: {}", released);
        try {
            return rwTx.cancel();
        } finally {
            afterClose();
        }
    }

    /**
     * Submits data changed in transaction to device with defined timeout to commit. If time from timeout runs out then
     * the commit will be interrupted and the device will be unlocked.
     *
     * @param timeout a timeout
     * @param timeUnit a time unit
     * @return FluentFuture which indicates when the commit is completed, failed if the commit could not be started.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public FluentFuture<? extends @NonNull CommitInfo> commit(long timeout, TimeUnit timeUnit) {
        // Atomic, so the submit timeout cannot cancel a transaction that is being committed
        if (!wasSubmittedOrCancelled.compareAndSet(false, true)) {
            String msg = "Transaction was already submitted or canceled!";
            LOG.error(msg);
            return FluentFutures.immediateFailedFluentFuture(new IllegalStateException(msg));
        }

        LOG.debug("Transaction committed. Lock: {}", released);
        FluentFuture<? extends @NonNull CommitInfo> commit;
        try {
            commit = rwTx.commit();
        } catch (RuntimeException e) {
            // For example, the mount point was torn down. No callback will release the device, so release it here.
            LOG.error("Device transaction commit could not be started! Unlocking device.", e);
            afterClose();
            return FluentFutures.immediateFailedFluentFuture(e);
        }
        FluentFuture<? extends @NonNull CommitInfo> future =
                commit.withTimeout(timeout, timeUnit, scheduledExecutorService);

        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(CommitInfo result) {
                LOG.debug("Transaction with lock {} successfully committed: {}", released, result);
                afterClose();
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Device transaction commit failed or submit took longer than {} {}! Unlocking device.",
                    timeout, timeUnit, throwable);
                afterClose();
            }
        }, scheduledExecutorService);
        return future;
    }

    /**
     * Returns state of transaction.
     *
     * @return true if transaction was closed; otherwise false
     */
    public AtomicBoolean wasSubmittedOrCancelled() {
        return wasSubmittedOrCancelled;
    }

    private void afterClose() {
        scheduledExecutorService.shutdown();
        released.set(null);
    }
}
