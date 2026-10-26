/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OperationQueueNameTest {

    @Test
    void testKnownQueueNamesAreValid() {
        assertTrue(OperationQueueName.isValid("service-create"));
    }

    @Test
    void testUnknownQueueNameIsInvalid() {
        assertFalse(OperationQueueName.isValid("service-creation"));
    }

    @Test
    void testConfigurationKeysAreNotQueueNames() {
        assertFalse(OperationQueueName.isValid("service-create.queueSize"));
    }

    @Test
    void testMissingQueueNameIsInvalid() {
        assertFalse(OperationQueueName.isValid(""));
        assertFalse(OperationQueueName.isValid(null));
    }

    @Test
    void testServiceCreateQueueHasABuiltInQueueSize() {
        assertEquals(8, OperationQueueName.SERVICE_CREATE.defaultQueueSize());
    }
}
