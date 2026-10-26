/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NodeLocksTest {

    private static final String NODE = "ROADM-A1";

    private final NodeLocks nodeLocks = new NodeLocks();

    @Test
    void runLockedRemovesTheLockOnceItIsNoLongerUsed() throws Exception {
        nodeLocks.runLocked(NODE, () -> assertEquals(1, nodeLocks.size()));

        assertEquals(0, nodeLocks.size());
    }

    @Test
    void runLockedRemovesTheLockWhenTheOperationFails() {
        assertThrows(OpenRoadmInterfaceException.class, () -> nodeLocks.runLocked(NODE, () -> {
            throw new OpenRoadmInterfaceException("Failed");
        }));

        assertEquals(0, nodeLocks.size());
    }

    @Test
    void runLockedCanBeInterruptedWhileWaitingForTheLock() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            try {
                nodeLocks.runLocked(NODE, () -> {
                    holding.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (OpenRoadmInterfaceException e) {
                throw new IllegalStateException(e);
            }
        });
        holder.start();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicBoolean ran = new AtomicBoolean();
        AtomicBoolean interruptStatus = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                nodeLocks.runLocked(NODE, () -> ran.set(true));
            } catch (OpenRoadmInterfaceException e) {
                failure.set(e);
            }
            interruptStatus.set(Thread.currentThread().isInterrupted());
        });
        try {
            assertTrue(holding.await(10, TimeUnit.SECONDS));
            waiter.start();
            // As when the waiter's pool shuts down while a node does not answer
            waiter.interrupt();
            waiter.join(10_000);

            assertFalse(waiter.isAlive());
            assertFalse(ran.get());
            assertInstanceOf(OpenRoadmInterfaceException.class, failure.get());
            assertInstanceOf(InterruptedException.class, failure.get().getCause());
            assertTrue(interruptStatus.get());
        } finally {
            release.countDown();
            holder.join(10_000);
        }
        assertEquals(0, nodeLocks.size());
    }
}
