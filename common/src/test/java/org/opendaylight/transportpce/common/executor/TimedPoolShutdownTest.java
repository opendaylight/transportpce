/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class TimedPoolShutdownTest {

    private final List<Pool> pools = new ArrayList<>();
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        release = new CountDownLatch(1);
    }

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

    /**
     * Occupies all threads with tasks that ignore interrupts until {@link #release}, so the pool cannot terminate
     * before the test releases them.
     */
    private void occupyThreadsUninterruptibly(ListeningExecutorService executor, int threads)
            throws InterruptedException {
        CountDownLatch started = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                started.countDown();
                Uninterruptibles.awaitUninterruptibly(release);
            });
        }
        started.await();
    }

    @Test
    void testShutdownStopsEveryPool() {
        Pool first = pool(4, 64);
        Pool second = pool(4, 64);

        new TimedPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(first, second));

        assertTrue(first.executorService().isTerminated());
        assertTrue(second.executorService().isTerminated());
    }

    @Test
    void testShutdownCancelsQueuedTasks() throws Exception {
        Pool pool = pool(1, 1);
        occupyThreads(pool.executorService(), 1);
        ListenableFuture<Object> queued = pool.executorService().submit(() -> null);

        new TimedPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(pool));

        assertTrue(queued.isCancelled());
    }

    @Test
    void testShutdownTwiceIsHarmless() {
        Pool pool = pool(4, 64);
        PoolShutdown shutdown = new TimedPoolShutdown(Duration.ofSeconds(5));

        shutdown.shutdown(List.of(pool));

        assertDoesNotThrow(() -> shutdown.shutdown(List.of(pool)));
    }

    @Test
    void testShutdownWaitsForAllPoolsAgainstOneSharedDeadline() throws Exception {
        // Three pools that cannot stop until released; waiting for each in turn would take three timeouts
        Duration timeout = Duration.ofMillis(300);
        List<Pool> stuck = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Pool pool = pool(1, 1);
            occupyThreadsUninterruptibly(pool.executorService(), 1);
            stuck.add(pool);
        }

        long start = System.nanoTime();
        new TimedPoolShutdown(timeout).shutdown(stuck);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertTrue(elapsed.compareTo(timeout.multipliedBy(2)) < 0, "shutdown() took " + elapsed);
    }

    @Test
    void testShutdownIsNotHeldUpByABlockingCancellationListener() throws Exception {
        Duration timeout = Duration.ofMillis(300);
        Pool first = pool(1, 1);
        occupyThreads(first.executorService(), 1);
        ListenableFuture<Object> queued = first.executorService().submit(() -> null);
        // Runs on the cancelling thread and blocks until the test ends. The wait is bounded so that, if shutdown()
        // ran it on its own thread, the test fails on the elapsed time instead of hanging past @Timeout.
        queued.addListener(
                () -> Uninterruptibles.awaitUninterruptibly(release, 5, TimeUnit.SECONDS),
                MoreExecutors.directExecutor());
        Pool second = pool(1, 1);

        long start = System.nanoTime();
        new TimedPoolShutdown(timeout).shutdown(List.of(first, second));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertTrue(elapsed.compareTo(timeout.multipliedBy(2)) < 0, "shutdown() took " + elapsed);
        assertTrue(queued.isCancelled());
        assertTrue(first.executorService().isTerminated());
        assertTrue(second.executorService().isTerminated());
    }

    @Test
    void testShutdownWaitsForCancellationListeners() throws Exception {
        Pool pool = pool(1, 1);
        occupyThreads(pool.executorService(), 1);
        ListenableFuture<Object> queued = pool.executorService().submit(() -> null);
        AtomicBoolean listenerDone = new AtomicBoolean();
        queued.addListener(() -> {
            Uninterruptibles.sleepUninterruptibly(Duration.ofMillis(100));
            listenerDone.set(true);
        }, MoreExecutors.directExecutor());

        new TimedPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(pool));

        assertTrue(listenerDone.get());
    }

    @Test
    void testShutdownStopsEveryPoolBeforeCancellingQueuedTasks() throws Exception {
        Pool first = pool(1, 1);
        occupyThreads(first.executorService(), 1);
        ListenableFuture<Object> queued = first.executorService().submit(() -> null);
        Pool second = pool(1, 1);
        AtomicBoolean secondStopped = new AtomicBoolean();
        queued.addListener(() -> secondStopped.set(second.executorService().isShutdown()),
                MoreExecutors.directExecutor());

        new TimedPoolShutdown(Duration.ofSeconds(5)).shutdown(List.of(first, second));

        assertTrue(secondStopped.get());
    }

    @Test
    void testShutdownIsNotHeldUpByListenersBlockingInsideSynchronized() throws Exception {
        // On Java 21, a virtual thread blocking inside synchronized pins its carrier thread. Enough such listeners
        // would occupy every carrier, so cancellations running on virtual threads could not reach the last task.
        // There are enough cancelling threads for every blocking listener and the last task.
        int blocking = Runtime.getRuntime().availableProcessors();
        Pool pool = pool(1, blocking + 1);
        ListeningExecutorService executor = pool.executorService();
        occupyThreads(executor, 1);
        for (int i = 0; i < blocking; i++) {
            Object lock = new Object();
            executor.submit(() -> null).addListener(() -> {
                synchronized (lock) {
                    Uninterruptibles.awaitUninterruptibly(release, 5, TimeUnit.SECONDS);
                }
            }, MoreExecutors.directExecutor());
        }
        ListenableFuture<Object> last = executor.submit(() -> null);

        new TimedPoolShutdown(Duration.ofMillis(300), blocking + 1).shutdown(List.of(pool));

        assertTrue(last.isCancelled());
    }

    @Test
    void testShutdownCancelsManyQueuedTasksOnFewThreads() throws Exception {
        // A pool with a long queue, such as the renderer's, must not start a thread for each queued task
        int queuedTasks = 200;
        int maxCancellingThreads = 4;
        Pool pool = pool(1, queuedTasks);
        occupyThreads(pool.executorService(), 1);
        Set<Thread> cancellingThreads = ConcurrentHashMap.newKeySet();
        List<ListenableFuture<Object>> queued = new ArrayList<>();
        for (int i = 0; i < queuedTasks; i++) {
            ListenableFuture<Object> task = pool.executorService().submit(() -> null);
            task.addListener(() -> cancellingThreads.add(Thread.currentThread()), MoreExecutors.directExecutor());
            queued.add(task);
        }

        new TimedPoolShutdown(Duration.ofSeconds(5), maxCancellingThreads).shutdown(List.of(pool));

        assertTrue(queued.stream().allMatch(ListenableFuture::isCancelled));
        assertTrue(cancellingThreads.size() <= maxCancellingThreads,
            "cancelled on " + cancellingThreads.size() + " threads");
    }

    @Test
    void testMaxCancellingThreadsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new TimedPoolShutdown(Duration.ofSeconds(5), 0));
    }
}
