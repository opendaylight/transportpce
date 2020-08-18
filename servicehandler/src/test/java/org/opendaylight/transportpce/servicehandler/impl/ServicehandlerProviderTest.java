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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.test.AbstractTest;

@Ignore
public class ServicehandlerProviderTest  extends AbstractTest {

    @Mock
    private PathComputationService pathComputationService;

    @Mock
    private RendererServiceOperations rendererServiceOperations;

    @Mock
    private NotificationPublishService notificationPublishService;

    @Mock
    RpcProviderService rpcProviderRegistry;

    @Mock
    NetworkModelWavelengthService networkModelWavelengthService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void testInitRegisterServiceHandlerToRpcRegistry() {
        ServicehandlerProvider provider =  new ServicehandlerProvider(
                getDataBroker(), rpcProviderRegistry,
                getNotificationService() , pathComputationService,
                rendererServiceOperations, networkModelWavelengthService,
                notificationPublishService);

        provider.init();

        verify(rpcProviderRegistry, times(1))
                .registerRpcImplementation(any(), any(ServicehandlerImpl.class));
    }





}
