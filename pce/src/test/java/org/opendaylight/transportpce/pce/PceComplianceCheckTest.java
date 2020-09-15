/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceComplianceCheckTest extends AbstractTest {


    @Before
    public void setup() {

    }

    @Test
    public void testCheckFalse() {
        Assert.assertFalse(
                PceComplianceCheck.check(PceTestData.getEmptyPCERequest()).hasPassed());
    }

    @Test
    public void testCheckTrue() {
        Assert.assertTrue(
                PceComplianceCheck.check(PceTestData.getEmptyPCERequestServiceNameWithRequestId()).hasPassed());
    }

    @Test
    public void testCheckFalseWihtoutRequestID() {
        Assert.assertFalse(
                PceComplianceCheck.check(PceTestData.getEmptyPCERequestServiceNameWithOutRequestId()).hasPassed());
    }
}
