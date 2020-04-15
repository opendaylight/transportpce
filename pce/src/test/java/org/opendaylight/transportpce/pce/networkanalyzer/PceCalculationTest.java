/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceCalculationTest extends AbstractTest {

    private PceCalculation pceCalculation;

    // setup object
    @Before
    public void setUp() {
        PceResult pceResult = new PceResult();
        pceResult.setRC("200");

        pceCalculation = new PceCalculation(
                PceTestData.getPCERequest(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                null,
                null,
                pceResult);


    }

    @Test
    public void testPceCalculationValues() {

        pceCalculation.retrievePceNetwork();
        Assert.assertEquals("100GE", pceCalculation.getServiceType());
        Assert.assertNotNull(pceCalculation.getReturnStructure());

        Assert.assertNull(pceCalculation.getaendPceNode());
        Assert.assertNull(pceCalculation.getzendPceNode());
    }
}
