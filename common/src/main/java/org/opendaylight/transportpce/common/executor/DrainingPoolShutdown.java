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
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PoolShutdown} that lets the pools finish their work: running tasks are not interrupted, and queued tasks
 * still run. New submissions are rejected.
 *
 * <p>For pools whose tasks must not be cut short, for example because a task changes several datastore entries that
 * belong together, and an interrupt would leave only some of them changed. The tasks should be short, since nothing
 * stops them.
 */
public class DrainingPoolShutdown implements PoolShutdown {

    private static final Logger LOG = LoggerFactory.getLogger(DrainingPoolShutdown.class);

    private final Duration timeout;

    /**
     * Creates a shutdown.
     *
     * @param timeout how long {@link #shutdown(List)} waits for the pools to finish their running and queued tasks
     */
    public DrainingPoolShutdown(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout cannot be null");
    }

    /**
     * Shuts every pool down, and waits for them to finish their running and queued tasks within a shared timeout.
     * Pools still running after it are logged and left to finish on their own.
     *
     * <p>Shutting down pools that are already shut down is harmless.
     */
    @Override
    public void shutdown(List<Pool> pools) {
        if (pools.isEmpty()) {
            return;
        }

        List<String> names = pools.stream().map(Pool::name).toList();
        LOG.info("Shutting down executor(s) {}, waiting at most {} seconds for their tasks to finish", names,
            timeout.toSeconds());

        for (Pool pool : pools) {
            pool.executorService().shutdown();
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            List<String> running = new ArrayList<>();
            for (Pool pool : pools) {
                if (!pool.awaitTermination(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                    running.add(pool.name());
                }
            }

            if (running.isEmpty()) {
                LOG.info("Executor(s) {} shut down", names);
            } else {
                LOG.warn("Shutdown timeout of {} seconds expired with executor(s) {} still running; leaving them to "
                    + "finish on their own", timeout.toSeconds(), running);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for executor(s) {} to terminate", names);
        }
    }
}
