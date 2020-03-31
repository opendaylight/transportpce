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
import org.opendaylight.transportpce.pce.PceSendingPceRPCs;
import org.opendaylight.transportpce.pce.utils.PceTestData;

public class PceSendingPceRPCsTest {

    private PceSendingPceRPCs pceSendingPceRPCs;

    @Before
    public void setUp() {
        pceSendingPceRPCs = new PceSendingPceRPCs(PceTestData.getPCE_test1_request_54(), null);
    }

    @Test
    public void cancelResourceReserve() {
        pceSendingPceRPCs.cancelResourceReserve();
        Assert.assertEquals(true, pceSendingPceRPCs.getSuccess());
    }

    @Test
    public void pathComputationTest() throws Exception {

        pceSendingPceRPCs.pathComputation();

    }

    @Test
    public void checkMessage() {
        Assert.assertNull(pceSendingPceRPCs.getMessage());
    }

    @Test
    public void responseCodeTest() {
        Assert.assertNull(pceSendingPceRPCs.getResponseCode());
    }

    @Test
    public void pathComputationWithConstraintsTest() {
        pceSendingPceRPCs.pathComputationWithConstraints(null, null);
    }
}
