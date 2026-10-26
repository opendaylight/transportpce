/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.executor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The permit of an operation admitted by an {@link OperationLimit}.
 */
public final class OperationPermit {

    private final Semaphore permits;
    private final AtomicBoolean released = new AtomicBoolean();

    OperationPermit(Semaphore permits) {
        this.permits = permits;
    }

    /**
     * Releases the permit, so that another operation can be admitted. Releasing it again has no effect.
     */
    public void release() {
        if (released.compareAndSet(false, true)) {
            permits.release();
        }
    }
}
