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
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;

@ExtendWith(MockitoExtension.class)
public class ServicehandlerProviderTest extends AbstractTest {

    @Mock
    RendererServiceOperations rendererServiceOperations;
    @Mock
    PathComputationService pathComputationService;
    @Mock
    NetworkModelService networkModelService;
    @Mock
    DataBroker dataBroker;
    @Mock
    RpcProviderService rpcProviderRegistry;
    @Mock
    ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    NotificationPublishService notificationPublishService;
    @Mock
    OrgOpenroadmServiceService servicehandler;
    @Mock
    DataTreeChangeListener<Services> serviceListener;

    private final PceListenerImpl pceListenerImpl = new PceListenerImpl(rendererServiceOperations,
        pathComputationService, notificationPublishService, serviceDataStoreOperations);
    private final RendererListenerImpl rendererListenerImpl = new RendererListenerImpl(pathComputationService,
        notificationPublishService, networkModelService);
    private final NetworkModelListenerImpl networkModelListenerImpl = new NetworkModelListenerImpl(
        notificationPublishService, serviceDataStoreOperations);

    @Test
    void testInitRegisterServiceHandlerToRpcRegistry() {
        new ServicehandlerProvider(dataBroker, rpcProviderRegistry, getNotificationService(),
            serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
            notificationPublishService, servicehandler, serviceListener);

        verify(rpcProviderRegistry, times(1)).registerRpcImplementation(any(), any(OrgOpenroadmServiceService.class));
        verify(dataBroker, times(1)).registerDataTreeChangeListener(any(), any());
    }
}