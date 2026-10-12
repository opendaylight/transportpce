/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A named executor together with the means to stop it.
 *
 * <p>Stopping is split in two steps, so that several pools can be shut down within one timeout. First call
 * {@link #stop()} on every pool. It returns immediately and runs no listeners, so every pool is stopped before
 * anything can block. Then cancel the returned tasks, and call {@link #awaitTermination(long, TimeUnit)} on every
 * pool, giving each only the time that is left until a shared deadline.
 *
 * <p>Cancelling a task runs the listeners of its future on the cancelling thread, and a listener added with
 * {@code directExecutor()} may block for as long as it likes, so cancel the tasks on other threads.
 * {@link DefaultExecutorPoolManager#close()} implements this.
 */
public interface Pool {

    /**
     * Returns the name of this pool, as used in log messages and as the prefix of its thread names.
     *
     * @return the pool name
     */
    String name();

    /**
     * Returns the executor that tasks are submitted to.
     *
     * @return the executor of this pool
     */
    ListeningExecutorService executorService();

    /**
     * Stops the pool without waiting for it, and returns the tasks that never ran without cancelling them.
     *
     * <p>Running tasks are interrupted. New submissions are rejected with
     * {@link java.util.concurrent.RejectedExecutionException}. Tasks still waiting in the queue are removed from it
     * and returned, still pending: callers waiting on their futures keep waiting until the tasks are cancelled.
     * Queued tasks that were passed to {@code execute(...)} have no future and are dropped.
     *
     * <p>This method returns immediately and runs no listeners. Use {@link #awaitTermination(long, TimeUnit)} to wait
     * until the pool's threads have finished. Calling this method more than once is harmless; later calls return an
     * empty list.
     *
     * @return the futures of the queued tasks that never ran, not yet cancelled
     */
    List<Future<?>> stop();

    /**
     * Waits until the pool's threads have finished, or the timeout expires.
     *
     * <p>This method only waits; it does not stop the pool. Call {@link #stop()} first.
     *
     * @param timeout the maximum time to wait; zero or a negative value returns immediately
     * @param unit the unit of {@code timeout}
     * @return {@code true} if the pool terminated within the timeout, {@code false} if it was still running
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
