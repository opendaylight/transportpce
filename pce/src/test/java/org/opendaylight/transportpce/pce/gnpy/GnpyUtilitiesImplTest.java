/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

@Ignore

public class GnpyUtilitiesImplTest extends AbstractTest {

    private GnpyUtilitiesImpl gnpyUtilitiesImpl;
    private NetworkTransactionImpl networkTransaction;

    @Before
    public void setUp()throws Exception {
        networkTransaction = new NetworkTransactionImpl(new RequestProcessor(this.getNewDataBroker()));

        gnpyUtilitiesImpl = new GnpyUtilitiesImpl(networkTransaction, PceTestData.getPCE_test1_request_54());
    }

    @Test(expected = Exception.class)
    public void askNewPathFromGnpyTest() throws Exception {
        PceConstraintsCalc constraints =
                new PceConstraintsCalc(PceTestData.getPCE_simpletopology_test1_request(), networkTransaction);
        PceConstraints pceHardConstraints = constraints.getPceHardConstraints();
        gnpyUtilitiesImpl.askNewPathFromGnpy(pceHardConstraints);
    }

    @Test(expected = Exception.class)
    public void gnpyResponseOneDirectionTest() throws Exception {
        gnpyUtilitiesImpl.gnpyResponseOneDirection(null);
    }


}
