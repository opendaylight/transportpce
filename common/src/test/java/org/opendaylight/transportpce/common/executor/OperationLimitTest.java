/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.executor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OperationLimitTest {

    private final OperationLimit limit = new OperationLimit(2);

    @Test
    void operationsBeyondTheLimitAreNotAdmitted() {
        assertTrue(limit.tryAcquire().isPresent());
        assertTrue(limit.tryAcquire().isPresent());

        assertTrue(limit.tryAcquire().isEmpty());
    }

    @Test
    void releasedPermitAdmitsAnotherOperation() {
        limit.tryAcquire().orElseThrow().release();
        limit.tryAcquire().orElseThrow();

        assertTrue(limit.tryAcquire().isPresent());
    }

    @Test
    void releasingAPermitTwiceAdmitsOnlyOneMoreOperation() {
        OperationPermit permit = limit.tryAcquire().orElseThrow();
        limit.tryAcquire().orElseThrow();
        permit.release();

        permit.release();

        assertTrue(limit.tryAcquire().isPresent());
        assertTrue(limit.tryAcquire().isEmpty());
    }
}
