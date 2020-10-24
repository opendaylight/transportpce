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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.gnpy.ConnectToGnpyServer;
import org.opendaylight.transportpce.pce.gnpy.JerseyServer;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserFactory;

@RunWith(MockitoJUnitRunner.class)
public class PceSendingPceRPCsTest extends AbstractTest {

    private PceSendingPceRPCs pceSendingPceRPCs;
    private NetworkTransactionImpl networkTransaction;
    @Mock
    private YangParserFactory yangParserFactory;
    @Mock
    private BindingDOMCodecServices bindingDOMCodecServices;
    private JerseyServer jerseyServer = new JerseyServer();
    private DataBroker dataBroker;


    @Before
    public void setUp() {
        this.dataBroker = getNewDataBroker();
        networkTransaction = new NetworkTransactionImpl(new RequestProcessor(this.dataBroker));
        PceTestUtils.writeNetworkInDataStore(this.dataBroker);
        pceSendingPceRPCs = new PceSendingPceRPCs(PceTestData.getPCE_test1_request_54(),
                        networkTransaction, bindingDOMCodecServices);
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
                        networkTransaction, null);

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
