/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.pool.shutdown;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opendaylight.transportpce.common.executor.pool.DefaultExecutorPool;
import org.opendaylight.transportpce.common.executor.pool.Pool;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class DrainingPoolShutdownTest {

    private final List<Pool> pools = new ArrayList<>();
    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void cleanup() {
        release.countDown();
        pools.forEach(Pool::stop);
    }

    private Pool pool(int threads, int queueSize) {
        Pool pool = new DefaultExecutorPool("test-pool", MoreExecutors.listeningDecorator(
                SpecialExecutors.newBoundedFastThreadPool(threads, queueSize, "test-pool", getClass())));
        pools.add(pool);
        return pool;
    }

    @Test
    void testShutdownStopsEveryPool() {
        Pool first = pool(4, 64);
        Pool second = pool(4, 64);

        new DrainingPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(first, second));

        assertTrue(first.executorService().isTerminated());
        assertTrue(second.executorService().isTerminated());
    }

    @Test
    void testShutdownLetsRunningTasksFinishWithoutInterruption() throws Exception {
        Pool pool = pool(1, 1);
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        ListenableFuture<?> running = pool.executorService().submit(() -> {
            started.countDown();
            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        started.await();

        new DrainingPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(pool));

        assertTrue(running.isDone());
        assertFalse(running.isCancelled());
        assertFalse(interrupted.get());
    }

    @Test
    void testShutdownRunsQueuedTasks() throws Exception {
        Pool pool = pool(1, 1);
        CountDownLatch started = new CountDownLatch(1);
        pool.executorService().submit(() -> {
            started.countDown();
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        });
        started.await();
        AtomicBoolean ran = new AtomicBoolean();
        pool.executorService().submit(() -> ran.set(true));

        new DrainingPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(pool));

        assertTrue(ran.get());
    }

    @Test
    void testShutdownRejectsNewTasks() {
        Pool pool = pool(1, 1);

        new DrainingPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(pool));

        assertThrows(RejectedExecutionException.class, () -> pool.executorService().execute(() -> { }));
    }

    @Test
    void testShutdownTwiceIsHarmless() {
        Pool pool = pool(4, 64);
        PoolShutdown shutdown = new DrainingPoolShutdown(Duration.ofSeconds(5));

        shutdown.shutdown(List.of(pool));

        assertDoesNotThrow(() -> shutdown.shutdown(List.of(pool)));
    }

    @Test
    void testShutdownWaitsForAllPoolsAgainstOneSharedDeadline() throws Exception {
        // Three pools that cannot finish until released; waiting for each in turn would take three timeouts
        Duration timeout = Duration.ofMillis(300);
        List<Pool> stuck = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Pool pool = pool(1, 1);
            CountDownLatch started = new CountDownLatch(1);
            pool.executorService().execute(() -> {
                started.countDown();
                Uninterruptibles.awaitUninterruptibly(release);
            });
            started.await();
            stuck.add(pool);
        }

        long start = System.nanoTime();
        new DrainingPoolShutdown(timeout).shutdown(stuck);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertTrue(elapsed.compareTo(timeout.multipliedBy(2)) < 0, "shutdown() took " + elapsed);
        assertFalse(stuck.get(0).executorService().isTerminated());
    }
}
