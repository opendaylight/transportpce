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
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceConstraintsCalcTest extends AbstractTest {
    private static PceConstraintsCalc pceConstraintsCalc = null;
    private static NetworkTransactionService networkTransactionService = null;

    @Before
    public void setup() {
        networkTransactionService = Mockito.mock(NetworkTransactionService.class);

    }

    @Test
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
