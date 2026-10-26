/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.common.executor.pool.ThreadPoolName;
import org.opendaylight.transportpce.common.executor.pool.config.PoolSize;

/**
 * Creates and owns bounded executors used to run blocking work, such as device operations, off the caller's thread.
 *
 * <p>Typical use is an RPC implementation that submits its work to an executor and returns the resulting
 * {@link com.google.common.util.concurrent.ListenableFuture}, instead of blocking the RESTCONF request thread.
 *
 * <p>Each component creates its own manager and closes it when the component stops. Closing the manager shuts down
 * every executor it has created, so the component does not need to keep track of them.
 */
public interface ExecutorPoolManager extends AutoCloseable {

    /**
     * Creates a new bounded executor for the given pool.
     *
     * <p>The thread count and queue size come from the pool's configuration, so an operator can change them without
     * changing code. Each call returns a new, independent executor. It is owned by this manager, which shuts it down
     * when the manager is closed; callers do not need to shut it down themselves.
     *
     * <p>When all threads are busy and the queue is full, or after the executor has been shut down,
     * {@code submit()} throws {@link RejectedExecutionException}. Callers should handle it, for example by returning
     * an RPC error, rather than letting it propagate.
     *
     * @param owner the class that owns the pool; uncaught exceptions from the pool's threads are logged under
     *     this class's logger
     * @param name the pool, whose name selects its configured sizes and is used as prefix for the names of its
     *     threads, which are named {@code <name>-<n>}
     * @return a new executor
     * @throws IllegalStateException if this manager has been closed
     */
    ListeningExecutorService executorService(@NonNull Class<?> owner, @NonNull ThreadPoolName name);

    /**
     * Creates a new executor with the given sizes, for a pool whose sizes do not come straight from the
     * configuration, for example because they are derived from another pool's.
     *
     * <p>Otherwise the same as {@link #executorService(Class, ThreadPoolName)}.
     *
     * @param owner the class that owns the pool; uncaught exceptions from the pool's threads are logged under
     *     this class's logger
     * @param name the prefix for the names of the pool's threads, which are named {@code <name>-<n>}
     * @param size the thread count and queue size of the pool
     * @return a new executor
     * @throws IllegalStateException if this manager has been closed
     */
    ListeningExecutorService executorService(@NonNull Class<?> owner, @NonNull String name, @NonNull PoolSize size);

    /**
     * Closes this manager and shuts down every executor it has created.
     *
     * <p>What happens to running and queued tasks is up to the implementation. After this method has been called,
     * {@link #executorService(Class, ThreadPoolName)} throws {@link IllegalStateException}. Calling this method
     * more than once is harmless.
     */
    @Override
    void close();
}
