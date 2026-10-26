/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link PoolShutdown}, which waits for the pools up to a timeout and does not let blocking cancellation
 * listeners hold it up.
 */
public class TimedPoolShutdown implements PoolShutdown {

    private static final Logger LOG = LoggerFactory.getLogger(TimedPoolShutdown.class);

    static final int DEFAULT_MAX_CANCELLING_THREADS = 16;

    private final Duration timeout;

    private final int maxCancellingThreads;

    /**
     * Creates a shutdown that cancels queued tasks on up to {@value #DEFAULT_MAX_CANCELLING_THREADS} threads.
     *
     * @param timeout how long {@link #shutdown(List)} waits for the pools to terminate and their queued tasks to be
     *     cancelled
     */
    public TimedPoolShutdown(Duration timeout) {
        this(timeout, DEFAULT_MAX_CANCELLING_THREADS);
    }

    /**
     * Creates a shutdown.
     *
     * @param timeout how long {@link #shutdown(List)} waits for the pools to terminate and their queued tasks to be
     *     cancelled
     * @param maxCancellingThreads the maximum number of threads that cancel queued tasks at the same time
     * @throws IllegalArgumentException if {@code maxCancellingThreads} is not positive
     */
    public TimedPoolShutdown(Duration timeout, int maxCancellingThreads) {
        this.timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
        if (maxCancellingThreads < 1) {
            throw new IllegalArgumentException("maxCancellingThreads must be positive, was " + maxCancellingThreads);
        }
        this.maxCancellingThreads = maxCancellingThreads;
    }

    /**
     * Stops all pools, cancels queued tasks, and waits for termination within a shared timeout.
     *
     * <p>First every pool is stopped, interrupting running tasks, rejecting new submissions, and removing queued
     * tasks without invoking their listeners. The timeout starts after all pools have been stopped.
     *
     * <p>Queued tasks are then cancelled to release callers waiting on their futures. Cancellation listeners may
     * block, so cancellation runs on separate platform threads, up to the constructor's limit. This bounds thread
     * creation for long queues and avoids Java 21 virtual-thread carrier pinning in {@code synchronized} code.
     * If all cancellation threads block, further cancellations wait for one to become available.
     *
     * <p>Pool termination and cancellation share one deadline, regardless of the number of pools or tasks.
     * Unfinished work is logged and left to finish on its own; this method does not forcibly terminate it.
     *
     * <p>Shutting down pools that are already shut down is harmless.
     */
    @Override
    public void shutdown(List<Pool> pools) {
        if (pools.isEmpty()) {
            return;
        }

        List<String> names = pools.stream().map(Pool::name).toList();
        LOG.info("Shutting down executor(s) {}, waiting at most {} seconds for them to finish", names,
            timeout.toSeconds());

        // Phase 1: stop every pool. This runs no listeners, so every pool is stopped before a listener gets a chance
        // to block.
        List<Future<?>> queued = new ArrayList<>();
        for (Pool pool : pools) {
            queued.addAll(pool.stop());
        }

        // The timeout covers phases 2 and 3 only; phase 1 is quick and bounded
        long deadline = System.nanoTime() + timeout.toNanos();

        // Platform threads: on Java 21 a listener blocking inside synchronized would pin a virtual thread's carrier.
        // Daemon threads, so a listener that never returns does not keep the JVM alive.
        // Do not use try-with-resources: close() would wait for blocking cancellation listeners beyond our timeout.
        ExecutorService cancellers = Executors.newFixedThreadPool(
                Math.max(1, Math.min(queued.size(), maxCancellingThreads)),
                Thread.ofPlatform().daemon().name("executor-shutdown-", 0).factory());

        try {
            // Phase 2: cancel the queued tasks on several threads, so a blocking listener holds up only its own
            // thread
            if (!queued.isEmpty()) {
                LOG.info("Cancelling {} queued task(s) of executor(s) {}", queued.size(), names);
            }
            List<Future<?>> cancellations = new ArrayList<>(queued.size());
            for (Future<?> task : queued) {
                cancellations.add(cancellers.submit(() -> task.cancel(false)));
            }

            // Phase 3: wait for the pools' threads and the cancellations against the shared deadline
            List<String> running = new ArrayList<>();
            for (Pool pool : pools) {
                if (!pool.awaitTermination(remainingNanos(deadline), TimeUnit.NANOSECONDS)) {
                    running.add(pool.name());
                }
            }
            int pending = awaitCancellations(cancellations, deadline);

            if (running.isEmpty() && pending == 0) {
                LOG.info("Executor(s) {} shut down", names);
            } else {
                LOG.warn("Shutdown timeout of {} seconds expired for executor(s) {} with executor(s) {} still running "
                    + "and {} queued task(s) still being cancelled; leaving them to finish on their own",
                    timeout.toSeconds(), names, running, pending);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for executor(s) {} to terminate", names);
        } finally {
            // Lets cancellations still running finish on their own; interrupting them would interrupt listeners
            cancellers.shutdown();
        }
    }

    /**
     * Waits for the cancellations to complete, until the deadline.
     *
     * @return the number of cancellations still running at the deadline
     */
    private static int awaitCancellations(List<Future<?>> cancellations, long deadline)
            throws InterruptedException {
        int pending = 0;
        for (Future<?> cancellation : cancellations) {
            try {
                cancellation.get(remainingNanos(deadline), TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                pending++;
            } catch (ExecutionException e) {
                LOG.warn("Failed to cancel a queued task", e.getCause());
            }
        }
        return pending;
    }

    private static long remainingNanos(long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }
}
