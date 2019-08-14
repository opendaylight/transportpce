/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
            InstanceIdentifier<T> path) {
        return rwTx.read(store, path);
    }

    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        rwTx.put(store, path, data);
    }

    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        rwTx.put(store, path, data, createMissingParents);
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        rwTx.merge(store, path, data);
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data,
            boolean createMissingParents) {
        rwTx.merge(store, path, data, createMissingParents);
    }

    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
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
     * Submits data changed in transaction to device with defined timeout to submit. If time from timeout runs out then
     * submit will be interrupted and device will be unlocked.
     *
     * @param timeout a timeout
     * @param timeUnit a time unit
     * @return ListenableFuture which indicates when is submit completed.
     */
    public ListenableFuture<Void> submit(long timeout, TimeUnit timeUnit) {
        if (wasSubmittedOrCancelled.get()) {
            String msg = "Transaction was already submitted or canceled!";
            LOG.error(msg);
            return Futures.immediateFailedFuture(new IllegalStateException(msg));
        }

        LOG.debug("Transaction submitted. Lock: {}", deviceLock);
        wasSubmittedOrCancelled.set(true);
        ListenableFuture<Void> future =
                Futures.withTimeout(rwTx.submit(), timeout, timeUnit, scheduledExecutorService);

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Transaction with lock {} successfully submitted.", deviceLock);
                afterClose();
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Device transaction submit failed or submit took longer than {} {}!"
                        + " Unlocking device.", timeout, timeUnit, throwable);
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
