/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.test.AbstractTest;

@Ignore
public class PceResultTest extends AbstractTest {

    private PceResult pceResult = null;

    @Before
    public void setUp() {
        pceResult = new PceResult();
    }

    @Test
    public void serviceTypeTest() {
        String serviceType = "some-service";
        pceResult.setServiceType(serviceType);
        Assert.assertEquals(pceResult.getServiceType(), serviceType);
    }

    @Test
    public void setResultTribSlotNbTest() {
        int nb = 10;
        pceResult.setResultTribSlotNb(nb);
        Assert.assertEquals(pceResult.getResultTribSlotNb(), nb);
    }

    @Test
    public void calcMessageTest() {
        pceResult.setCalcMessage("some-message");
        pceResult.setRC("200");
        Assert.assertEquals(pceResult.getMessage(), "Path is calculated by PCE");
    }

    @Test
    public void waveLengthTest() {
        Assert.assertEquals(pceResult.getResultWavelength(), -1);
        pceResult.setResultWavelength(12);
        Assert.assertEquals(pceResult.getResultWavelength(), 12);
    }

    @Test
    public void localCause() {
        pceResult.setLocalCause(PceResult.LocalCause.INT_PROBLEM);
        Assert.assertEquals(pceResult.getLocalCause(), PceResult.LocalCause.INT_PROBLEM);
    }
}
