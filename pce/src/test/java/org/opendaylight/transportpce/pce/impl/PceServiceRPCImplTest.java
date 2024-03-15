/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.impl;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ClassToInstanceMap;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveInputBuilder;


@ExtendWith(MockitoExtension.class)
public class PceServiceRPCImplTest extends AbstractTest {

    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private NetworkTransactionImpl networkTransaction;
    @Mock
    private PortMapping portMapping;
    @Mock
    private RpcProviderService rpcProviderService;


    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        PceTestUtils.writeNetworkIntoDataStore(getDataBroker(), getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());
        notificationPublishService = new NotificationPublishServiceMock();
        networkTransaction =  new NetworkTransactionImpl(getDataBroker());
        pathComputationService = new PathComputationServiceImpl(networkTransaction, notificationPublishService,
                null, portMapping);
    }

    @Test
    void testRpcRegistration() {
        new PceServiceRPCImpl(rpcProviderService, pathComputationService);
        verify(rpcProviderService, times(1)).registerRpcImplementations(any(ClassToInstanceMap.class));
    }

    @Test
    void testCancelResourceReserve() {
        assertNotNull(new CancelResourceReserveImpl(pathComputationService)
                .invoke(new CancelResourceReserveInputBuilder().build()));
    }

    @Test
    void testPathComputationRequest() {
        assertNotNull(new PathComputationRequestImpl(pathComputationService)
                .invoke(PceTestData.getPCERequest()));
    }

    @Test
    void testPathComputationRerouteRequest() {
        assertNotNull(new PathComputationRerouteRequestImpl(pathComputationService)
                .invoke(PceTestData.getPCERerouteRequest()));
    }

    @Test
    void testPathComputationRequestCoRoutingOrGeneral2() {
        assertNotNull(new PathComputationRequestImpl(pathComputationService)
                .invoke(PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral2()));
    }
}
