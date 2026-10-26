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

class ThreadPoolNameTest {

    @Test
    void testKnownPoolNameIsValid() {
        assertTrue(ThreadPoolName.isValid("olm-get-pm"));
        assertTrue(ThreadPoolName.isValid("service-deletion"));
        assertTrue(ThreadPoolName.isValid("service-creation"));
    }

    @Test
    void testUnknownPoolNameIsInvalid() {
        assertFalse(ThreadPoolName.isValid("olm-get-pn"));
    }

    @Test
    void testConfigurationKeysAreNotPoolNames() {
        assertFalse(ThreadPoolName.isValid("olm-get-pm.threads"));
        assertFalse(ThreadPoolName.isValid("olm-get-pm.queueSize"));
    }

    @Test
    void testEnumConstantNameIsNotPoolName() {
        assertFalse(ThreadPoolName.isValid("OLM_GET_PM"));
    }

    @Test
    void testMissingPoolNameIsInvalid() {
        assertFalse(ThreadPoolName.isValid(""));
        assertFalse(ThreadPoolName.isValid(null));
    }

    @Test
    void testPoolWithoutBuiltInQueueSizeUsesTheSharedDefault() {
        assertEquals(64, ThreadPoolName.OLM_GET_PM.defaultQueueSize(64));
    }

    @Test
    void testBuiltInQueueSizeReplacesTheSharedDefault() {
        assertEquals(1024, ThreadPoolName.RENDERER.defaultQueueSize(64));
    }
}
