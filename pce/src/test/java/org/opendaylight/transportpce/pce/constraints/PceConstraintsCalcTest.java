/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.constraints;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceConstraintsCalcTest extends AbstractTest {
    private static NetworkTransactionService networkTransactionService = null;
    private DataBroker dataBroker = getDataBroker();

    //TODO: review this test class. May be miss few assert.
    @BeforeEach
    void setup() throws Exception {
        // networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        PceTestUtils.writeNetworkIntoDataStore(dataBroker, getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());
        networkTransactionService = new NetworkTransactionImpl(dataBroker);
    }

    @Test
    void testNoHardOrSoftConstrainsExists() {
        PceTestData.getPCE_test2_request_54().getSoftConstraints();
        new PceConstraintsCalc(PceTestData.getEmptyPCERequest(), networkTransactionService);
    }

    @Test()
    void testHardConstrainsExists() {
        new PceConstraintsCalc(
            PceTestData.getPCE_simpletopology_test1_requestSetHardAndSoftConstrains(),
            networkTransactionService);
    }

    @Test()
    void testHardConstrainsExists1() {
        new PceConstraintsCalc(
            PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral(),
            networkTransactionService);
    }

    @Test
    void testSoftConstrainsExists() {
        new PceConstraintsCalc(PceTestData.getPCERequest(), networkTransactionService);
    }

    //TODO: See if this test is relevant.
    @Test
    void testHardConstrainsExists2() {
        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            new PceConstraintsCalc(
                    PceTestData.build_diversity_from_request(PceTestData.getPCERequest()),
                    networkTransactionService);
        });
        assertTrue(exception.getMessage().contains("No value present"));
    }

    @Test()
    void testHardConstrainsExists3() {
        new PceConstraintsCalc(PceTestData.getEmptyPCERequestServiceNameWithRequestId(), networkTransactionService);
    }

    @Test()
    void testHardConstrainsExists4() {
        new PceConstraintsCalc(PceTestData.getPCE_test2_request_54(), networkTransactionService);
    }
}
