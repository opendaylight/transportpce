/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceComplianceCheckTest extends AbstractTest {

    @Test
    void testCheckFalse() {
        assertFalse(PceComplianceCheck.check(PceTestData.getEmptyPCERequest()).hasPassed());
    }

    @Test
    void testCheckTrue() {
        assertTrue(PceComplianceCheck.check(PceTestData.getEmptyPCERequestServiceNameWithRequestId()).hasPassed());
    }

    @Test
    void testCheckFalseWihtoutRequestID() {
        assertFalse(PceComplianceCheck.check(PceTestData.getEmptyPCERequestServiceNameWithOutRequestId()).hasPassed());
    }
}
