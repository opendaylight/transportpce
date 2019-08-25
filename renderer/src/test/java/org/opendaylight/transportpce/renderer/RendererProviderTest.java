/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.transportpce.test.AbstractTest;


public class RendererProviderTest extends AbstractTest {


    @Mock
    RpcProviderRegistry rpcProviderRegistry;

    @Mock
    private RendererServiceOperations rendererServiceOperations;

    @Mock
    DeviceRendererRPCImpl deviceRendererRPC;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void testInitMethodRegistersRendererToRpcRegistry() {
        RendererProvider provider = new RendererProvider(rpcProviderRegistry,
                deviceRendererRPC, rendererServiceOperations);
        provider.init();

        verify(rpcProviderRegistry, times(1))
                .addRpcImplementation(any(), any(TransportPCEServicePathRPCImpl.class));

        verify(rpcProviderRegistry, times(1))
                .addRpcImplementation(any(), any(DeviceRendererRPCImpl.class));
    }


}
