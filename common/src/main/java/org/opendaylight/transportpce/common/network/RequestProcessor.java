/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.network;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "UL_UNRELEASED_LOCK_EXCEPTION_PATH",
    justification = "This appears to be doing exactly the right thing with the finally-clause to release the lock")
public class RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessor.class);

    private final DataBroker dataBroker;
    private final ReentrantReadWriteLock rwL = new ReentrantReadWriteLock();
    private final Lock readL = rwL.readLock();
    private final Lock writeL = rwL.writeLock();
    private Map<String, WriteTransaction> txMap = new HashMap<>();
    private Map<String, ReadTransaction> rxMap = new HashMap<>();


    public RequestProcessor(DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
        LOG.info("RequestProcessor instantiated");
    }

    public <T extends DataObject> ListenableFuture<Optional<T>> read(LogicalDatastoreType store,
            InstanceIdentifier<T> path) {
        LOG.debug("read - store, thread = {} - {}", store, Thread.currentThread().getName());
        readL.lock();
        LOG.debug("read locked by {}", Thread.currentThread().getName());
        try {
            if (!rxMap.containsKey(Thread.currentThread().getName())) {
                rxMap.put(Thread.currentThread().getName(), dataBroker.newReadOnlyTransaction());
            }
            return rxMap.get(Thread.currentThread().getName()).read(store, path);
        }
        finally {
            LOG.debug("read before unlock - {}", Thread.currentThread().getName());
            rxMap.remove(Thread.currentThread().getName());
            readL.unlock();
            LOG.debug("delete after unlock - {}", Thread.currentThread().getName());
        }
    }

    public <T extends DataObject> void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        LOG.debug("delete - store, thread = {} - {}", store, Thread.currentThread().getName());
        writeL.lock();
        LOG.debug("delete locked by {}", Thread.currentThread().getName());
        try {
            if (!txMap.containsKey(Thread.currentThread().getName())) {
                txMap.put(Thread.currentThread().getName(), dataBroker.newWriteOnlyTransaction());
            }
            txMap.get(Thread.currentThread().getName()).delete(store, path);
        }
        finally {
            LOG.debug("delete before unlock - {}", Thread.currentThread().getName());
            writeL.unlock();
            LOG.debug("delete after unlock - {}", Thread.currentThread().getName());
        }
    }

    public <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        LOG.debug("put - store, thread = {} - {}", store, Thread.currentThread().getName());
        writeL.lock();
        LOG.debug("put locked by {}", Thread.currentThread().getName());
        try {
            if (!txMap.containsKey(Thread.currentThread().getName())) {
                txMap.put(Thread.currentThread().getName(), dataBroker.newWriteOnlyTransaction());
            }
            txMap.get(Thread.currentThread().getName()).put(store, path, data);
        }
        finally {
            LOG.debug("put before unlock - {}", Thread.currentThread().getName());
            writeL.unlock();
            LOG.debug("put after unlock - {}",Thread.currentThread().getName());
        }
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
        LOG.debug("merge - store, thread = {} - {}", store, Thread.currentThread().getName());
        writeL.lock();
        LOG.debug("merge locked by {}", Thread.currentThread().getName());
        try {
            if (!txMap.containsKey(Thread.currentThread().getName())) {
                txMap.put(Thread.currentThread().getName(), dataBroker.newWriteOnlyTransaction());
            }
            txMap.get(Thread.currentThread().getName()).merge(store, path, data);
        }
        finally {
            LOG.debug("merge before unlock - {}", Thread.currentThread().getName());
            writeL.unlock();
            LOG.debug("merge after unlock - {}", Thread.currentThread().getName());
        }
    }

    public FluentFuture<? extends @NonNull CommitInfo> commit() {
        LOG.debug("commit - thread = {}", Thread.currentThread().getName());
        writeL.lock();
        LOG.debug("commit locked by {}", Thread.currentThread().getName());
        try {
            if (txMap.containsKey(Thread.currentThread().getName())) {
                return txMap.get(Thread.currentThread().getName()).commit();
            } else {
                LOG.warn("No write transaction available for thread {}", Thread.currentThread().getName());
                return FluentFutures.immediateCancelledFluentFuture();
            }
        }
        finally {
            LOG.debug("commit before unlock - {}", Thread.currentThread().getName());
            txMap.remove(Thread.currentThread().getName());
            writeL.unlock();
            LOG.debug("commit after unlock - {}", Thread.currentThread().getName());
        }
    }

    public void close() {
        LOG.info("closing RequestProcessor Locks by {}", Thread.currentThread().getName());
        txMap.remove(Thread.currentThread().getName());
        rxMap.remove(Thread.currentThread().getName());
        readL.unlock();
        writeL.unlock();
    }

    /**
     * Return the dataBroker related to RequestProcessor.
     * @return the dataBroker
     */
    public DataBroker getDataBroker() {
        return dataBroker;
    }
}
