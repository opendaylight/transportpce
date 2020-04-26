/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.constraints;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceConstraintsCalcTest extends AbstractTest {
    private static PceConstraintsCalc pceConstraintsCalc = null;
    private static NetworkTransactionService networkTransactionService = null;
    private DataStoreContext dataStoreContext = this.getDataStoreContextUtil();
    private DataBroker dataBroker = this.getDataBroker();

    @Before
    public void setup() throws Exception {
        // networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        networkTransactionService = new NetworkTransactionImpl(new RequestProcessor(dataBroker));
        PceTestUtils.writeNetworkIntoDataStore(dataBroker, dataStoreContext, TransactionUtils.getNetworkForSpanLoss());

    }

    @Test
    public void testNoHardOrSoftConstrainsExists() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getEmptyPCERequest(), networkTransactionService);
    }

    @Test(expected = NullPointerException.class)
    public void testHardConstrainsExists() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPCE_test2_request_54(), networkTransactionService);
    }

    @Test
    public void testSoftConstrainsExists() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPCERequest(), networkTransactionService);
    }

}
