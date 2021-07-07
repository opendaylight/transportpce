/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OpucnTribSlotDef;

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
        OpucnTribSlotDef minOpucnTs = new OpucnTribSlotDef("1.1");
        OpucnTribSlotDef maxOpucnTs = new OpucnTribSlotDef("1.20");
        List<OpucnTribSlotDef> minmaxTpTsList = new ArrayList<>();
        minmaxTpTsList.add(minOpucnTs);
        minmaxTpTsList.add(maxOpucnTs);
        pceResult.setResultTribPortTribSlot(minmaxTpTsList);
        Assert.assertEquals(pceResult.getResultTribPortTribSlot().get(0), new OpucnTribSlotDef("1.1"));
        Assert.assertEquals(pceResult.getResultTribPortTribSlot().get(1), new OpucnTribSlotDef("1.20"));
    }

    @Test
    public void calcMessageTest() {
        pceResult.setCalcMessage("some-message");
        pceResult.setRC("200");
        Assert.assertEquals(pceResult.getMessage(), "Path is calculated by PCE");
    }

    @Test
    public void waveLengthTest() {
        Assert.assertEquals(0, pceResult.getResultWavelength());
        pceResult.setResultWavelength(12);
        Assert.assertEquals(12, pceResult.getResultWavelength());
    }

    @Test
    public void localCause() {
        pceResult.setLocalCause(PceResult.LocalCause.INT_PROBLEM);
        Assert.assertEquals(pceResult.getLocalCause(), PceResult.LocalCause.INT_PROBLEM);
    }
}
