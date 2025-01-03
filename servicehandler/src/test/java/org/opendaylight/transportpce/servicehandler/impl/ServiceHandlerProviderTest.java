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
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.PceNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.RendererNotificationHandler;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;

@ExtendWith(MockitoExtension.class)
public class ServiceHandlerProviderTest extends AbstractTest {

    @Mock
    RendererServiceOperations rendererServiceOperations;
    @Mock
    PathComputationService pathComputationService;
    @Mock
    NetworkModelService networkModelService;
    @Mock
    DataBroker dataBroker;
    @Mock
    ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    NotificationPublishService notificationPublishService;
    @Mock
    DataTreeChangeListener<Services> serviceListener;

    private final PceNotificationHandler pceNotificationHandler = new PceNotificationHandler(rendererServiceOperations,
        pathComputationService, notificationPublishService, serviceDataStoreOperations);
    private final RendererNotificationHandler rendererNotificationHandler = new RendererNotificationHandler(
        pathComputationService, notificationPublishService, networkModelService);
    private final NetworkModelNotificationHandler networkModelNotificationHandler = new NetworkModelNotificationHandler(
        notificationPublishService, serviceDataStoreOperations);

    @Test
    void testInitRegisterServiceHandlerToRpcRegistry() {
        new ServiceHandlerProvider(dataBroker, getNotificationService(),
            serviceDataStoreOperations, pceNotificationHandler, rendererNotificationHandler,
            networkModelNotificationHandler, serviceListener);
        verify(dataBroker, times(1)).registerTreeChangeListener(any(), any(), any());
    }
}
