/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.executor.limit;

import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * Limits how many operations of a kind are in progress at once, such as the requests of an RPC, or the service
 * handler's service creations. An operation that queues at most one task at a time on an executor whose queue has room
 * for as many tasks as the limit allows never has a task turned away for want of room, which would leave it unfinished.
 */
public class OperationLimit {

    private final Semaphore permits;

    /**
     * Creates a limit.
     *
     * @param maxOperations how many operations may be in progress at once
     */
    public OperationLimit(int maxOperations) {
        this.permits = new Semaphore(maxOperations);
    }

    /**
     * Admits an operation, unless the limit is reached.
     *
     * @return the operation's permit, or empty if as many operations as the limit allows are in progress
     */
    public Optional<OperationPermit> tryAcquire() {
        return permits.tryAcquire() ? Optional.of(new OperationPermit(permits)) : Optional.empty();
    }
}
