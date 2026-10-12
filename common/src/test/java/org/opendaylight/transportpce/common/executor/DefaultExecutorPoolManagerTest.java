/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opendaylight.transportpce.common.executor.config.DefaultExecutorPoolConfig;
import org.opendaylight.transportpce.common.executor.config.ExecutorPoolConfig;
import org.opendaylight.transportpce.common.executor.config.PoolSize;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class DefaultExecutorPoolManagerTest {

    private static final ThreadPoolName POOL = ThreadPoolName.OLM_GET_PM;

    private final List<DefaultExecutorPoolManager> managers = new ArrayList<>();
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        release = new CountDownLatch(1);
    }

    @AfterEach
    void cleanup() {
        release.countDown();
        managers.forEach(DefaultExecutorPoolManager::close);
    }

    private DefaultExecutorPoolManager manager(ExecutorPoolConfig config) {
        DefaultExecutorPoolManager manager = new DefaultExecutorPoolManager(config);
        managers.add(manager);
        return manager;
    }

    private DefaultExecutorPoolManager manager(Duration shutdownTimeout, int threads, int queueSize) {
        return manager(new DefaultExecutorPoolConfig(shutdownTimeout, new PoolSize(threads, queueSize), Map.of()));
    }

    private DefaultExecutorPoolManager manager() {
        return manager(Duration.ofSeconds(5), 4, 64);
    }

    /**
     * Occupies all threads with tasks that wait for {@link #release}, and returns once they are all running.
     */
    private void occupyThreads(ListeningExecutorService executor, int threads) throws InterruptedException {
        CountDownLatch started = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                started.countDown();
                release.await();
                return null;
            });
        }
        started.await();
    }

    @Test
    void testThreadsAreNamedAfterThePool() throws Exception {
        ListeningExecutorService executor = manager().executorService(getClass(), POOL);

        String threadName = executor.submit(() -> Thread.currentThread().getName()).get();

        assertTrue(threadName.startsWith(POOL.poolName() + "-"), threadName);
    }

    @Test
    void testPoolSizeBoundsThePool() throws Exception {
        // The pool's own sizes take precedence over the defaults
        ExecutorPoolConfig config = new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(4, 64),
            Map.of(POOL, new PoolSize(1, 1)));
        ListeningExecutorService executor = manager(config).executorService(getClass(), POOL);
        occupyThreads(executor, 1);
        executor.submit(() -> null);

        // One thread busy and one task queued: the pool is full
        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> null));
    }

    @Test
    void testDefaultPoolSizeBoundsThePool() throws Exception {
        ListeningExecutorService executor = manager(Duration.ofSeconds(5), 1, 1).executorService(getClass(), POOL);
        occupyThreads(executor, 1);
        executor.submit(() -> null);

        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> null));
    }

    @Test
    void testInvalidConfigurationFallsBackToDefaults() throws Exception {
        // Without the fallback, zero threads would make pool creation fail
        ListeningExecutorService executor = manager(null, 0, -1).executorService(getClass(), POOL);

        assertEquals("done", executor.submit(() -> "done").get());
    }

    @Test
    void testEachCallReturnsANewPool() {
        DefaultExecutorPoolManager manager = manager();

        ListeningExecutorService first = manager.executorService(getClass(), POOL);
        ListeningExecutorService second = manager.executorService(getClass(), POOL);

        assertTrue(first != second);
    }

    @Test
    void testOwnerAndNameAreRequired() {
        DefaultExecutorPoolManager manager = manager();

        assertThrows(NullPointerException.class, () -> manager.executorService(null, POOL));
        assertThrows(NullPointerException.class, () -> manager.executorService(getClass(), null));
    }

    @Test
    void testCloseShutsDownEveryPool() {
        DefaultExecutorPoolManager manager = manager();
        ListeningExecutorService first = manager.executorService(getClass(), POOL);
        ListeningExecutorService second = manager.executorService(getClass(), POOL);

        manager.close();

        assertTrue(first.isTerminated());
        assertTrue(second.isTerminated());
    }

    @Test
    void testCloseCancelsQueuedTasks() throws Exception {
        DefaultExecutorPoolManager manager = manager(Duration.ofSeconds(5), 1, 1);
        ListeningExecutorService executor = manager.executorService(getClass(), POOL);
        occupyThreads(executor, 1);
        ListenableFuture<Object> queued = executor.submit(() -> null);

        manager.close();

        assertTrue(queued.isCancelled());
    }

    @Test
    void testCreatingAPoolAfterCloseFails() {
        DefaultExecutorPoolManager manager = manager();

        manager.close();

        assertThrows(IllegalStateException.class, () -> manager.executorService(getClass(), POOL));
    }

    @Test
    void testCloseTwiceIsHarmless() {
        DefaultExecutorPoolManager manager = manager();
        manager.executorService(getClass(), POOL);

        manager.close();

        assertDoesNotThrow(manager::close);
    }

    @Test
    void testCloseHandsEveryPoolToThePoolShutdown() {
        List<List<Pool>> shutdowns = new ArrayList<>();
        DefaultExecutorPoolManager manager = new DefaultExecutorPoolManager(
                new DefaultExecutorPoolConfig(Duration.ofSeconds(5), new PoolSize(4, 64), Map.of()),
                shutdowns::add);
        managers.add(manager);
        final List<ListeningExecutorService> created = List.of(
                manager.executorService(getClass(), POOL),
                manager.executorService(getClass(), POOL));

        manager.close();
        manager.close();

        assertEquals(1, shutdowns.size());
        assertEquals(created, shutdowns.get(0).stream().map(Pool::executorService).toList());
        // The PoolShutdown here does nothing, so stop the pools before the test ends
        shutdowns.get(0).forEach(Pool::stop);
    }
}
