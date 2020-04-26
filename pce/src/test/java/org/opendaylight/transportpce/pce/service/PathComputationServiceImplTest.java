/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.transportpce.pce.gnpy.GnpyTopoImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;

public class PathComputationServiceImplTest extends AbstractTest {

    private PathComputationServiceImpl pathComputationServiceImpl;
    private static NetworkTransactionService networkTransactionService = null;
    private static GnpyTopoImpl gnpyTopoImpl = null;
    private static GnpyResult gnpyResult = null;
    private DataStoreContext dataStoreContext = this.getDataStoreContextUtil();
    private DataBroker dataBroker = this.getDataBroker();

    @Before
    public void setUp() {
        networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        gnpyTopoImpl = Mockito.mock(GnpyTopoImpl.class);
        gnpyResult = Mockito.mock(GnpyResult.class);
        pathComputationServiceImpl = new PathComputationServiceImpl(
                networkTransactionService,
                this.getNotificationPublishService());
        pathComputationServiceImpl.init();
    }

    @Test
    public void pathComputationRequestTest() {
        Assert.assertNotNull(
                pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_simpletopology_test1_request()));

    }

    @Test(expected = Exception.class)
    public void generateGnpyResponse() throws Exception {
        PceTestUtils.writeNetworkIntoDataStore(dataBroker, dataStoreContext, TransactionUtils.getNetworkForSpanLoss());
        GnpyResult gnpyResult2 =
                new GnpyResult("A-to-Z",
                        new GnpyTopoImpl(new NetworkTransactionImpl(
                                new RequestProcessor(dataBroker))));
        pathComputationServiceImpl.generateGnpyResponse(gnpyResult2.getResponse(), "A-to-Z");
    }


    @After
    public void destroy() {
        pathComputationServiceImpl.close();
    }
}
