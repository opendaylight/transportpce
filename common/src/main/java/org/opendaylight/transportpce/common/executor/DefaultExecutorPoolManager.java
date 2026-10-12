/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.common.executor.config.ExecutorPoolConfig;
import org.opendaylight.transportpce.common.executor.config.PoolSize;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

/**
 * Default {@link ExecutorPoolManager}, which sizes its pools from an {@link ExecutorPoolConfig}.
 */
public class DefaultExecutorPoolManager implements ExecutorPoolManager {

    // guarded by itself: only access inside synchronized (executors)
    private final List<Pool> executors = new ArrayList<>();

    // guarded by executors
    private boolean closed;

    private final ExecutorPoolConfig config;

    private final PoolShutdown poolShutdown;

    /**
     * Creates a manager that shuts down its pools with a {@link DefaultPoolShutdown}, using the configured shutdown
     * timeout.
     *
     * @param config the sizes of the pools and the shutdown timeout
     */
    public DefaultExecutorPoolManager(ExecutorPoolConfig config) {
        this(config, new DefaultPoolShutdown(
                Objects.requireNonNull(config, "config cannot be null").shutDownTimeout()));
    }

    /**
     * Creates a manager.
     *
     * @param config the sizes of the pools
     * @param poolShutdown shuts down the pools when this manager is closed
     */
    public DefaultExecutorPoolManager(ExecutorPoolConfig config, PoolShutdown poolShutdown) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.poolShutdown = Objects.requireNonNull(poolShutdown, "poolShutdown cannot be null");
    }

    @Override
    public ListeningExecutorService executorService(@NonNull Class<?> owner, @NonNull ThreadPoolName name) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        PoolSize size = config.poolSize(name);

        synchronized (executors) {
            if (closed) {
                throw new IllegalStateException("ExecutorPoolManager has been closed");
            }

            ListeningExecutorService service = MoreExecutors.listeningDecorator(
                    SpecialExecutors.newBoundedFastThreadPool(
                            size.threads(),
                            size.queueSize(),
                            name.poolName(),
                            owner));

            executors.add(new DefaultExecutorPool(name.poolName(), service));

            return service;
        }
    }

    /**
     * Closes this manager and shuts down every executor it has created with its {@link PoolShutdown}.
     *
     * <p>After this method has been called, {@link #executorService(Class, ThreadPoolName)} throws
     * {@link IllegalStateException}.
     *
     * <p>Calling this method more than once is harmless.
     */
    @Override
    public void close() {
        List<Pool> toClose;

        synchronized (executors) {
            closed = true;
            toClose = List.copyOf(executors);
            executors.clear();
        }

        if (!toClose.isEmpty()) {
            poolShutdown.shutdown(toClose);
        }
    }
}
