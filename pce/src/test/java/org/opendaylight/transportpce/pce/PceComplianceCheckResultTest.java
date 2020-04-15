/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.transportpce.pce.PceComplianceCheckResult;

public class PceComplianceCheckResultTest {

    PceComplianceCheckResult pceComplianceCheckResult = new PceComplianceCheckResult(false, "message");

    @Test
    public void checkGetter() {
        Assert.assertEquals(false, pceComplianceCheckResult.hasPassed());
        Assert.assertNotNull(pceComplianceCheckResult.getMessage());
    }
}
