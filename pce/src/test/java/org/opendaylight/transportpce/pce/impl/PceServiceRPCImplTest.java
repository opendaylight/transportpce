/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInputBuilder;




public class PceServiceRPCImplTest extends AbstractTest {

    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private NetworkTransactionImpl networkTransaction;
    private RequestProcessor requestProcessor;
    private PceServiceRPCImpl pceServiceRPC;

    @Before
    public void setUp() {
        notificationPublishService = new NotificationPublishServiceMock();
        requestProcessor = Mockito.mock(RequestProcessor.class);
        networkTransaction = new NetworkTransactionImpl(requestProcessor);
        pathComputationService = new PathComputationServiceImpl(networkTransaction, notificationPublishService);
        pceServiceRPC = new PceServiceRPCImpl(pathComputationService);
    }

    @Test
    public void testCancelResourceReserve() {
        CancelResourceReserveInputBuilder cancelResourceReserveInput = new CancelResourceReserveInputBuilder();
        assertNotNull(pceServiceRPC.cancelResourceReserve(cancelResourceReserveInput.build()));
    }

    @Test
    public void testPathComputationRequest() {
        PathComputationRequestInput pathComputationRequestInput =
                new PathComputationRequestInputBuilder().build();
        assertNotNull(pceServiceRPC.pathComputationRequest(pathComputationRequestInput));
    }

}
