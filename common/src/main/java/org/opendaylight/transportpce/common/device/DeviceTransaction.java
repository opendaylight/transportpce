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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
 * <p>
 * WARNING: Only one transaction can be opened at the same time on device!
 * It's important to close (cancel/submit) transaction when work is done with it
 * (so others can access the device).
 * </p>
 */
public class DeviceTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTransaction.class);

    private final ReadWriteTransaction rwTx;
    private final CountDownLatch deviceLock;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean wasSubmittedOrCancelled = new AtomicBoolean(false);

    DeviceTransaction(ReadWriteTransaction rwTx, CountDownLatch deviceLock) {
        this.rwTx = rwTx;
        this.deviceLock = deviceLock;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        LOG.debug("Device transaction created. Lock: {}", deviceLock);
    }

    public <T extends DataObject> ListenableFuture<Optional<T>> read(LogicalDatastoreType store,
            DataObjectIdentifier<T> path) {
        return rwTx.read(store, path);
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
     * @return true if cancel was successful.
     */
    public boolean cancel() {
        if (wasSubmittedOrCancelled.get()) {
            LOG.warn("Transaction was already submitted or canceled!");
            return false;
        }

        LOG.debug("Transaction cancelled. Lock: {}", deviceLock);
        wasSubmittedOrCancelled.set(true);
        afterClose();
        return rwTx.cancel();
    }

    /**
     * Submits data changed in transaction to device with defined timeout to commit. If time from timeout runs out then
     * the commit will be interrupted and the device will be unlocked.
     *
     * @param timeout a timeout
     * @param timeUnit a time unit
     * @return FluentFuture which indicates when the commit is completed.
     */
    public FluentFuture<? extends @NonNull CommitInfo> commit(long timeout, TimeUnit timeUnit) {
        if (wasSubmittedOrCancelled.get()) {
            String msg = "Transaction was already submitted or canceled!";
            LOG.error(msg);
            return FluentFutures.immediateFailedFluentFuture(new IllegalStateException(msg));
        }

        LOG.debug("Transaction committed. Lock: {}", deviceLock);
        wasSubmittedOrCancelled.set(true);
        FluentFuture<? extends @NonNull CommitInfo> future =
                rwTx.commit().withTimeout(timeout, timeUnit, scheduledExecutorService);

        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(CommitInfo result) {
                LOG.debug("Transaction with lock {} successfully committed: {}", deviceLock, result);
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
     * @return true if transaction was closed; otherwise false
     */
    public AtomicBoolean wasSubmittedOrCancelled() {
        return wasSubmittedOrCancelled;
    }

    private void afterClose() {
        scheduledExecutorService.shutdown();
        deviceLock.countDown();
    }
}
