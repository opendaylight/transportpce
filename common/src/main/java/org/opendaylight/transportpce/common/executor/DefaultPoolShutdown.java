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
public class DefaultPoolShutdown implements PoolShutdown {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPoolShutdown.class);

    private final Duration timeout;

    /**
     * Creates a shutdown.
     *
     * @param timeout how long {@link #shutdown(List)} waits for the pools to terminate and their queued tasks to be
     *     cancelled
     */
    public DefaultPoolShutdown(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
    }

    /**
     * Shuts down the pools.
     *
     * <p>The pools accept caller-supplied tasks and do not require listeners attached to their futures to be
     * non-blocking. Shutdown therefore accounts for blocking cancellation listeners, even if the current callers
     * do not use them. The separate shutdown phases and cancellation threads below prevent such a listener from
     * delaying shutdown of other pools or cancellation of other queued tasks. Running tasks and listeners that do
     * not finish within the timeout may continue after this method returns; shutdown does not forcibly terminate
     * them.
     *
     * <p>Shutdown happens in three phases. First every pool is stopped: running tasks are interrupted, new
     * submissions are rejected, and tasks still waiting in a queue are removed from it. This runs no listeners, so
     * nothing can block until every pool has stopped, and the timeout starts after it.
     *
     * <p>Then the removed tasks are cancelled, so callers waiting on their futures are not left hanging. Cancelling
     * runs the futures' listeners, and a listener added with {@code directExecutor()} may block, so each task is
     * cancelled on its own platform thread and a blocking listener only holds up its own task. Platform threads are
     * used rather than virtual threads because, on Java 21, a virtual thread blocking inside {@code synchronized}
     * pins its carrier thread: enough such listeners would occupy every carrier, stalling the remaining
     * cancellations and every other virtual thread in the JVM. The number of threads is bounded by the number of
     * queued tasks.
     *
     * <p>Finally this method waits for the pools' threads to finish and for the cancellations to complete, sharing
     * a single deadline of the timeout, so the total wait does not grow with the number of pools or tasks. Pools and
     * cancellations that have not finished by then are logged and left to finish on their own.
     *
     * <p>Shutting down pools that are already shut down is harmless.
     */
    @Override
    public void shutdown(List<Pool> pools) {
        if (pools.isEmpty()) {
            return;
        }

        LOG.info("Shutting down {} executors", pools.size());

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
        ExecutorService cancellers = Executors.newThreadPerTaskExecutor(
                Thread.ofPlatform().daemon().name("executor-shutdown-", 0).factory());

        try {
            // Phase 2: cancel the queued tasks, each on its own thread, so a blocking listener holds up only its own
            // task
            if (!queued.isEmpty()) {
                LOG.info("Cancelling {} queued tasks", queued.size());
            }
            List<Future<?>> cancellations = new ArrayList<>(queued.size());
            for (Future<?> task : queued) {
                cancellations.add(cancellers.submit(() -> task.cancel(false)));
            }

            // Phase 3: wait for the pools' threads and the cancellations against the shared deadline
            int running = 0;
            for (Pool pool : pools) {
                if (!pool.awaitTermination(remainingNanos(deadline), TimeUnit.NANOSECONDS)) {
                    running++;
                }
            }
            int pending = awaitCancellations(cancellations, deadline);

            if (running == 0 && pending == 0) {
                LOG.info("All {} executors shut down", pools.size());
            } else {
                LOG.warn("Shutdown timeout of {} expired with {} of {} executors still running and {} queued tasks "
                    + "still being cancelled; leaving them to finish on their own",
                    timeout, running, pools.size(), pending);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for executors to terminate");
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
