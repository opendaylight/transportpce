/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NodeWorkTest {

    private final ForkJoinPool pool = new ForkJoinPool(1);

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void interruptedRequestWaitsForTheNodeInProgressAndSkipsTheOthers() throws Exception {
        AtomicBoolean stopped = new AtomicBoolean();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch firstMayEnd = new CountDownLatch(1);
        List<String> configured = new CopyOnWriteArrayList<>();
        ForkJoinTask<?> work = pool.submit(() -> List.of("node 1", "node 2").forEach(node -> {
            if (stopped.get()) {
                return;
            }
            if ("node 1".equals(node)) {
                firstStarted.countDown();
                Uninterruptibles.awaitUninterruptibly(firstMayEnd);
            }
            configured.add(node);
        }));
        CompletableFuture<Throwable> outcome = new CompletableFuture<>();
        Thread request = new Thread(() -> {
            try {
                NodeWork.await(work, stopped);
                outcome.complete(null);
            } catch (InterruptedException | ExecutionException e) {
                outcome.complete(e);
            }
        });
        request.start();
        assertTrue(firstStarted.await(10, TimeUnit.SECONDS), "node 1 was not started");

        // As the renderer does with a direction that does not finish in time
        request.interrupt();

        // Node 1 is still being configured
        assertThrows(TimeoutException.class, () -> outcome.get(200, TimeUnit.MILLISECONDS));
        firstMayEnd.countDown();
        assertInstanceOf(InterruptedException.class, outcome.get(10, TimeUnit.SECONDS));
        assertEquals(List.of("node 1"), configured);
    }
}
