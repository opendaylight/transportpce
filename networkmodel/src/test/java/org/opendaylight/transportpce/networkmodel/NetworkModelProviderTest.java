/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.service.FrequenciesService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev220630.TransportpceNetworkutilsService;

@ExtendWith(MockitoExtension.class)
public class NetworkModelProviderTest extends AbstractTest {
    @Mock
    NetworkTransactionService networkTransactionService;
    @Mock
    RpcProviderService rpcProviderService;
    @Mock
    NetworkModelService networkModelService;
    @Mock
    DeviceTransactionManager deviceTransactionManager;
    @Mock
    PortMapping portMapping;
    @Mock
    NetConfTopologyListener topologyListener;
    @Mock
    NotificationService notificationService;
    @Mock
    FrequenciesService frequenciesService;


    @Test
    void networkmodelProviderInitTest() {
        Answer<FluentFuture<CommitInfo>> answer = new Answer<FluentFuture<CommitInfo>>() {

            @Override
            public FluentFuture<CommitInfo> answer(InvocationOnMock invocation) throws Throwable {
                return CommitInfo.emptyFluentFuture();
            }

        };
        when(networkTransactionService.commit()).then(answer);

        new NetworkModelProvider(networkTransactionService, getDataBroker(),
            rpcProviderService, networkModelService, deviceTransactionManager, portMapping, notificationService,
            frequenciesService);

//        provider.init();

        verify(rpcProviderService, times(1))
            .registerRpcImplementation(any(), any(TransportpceNetworkutilsService.class));
    }
}
