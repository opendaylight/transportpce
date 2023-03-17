/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.Services;

@ExtendWith(MockitoExtension.class)
public class ServicehandlerProviderTest extends AbstractTest {

    @Mock
    DataBroker dataBroker;
    @Mock
    RpcProviderService rpcProviderRegistry;
    @Mock
    ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    TransportpcePceListener pceListenerImpl;
    @Mock
    TransportpceRendererListener rendererListenerImpl;
    @Mock
    TransportpceNetworkmodelListener networkModelListenerImpl;
    @Mock
    NotificationPublishService notificationPublishService;
    @Mock
    OrgOpenroadmServiceService servicehandler;
    @Mock
    DataTreeChangeListener<Services> serviceListener;

    @Test
    void testInitRegisterServiceHandlerToRpcRegistry() {
        new ServicehandlerProvider(dataBroker, rpcProviderRegistry, getNotificationService() ,
                serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                notificationPublishService, servicehandler, serviceListener);

        verify(rpcProviderRegistry, times(1)).registerRpcImplementation(any(), any(OrgOpenroadmServiceService.class));
        verify(dataBroker, times(1)).registerDataTreeChangeListener(any(), any());
    }
}