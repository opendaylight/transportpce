/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.impl;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.CancelResourceReserveInputBuilder;


public class PceServiceRPCImplTest extends AbstractTest {

    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private NetworkTransactionImpl networkTransaction;
    private PceServiceRPCImpl pceServiceRPC;
    @Mock
    private PortMapping portMapping;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        PceTestUtils.writeNetworkIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());
        notificationPublishService = new NotificationPublishServiceMock();
        networkTransaction =  new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker()));
        pathComputationService = new PathComputationServiceImpl(networkTransaction, notificationPublishService,
                null, portMapping);
        pceServiceRPC = new PceServiceRPCImpl(pathComputationService);

    }

    @Test
    public void testCancelResourceReserve() {
        CancelResourceReserveInputBuilder cancelResourceReserveInput = new CancelResourceReserveInputBuilder();
        assertNotNull(pceServiceRPC.cancelResourceReserve(cancelResourceReserveInput.build()));
    }

    @Test
    public void testPathComputationRequest() {
        assertNotNull(pceServiceRPC.pathComputationRequest(PceTestData.getPCERequest()));
    }

    @Test
    public void testPathComputationRequestCoRoutingOrGeneral2() {
        assertNotNull(pceServiceRPC.pathComputationRequest(
                PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral2()));
    }
}
