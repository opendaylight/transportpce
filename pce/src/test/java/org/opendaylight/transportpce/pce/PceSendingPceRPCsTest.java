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
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.gnpy.ConnectToGnpyServer;
import org.opendaylight.transportpce.pce.gnpy.JerseyServer;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.test.AbstractTest;

@Ignore
//@RunWith(MockitoJUnitRunner.class)
public class PceSendingPceRPCsTest extends AbstractTest {

    private PceSendingPceRPCs pceSendingPceRPCs;
    private NetworkTransactionImpl networkTransaction;
    private DataStoreContext dataStoreContext = this.getDataStoreContextUtil();
    private DataBroker dataBroker = this.getDataBroker();
    private JerseyServer jerseyServer = new JerseyServer();


    @Before
    public void setUp() {
        networkTransaction = new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker()));
        PceTestUtils.writeNetworkInDataStore(this.getDataBroker());
        pceSendingPceRPCs =
                new PceSendingPceRPCs(PceTestData.getPCE_test1_request_54(), networkTransaction, null, null);
    }

    @Test
    public void cancelResourceReserve() {
        pceSendingPceRPCs.cancelResourceReserve();
        Assert.assertTrue("Success should equal to true", pceSendingPceRPCs.getSuccess());
    }

    @Test
    public void pathComputationTest() throws Exception {
        jerseyServer.setUp();
        pceSendingPceRPCs =
                new PceSendingPceRPCs(PceTestData.getGnpyPCERequest("XPONDER-1", "XPONDER-2"),
                        networkTransaction, null, null);

        pceSendingPceRPCs.pathComputation();
        ConnectToGnpyServer connectToGnpy = new ConnectToGnpyServer();
        Assert.assertTrue(connectToGnpy.isGnpyURLExist());
        jerseyServer.tearDown();

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
