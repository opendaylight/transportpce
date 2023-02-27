/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ComplianceCheckResultTest {

    @Test
    void constructComplianceCheckResult() {
        ComplianceCheckResult checkResult = new ComplianceCheckResult(true);
        assertEquals(true, checkResult.hasPassed());

        checkResult = new ComplianceCheckResult(false);
        assertEquals(false, checkResult.hasPassed());
    }
}