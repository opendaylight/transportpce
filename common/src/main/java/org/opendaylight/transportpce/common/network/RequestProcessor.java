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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "UL_UNRELEASED_LOCK_EXCEPTION_PATH",
    justification = "This appears to be doing exactly the right thing with the finally-clause to release the lock")
public class RequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessor.class);

    private final DataBroker dataBroker;
    private final ReentrantReadWriteLock rwL = new ReentrantReadWriteLock();
    private final Lock readL = rwL.readLock();
    private final Lock writeL = rwL.writeLock();
    private Map<String, WriteTransaction> writeTrMap = new HashMap<>();


    public RequestProcessor(DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
        LOG.info("RequestProcessor instantiated");
    }

    public <T extends DataObject> ListenableFuture<Optional<T>> read(LogicalDatastoreType store,
            DataObjectIdentifier<T> path) {
        ReadTransaction readTx = dataBroker.newReadOnlyTransaction();
        String thread = Thread.currentThread().getName();
        readL.lock();
        LOG.debug("read locked {} by {}", store, thread);
        try {
            return readTx.read(store, path);
        }
        finally {
            readTx.close();
            readL.unlock();
            LOG.debug("read after unlock - {}", thread);
        }
    }

    public <T extends DataObject> void delete(LogicalDatastoreType store, DataObjectIdentifier<?> path) {
        String thread = Thread.currentThread().getName();
        LOG.debug("delete - store, thread = {} - {}", store, thread);
        writeL.lock();
        LOG.debug("delete locked by {}", thread);
        try {
            if (!writeTrMap.containsKey(thread)) {
                writeTrMap.put(thread, dataBroker.newWriteOnlyTransaction());
            }
            writeTrMap.get(thread).delete(store, path);
        }
        finally {
            LOG.debug("delete before unlock - {}", thread);
            writeL.unlock();
            LOG.debug("delete after unlock1 - {}", Thread.currentThread().getName());
            LOG.debug("delete after unlock2 - {}", thread);
        }
    }

    public <T extends DataObject> void put(LogicalDatastoreType store, DataObjectIdentifier<T> path, T data) {
        String thread = Thread.currentThread().getName();
        writeL.lock();
        LOG.debug("put locked {} by {}", store, thread);
        try {
            if (!writeTrMap.containsKey(thread)) {
                writeTrMap.put(thread, dataBroker.newWriteOnlyTransaction());
            }
            writeTrMap.get(thread).put(store, path, data);
        }
        finally {
            writeL.unlock();
            LOG.debug("put after unlock - {}", thread);
        }
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store, DataObjectIdentifier<T> path, T data) {
        String thread = Thread.currentThread().getName();
        writeL.lock();
        LOG.debug("merge locked {} by {}", store, thread);
        try {
            if (!writeTrMap.containsKey(thread)) {
                writeTrMap.put(thread, dataBroker.newWriteOnlyTransaction());
            }
            writeTrMap.get(thread).merge(store, path, data);
        }
        finally {
            writeL.unlock();
            LOG.debug("merge after unlock - {}", thread);
        }
    }

    public FluentFuture<? extends @NonNull CommitInfo> commit() {
        String thread = Thread.currentThread().getName();
        writeL.lock();
        LOG.debug("commit locked by {}", thread);
        try {
            if (writeTrMap.containsKey(thread)) {
                return writeTrMap.get(thread).commit();
            } else {
                LOG.warn("No write transaction available for thread {}", thread);
                return FluentFutures.immediateNullFluentFuture();
            }
        }
        finally {
            writeTrMap.remove(thread);
            writeL.unlock();
            LOG.debug("commit after unlock - {}", thread);
        }
    }

    /**
     * Return the dataBroker related to RequestProcessor.
     * @return the dataBroker
     */
    public DataBroker getDataBroker() {
        return dataBroker;
    }
}
