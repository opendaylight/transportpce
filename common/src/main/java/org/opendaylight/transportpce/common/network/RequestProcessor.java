/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.network;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessor.class);

    private final DataBroker dataBroker;
    private ReadWriteTransaction rwTx;
    private ReadTransaction readTx;
    private ReentrantReadWriteLock lock;



    public RequestProcessor(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        rwTx = dataBroker.newReadWriteTransaction();
        readTx = dataBroker.newReadOnlyTransaction();
        lock = new ReentrantReadWriteLock();
        LOG.info("RequestProcessor instantiated");

    }

    public <T extends DataObject> ListenableFuture<Optional<T>>
         read(LogicalDatastoreType store,InstanceIdentifier<T> path) {

        ListenableFuture<Optional<T>> result = null;
        acquireReadLock();
        LOG.debug("Number of threads in queue to read {}", lock.getQueueLength());
        result = rwTx.read(store, path);

        releaseReadLock();
        return result;
    }

    public <T extends DataObject> void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {

        acquireLock();
        LOG.info("Number of delete requests waiting in queue :{}", lock.getQueueLength());
        rwTx.delete(store, path);
    }


    public <T extends DataObject> void put(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data) {

        acquireLock();
        LOG.debug("Number of put requests waiting in queue :{}", lock.getQueueLength());
        rwTx.put(store, path, data);
    }


    public <T extends DataObject> void merge(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data) {

        acquireLock();
        LOG.debug("Number of merge requests waiting in queue :{}", lock.getQueueLength());
        rwTx.merge(store, path, data);
    }

    public FluentFuture<? extends @NonNull CommitInfo> commit() {
        acquireLock();
        FluentFuture<? extends @NonNull CommitInfo> future = null;
        future = rwTx.commit();
        releaseLock();
        resetRwTx();
        return future;
    }

    public void close() {
        releaseLock();
    }

    private void acquireLock() {
        if (!lock.writeLock().isHeldByCurrentThread()) {
            lock.writeLock().lock();
            LOG.debug("Number of write lock requests waiting in queue :{}", lock.getQueueLength());
            LOG.info("Write Lock acquired by : {}", Thread.currentThread().getName());
            rwTx = resetRwTx();
        } else {
            LOG.debug("Lock already acquired by : {}", Thread.currentThread().getName());
        }
    }

    private void acquireReadLock() {
        if (lock.getReadHoldCount() > 0) {
            LOG.info("Read Lock already acquired by : {}", Thread.currentThread().getName());
        } else {
            lock.readLock().lock();
            rwTx = resetRwTx();
            LOG.info("Read Lock acquired by : {}", Thread.currentThread().getName());
        }
    }

    private void releaseLock() {
        if (lock.writeLock().isHeldByCurrentThread()) {
            LOG.info("Write Lock released by : {}", Thread.currentThread().getName());
            lock.writeLock().unlock();
        }
    }

    private void releaseReadLock() {
        LOG.info("Read Lock released by : {}", Thread.currentThread().getName());
        lock.readLock().unlock();
    }

    private ReadWriteTransaction resetRwTx() {
        LOG.info("Resetting the read write transaction .....");
        rwTx = dataBroker.newReadWriteTransaction();
        return rwTx;
    }
}
