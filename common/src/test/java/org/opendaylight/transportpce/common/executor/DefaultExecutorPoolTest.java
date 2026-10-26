/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class DefaultExecutorPoolTest {

    private ListeningExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        // One thread and room for one queued task, so a test can fill both
        executor = MoreExecutors.listeningDecorator(
                new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1)));
        release = new CountDownLatch(1);
    }

    @AfterEach
    void cleanup() {
        release.countDown();
        executor.shutdownNow();
    }

    @Test
    void testNameIsKept() {
        assertEquals("my-pool", new DefaultExecutorPool("my-pool", executor).name());
    }

    @Test
    void testMissingNameFallsBackToDefault() {
        assertEquals("executor-pool", new DefaultExecutorPool(null, executor).name());
        assertEquals("executor-pool", new DefaultExecutorPool("", executor).name());
    }

    @Test
    void testExecutorServiceIsRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultExecutorPool("my-pool", null));
    }

    @Test
    void testExecutorServiceIsExposed() {
        assertSame(executor, new DefaultExecutorPool("my-pool", executor).executorService());
    }

    @Test
    void testStopReturnsQueuedTasksWithoutCancellingThem() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        final ListenableFuture<Object> running = executor.submit(() -> {
            started.countDown();
            release.await();
            return null;
        });
        started.await();
        ListenableFuture<Object> queued = executor.submit(() -> null);

        List<Future<?>> stopped = new DefaultExecutorPool("my-pool", executor).stop();

        assertEquals(List.of(queued), stopped);
        assertFalse(queued.isDone());
        ExecutionException thrown = assertThrows(ExecutionException.class, running::get);
        assertInstanceOf(InterruptedException.class, thrown.getCause());
        assertTrue(executor.isShutdown());
    }

    @Test
    void testStopTwiceReturnsNothingTheSecondTime() {
        DefaultExecutorPool pool = new DefaultExecutorPool("my-pool", executor);
        pool.stop();

        assertTrue(pool.stop().isEmpty());
    }

    @Test
    void testSubmitAfterStopIsRejected() {
        new DefaultExecutorPool("my-pool", executor).stop();

        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> null));
    }

    @Test
    void testAwaitTerminationReturnsTrueWhenThePoolStops() throws Exception {
        DefaultExecutorPool pool = new DefaultExecutorPool("my-pool", executor);
        pool.stop();

        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
    }

    @Test
    void testAwaitTerminationReturnsFalseWhenATaskIgnoresTheInterrupt() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        executor.submit(() -> {
            started.countDown();
            Uninterruptibles.awaitUninterruptibly(release);
        });
        started.await();
        DefaultExecutorPool pool = new DefaultExecutorPool("my-pool", executor);
        pool.stop();

        assertFalse(pool.awaitTermination(100, TimeUnit.MILLISECONDS));

        release.countDown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testAwaitTerminationWithoutStopDoesNotStopThePool() throws Exception {
        DefaultExecutorPool pool = new DefaultExecutorPool("my-pool", executor);

        assertFalse(pool.awaitTermination(50, TimeUnit.MILLISECONDS));
        assertFalse(executor.isShutdown());
    }
}
