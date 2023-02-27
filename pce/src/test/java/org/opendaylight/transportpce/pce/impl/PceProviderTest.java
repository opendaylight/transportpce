/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.TransportpcePceService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

public class PceProviderTest extends AbstractTest {

    private RpcProviderService rpcService;
    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private NetworkTransactionImpl networkTransaction;
    private ObjectRegistration<TransportpcePceService> rpcRegistration;
    private PceProvider pceProvider;

    @BeforeEach
    void setUp() {
        rpcService = mock(RpcProviderService.class);
        notificationPublishService = new NotificationPublishServiceMock();
        networkTransaction = new NetworkTransactionImpl(getDataBroker());
        pathComputationService = new PathComputationServiceImpl(networkTransaction, notificationPublishService,
                null, null);
        pceProvider = new PceProvider(rpcService, pathComputationService);
    }

    @Test
    void testInit() {
        this.rpcRegistration = new ObjectRegistration<TransportpcePceService>() {
            @NonNull
            @Override
            public TransportpcePceService getInstance() {
                return new PceServiceRPCImpl(pathComputationService);
            }

            @Override
            public void close() {

            }
        };
        when(rpcService.registerRpcImplementation(eq(TransportpcePceService.class), any())).thenReturn(rpcRegistration);
        pceProvider.init();
        pceProvider.close();
    }
}
