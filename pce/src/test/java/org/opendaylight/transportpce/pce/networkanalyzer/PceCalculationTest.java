/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;

@Ignore
public class PceCalculationTest extends AbstractTest {

    private PceCalculation pceCalculation;
    private PceConstraintsCalc pceConstraintsCalc;
    private PceResult pceResult = new PceResult();

    // setup object
    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        pceResult.setRC("200");
        PceTestUtils.writeNetworkIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());

        pceConstraintsCalc = new PceConstraintsCalc(PceTestData.getPCERequest(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())));

        pceCalculation = new PceCalculation(
                PceTestData.getPCERequest(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
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

    @Test
    public void testPceCalculationValues2() {

        pceCalculation = new PceCalculation(
                PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
                pceResult);
        pceCalculation.retrievePceNetwork();
        Assert.assertEquals("100GE", pceCalculation.getServiceType());
        Assert.assertNotNull(pceCalculation.getReturnStructure());

        Assert.assertNull(pceCalculation.getaendPceNode());
        Assert.assertNull(pceCalculation.getzendPceNode());
    }

    @Test
    public void testPceCalculationValues42() {

        PathComputationRequestInput input = PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral2();
        pceConstraintsCalc = new PceConstraintsCalc(input,
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())));

        pceCalculation = new PceCalculation(
                PceTestData.getPCE_test3_request_54(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
                pceResult);

        pceCalculation.retrievePceNetwork();
//        Assert.assertEquals("100GE", pceCalculation.getServiceType());
        Assert.assertNotNull(pceCalculation.getReturnStructure());

        Assert.assertNull(pceCalculation.getaendPceNode());
        Assert.assertNull(pceCalculation.getzendPceNode());
    }
}
