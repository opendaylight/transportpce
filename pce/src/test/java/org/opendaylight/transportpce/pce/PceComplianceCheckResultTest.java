/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class PceComplianceCheckResultTest {

    PceComplianceCheckResult pceComplianceCheckResult = new PceComplianceCheckResult(false, "message");

    @Test
    void checkGetter() {
        assertFalse(pceComplianceCheckResult.hasPassed());
        assertNotNull(pceComplianceCheckResult.getMessage());
    }
}
