/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor.pool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestLimitNameTest {

    @Test
    void testKnownLimitNamesAreValid() {
        assertTrue(RequestLimitName.isValid("service-implementation-request"));
        assertTrue(RequestLimitName.isValid("service-delete"));
    }

    @Test
    void testUnknownLimitNameIsInvalid() {
        assertFalse(RequestLimitName.isValid("service-delet"));
    }

    @Test
    void testConfigurationKeysAreNotLimitNames() {
        assertFalse(RequestLimitName.isValid("service-implementation-request.maxRequests"));
        assertFalse(RequestLimitName.isValid("service-delete.maxRequests"));
    }

    @Test
    void testEnumConstantNamesAreNotLimitNames() {
        assertFalse(RequestLimitName.isValid("SERVICE_IMPLEMENTATION_REQUEST"));
        assertFalse(RequestLimitName.isValid("SERVICE_DELETE"));
    }

    @Test
    void testMissingLimitNameIsInvalid() {
        assertFalse(RequestLimitName.isValid(""));
        assertFalse(RequestLimitName.isValid(null));
    }
}
