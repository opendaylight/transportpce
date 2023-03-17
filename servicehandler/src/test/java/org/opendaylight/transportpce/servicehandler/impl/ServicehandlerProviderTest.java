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
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.ServiceListener;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.test.AbstractTest;

@ExtendWith(MockitoExtension.class)
public class ServicehandlerProviderTest  extends AbstractTest {

    @Mock
    RpcProviderService rpcProviderRegistry;
    @Mock
    ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    CatalogDataStoreOperations catalogDataStoreOperations;
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


    @Test
    void testInitRegisterServiceHandlerToRpcRegistry() {
        ServicehandlerProvider provider =  new ServicehandlerProvider(
                getDataBroker(), rpcProviderRegistry,
                getNotificationService() , serviceDataStoreOperations, pceListenerImpl, serviceListener,
                rendererListenerImpl, networkModelListenerImpl, servicehandler, catalogDataStoreOperations);

        provider.init();

        verify(rpcProviderRegistry, times(1)).registerRpcImplementation(any(), any(ServicehandlerImpl.class));
    }
}