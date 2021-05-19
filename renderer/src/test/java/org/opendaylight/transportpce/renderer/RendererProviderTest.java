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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.transportpce.test.AbstractTest;

@Ignore
public class RendererProviderTest extends AbstractTest {


    @Mock
    RpcProviderService rpcProviderService;

    @Mock
    private RendererServiceOperations rendererServiceOperations;

    @Mock
    DeviceRendererRPCImpl deviceRendererRPC;

    private AutoCloseable closeable;

    @Before
    public void openMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInitMethodRegistersRendererToRpcService() {
        RendererProvider provider =
            new RendererProvider(rpcProviderService, deviceRendererRPC, rendererServiceOperations);
        provider.init();

        verify(rpcProviderService, times(1))
                .registerRpcImplementation(any(), any(TransportPCEServicePathRPCImpl.class));

        verify(rpcProviderService, times(1))
                .registerRpcImplementation(any(), any(DeviceRendererRPCImpl.class));
    }

    @After public void releaseMocks() throws Exception {
        closeable.close();
    }

}
