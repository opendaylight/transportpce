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
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

@RunWith(MockitoJUnitRunner.class)
public class PceSendingPceRPCsTest extends AbstractTest {

    private PceSendingPceRPCs pceSendingPceRPCs;
    private NetworkTransactionImpl networkTransaction;


    @Before
    public void setUp() {
        networkTransaction =new NetworkTransactionImpl(new RequestProcessor(this.getNewDataBroker()));
        pceSendingPceRPCs = new PceSendingPceRPCs();
        pceSendingPceRPCs =
                new PceSendingPceRPCs(PceTestData.getPCERequest(), networkTransaction
                );
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
    public void gnpyAtoZ() {
        Assert.assertNull(pceSendingPceRPCs.getGnpyAtoZ());
    }

    @Test
    public void getGnpyZtoA() {
        Assert.assertNull(pceSendingPceRPCs.getGnpyZtoA());
    }


}
