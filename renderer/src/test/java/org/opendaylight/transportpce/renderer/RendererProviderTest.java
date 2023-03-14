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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.transportpce.test.AbstractTest;

@ExtendWith(MockitoExtension.class)
public class RendererProviderTest extends AbstractTest {

    @Mock
    RpcProviderService rpcProviderService;
    @Mock
    DeviceRendererService deviceRenderer;
    @Mock
    OtnDeviceRendererService otnDeviceRendererService;
    @Mock
    DeviceRendererRPCImpl deviceRendererRPCImpl;
    @Mock
    TransportPCEServicePathRPCImpl transportPCEServicePathRPCImpl;

    @Test
    void testInitMethodRegistersRendererToRpcService() {
        new RendererProvider(rpcProviderService, deviceRenderer, otnDeviceRendererService, deviceRendererRPCImpl,
            transportPCEServicePathRPCImpl);

        verify(rpcProviderService, times(1))
            .registerRpcImplementation(any(), any(TransportPCEServicePathRPCImpl.class));
        verify(rpcProviderService, times(1))
            .registerRpcImplementation(any(), any(DeviceRendererRPCImpl.class));
    }
}
