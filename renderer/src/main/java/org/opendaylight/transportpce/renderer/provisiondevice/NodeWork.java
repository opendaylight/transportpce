/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Waits for the per-node work of a service path request, which runs on a fork/join pool of its own.
 */
final class NodeWork {

    private static final Logger LOG = LoggerFactory.getLogger(NodeWork.class);

    private NodeWork() {
    }

    /**
     * Waits for the per-node work of a request. A request that is interrupted, as the renderer does with a direction
     * that does not finish in time, stops the work from starting on further nodes, but still waits for the nodes in
     * progress: it must not return while it is configuring the devices, as its result, or a rollback from its
     * transaction history, would miss that configuration. The fork/join threads do not heed interrupts. The device
     * operations of a node are bounded by the device timeouts, but may first wait for their turn on a busy device.
     *
     * @param work the per-node work
     * @param stopped set once the request is interrupted, which the work checks before it starts on a node
     * @throws InterruptedException if the request was interrupted, once the nodes in progress are done
     * @throws ExecutionException if the work failed
     */
    static void await(ForkJoinTask<?> work, AtomicBoolean stopped) throws InterruptedException, ExecutionException {
        try {
            work.get();
        } catch (InterruptedException e) {
            stopped.set(true);
            try {
                Uninterruptibles.getUninterruptibly(work);
            } catch (ExecutionException | CancellationException failure) {
                // The request is interrupted, which is what it reports
                LOG.warn("Per-node work failed after the request was interrupted", failure);
            }
            throw e;
        }
    }
}
