/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OpucnTribSlotDef;

public class PceResultTest extends AbstractTest {

    private PceResult pceResult = null;

    @BeforeEach
    void setUp() {
        pceResult = new PceResult();
    }

    @Test
    void serviceTypeTest() {
        String serviceType = "some-service";
        pceResult.setServiceType(serviceType);
        assertEquals(pceResult.getServiceType(), serviceType);
    }

    @Test
    void setResultTribSlotNbTest() {
        OpucnTribSlotDef minOpucnTs = new OpucnTribSlotDef("1.1");
        OpucnTribSlotDef maxOpucnTs = new OpucnTribSlotDef("1.20");
        List<OpucnTribSlotDef> minmaxTpTsList = new ArrayList<>();
        minmaxTpTsList.add(minOpucnTs);
        minmaxTpTsList.add(maxOpucnTs);
        pceResult.setResultTribPortTribSlot(minmaxTpTsList);
        assertEquals(pceResult.getResultTribPortTribSlot().get(0), new OpucnTribSlotDef("1.1"));
        assertEquals(pceResult.getResultTribPortTribSlot().get(1), new OpucnTribSlotDef("1.20"));
    }

    @Test
    void calcMessageTest() {
        pceResult.setCalcMessage("some-message");
        pceResult.success();
        assertEquals(pceResult.getMessage(), "Path is calculated by PCE");
    }

    @Test
    void waveLengthTest() {
        assertEquals(0, pceResult.getResultWavelength());
        pceResult.setResultWavelength(12);
        assertEquals(12, pceResult.getResultWavelength());
    }

    @Test
    void localCause() {
        pceResult.setLocalCause(PceResult.LocalCause.INT_PROBLEM);
        assertEquals(pceResult.getLocalCause(), PceResult.LocalCause.INT_PROBLEM);
    }
}
