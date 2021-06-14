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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.ServiceListener;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.test.AbstractTest;

public class ServicehandlerProviderTest  extends AbstractTest {

    @Mock
    RpcProviderService rpcProviderRegistry;

    @Mock
    ServiceDataStoreOperations serviceDataStoreOperations;

    @Mock
    PceListenerImpl pceListenerImpl;

    @Mock
    ServiceListener serviceListener;

    @Mock
    RendererListenerImpl rendererListenerImpl;

    @Mock
    NetworkModelListenerImpl networkModelListenerImpl;

    @Mock
    ServicehandlerImpl servicehandler;


    private AutoCloseable closeable;

    @Before
    public void openMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInitRegisterServiceHandlerToRpcRegistry() {
        ServicehandlerProvider provider =  new ServicehandlerProvider(
                getDataBroker(), rpcProviderRegistry,
                getNotificationService() , serviceDataStoreOperations, pceListenerImpl, serviceListener,
                rendererListenerImpl, networkModelListenerImpl, servicehandler);

        provider.init();

        verify(rpcProviderRegistry, times(1))
                .registerRpcImplementation(any(), any(ServicehandlerImpl.class));
    }

    @After public void releaseMocks() throws Exception {
        closeable.close();
    }

}
