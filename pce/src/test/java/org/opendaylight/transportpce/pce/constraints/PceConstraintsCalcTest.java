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
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceConstraintsCalcTest extends AbstractTest {
    private static PceConstraintsCalc pceConstraintsCalc;
    private static NetworkTransactionService networkTransactionService = null;
    private DataBroker dataBroker = getDataBroker();

    @Before
    public void setup() throws Exception {
        // networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        PceTestUtils.writeNetworkIntoDataStore(dataBroker, getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());
        networkTransactionService = new NetworkTransactionImpl(new RequestProcessor(dataBroker));


    }

    @Test
    public void testNoHardOrSoftConstrainsExists() {
        PceTestData.getPCE_test2_request_54().getSoftConstraints();
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getEmptyPCERequest(), networkTransactionService);
    }

    @Test()
    public void testHardConstrainsExists() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPCE_simpletopology_test1_requestSetHardAndSoftConstrains(), networkTransactionService);
    }

    @Test()
    public void testHardConstrainsExists1() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPathComputationRequestInputWithCoRoutingOrGeneral(), networkTransactionService);
    }

    @Test
    public void testSoftConstrainsExists() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPCERequest(), networkTransactionService);
    }

    @Test(expected = Exception.class)
    public void testHardConstrainsExists2() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .build_diversity_from_request(PceTestData.getPCERequest()), networkTransactionService);
    }

    @Test()
    public void testHardConstrainsExists3() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getEmptyPCERequestServiceNameWithRequestId(), networkTransactionService);
    }

    @Test(expected = Exception.class)
    public void testHardConstrainsExists4() {
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPCE_test2_request_54(), networkTransactionService);
    }

}
