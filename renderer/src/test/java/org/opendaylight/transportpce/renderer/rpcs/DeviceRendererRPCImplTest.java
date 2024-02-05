/*
 * Copyright © 2019 Orange , Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOms;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePath;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInput;


public class DeviceRendererRPCImplTest extends AbstractTest {
    private final RpcProviderService rpcProviderService = mock(RpcProviderService.class);
    private final DeviceRendererService deviceRenderer = mock(DeviceRendererService.class);
    private final ServicePathInput servicePathInput = spy(ServicePathInput.class);
    private final CreateOtsOmsInput createOtsOmsInput = mock(CreateOtsOmsInput.class);
    private final RendererRollbackInput rendererRollbackInput = mock(RendererRollbackInput.class);
    private ServicePath servicePath;
    private RendererRollback rendererRollback;
    private CreateOtsOms createOtsOms;

    @BeforeAll
    void setup() {
        servicePath = new ServicePathImpl(deviceRenderer);
        rendererRollback = new RendererRollbackImpl(deviceRenderer);
        createOtsOms = new CreateOtsOmsImpl(deviceRenderer);
    }

    @Test
    void testRpcRegistration() {
        verify(rpcProviderService, times(1)).registerRpcImplementations(any());
    }

    @Test
    void testServicePathCreateOption() {
        when(servicePathInput.getOperation()).thenReturn(Action.Create);
        servicePath.invoke(servicePathInput);
        verify(deviceRenderer, times(1)).setupServicePath(servicePathInput, null);
    }

    @Test
    void testServicePathDeleteOption() {
        when(servicePathInput.getOperation()).thenReturn(Action.Delete);
        servicePath.invoke(servicePathInput);
        verify(deviceRenderer, times(1)).deleteServicePath(servicePathInput);
    }

    @Test
    void testRendererRollback() {
        when(deviceRenderer.rendererRollback(rendererRollbackInput))
            .thenReturn(new RendererRollbackOutputBuilder().build());
        rendererRollback.invoke(rendererRollbackInput);
        verify(deviceRenderer, times(1)).rendererRollback(rendererRollbackInput);
    }

    @Test
    void testCreateOtsOms() throws OpenRoadmInterfaceException {
        when(createOtsOmsInput.getNodeId()).thenReturn("nodeId");
        when(createOtsOmsInput.getLogicalConnectionPoint()).thenReturn("logicalConnectionPoint");
        when(deviceRenderer.createOtsOms(createOtsOmsInput)).thenReturn(null);
        createOtsOms.invoke(createOtsOmsInput);
        verify(deviceRenderer, times(1)).createOtsOms(createOtsOmsInput);
    }

    @Test
    void testCreateOtsOmsReturnException() throws OpenRoadmInterfaceException {
        when(createOtsOmsInput.getNodeId()).thenReturn("nodeId");
        when(createOtsOmsInput.getLogicalConnectionPoint()).thenReturn("logicalConnectionPoint");
        when(deviceRenderer.createOtsOms(createOtsOmsInput)).thenThrow(OpenRoadmInterfaceException.class);
        assertNull(createOtsOms.invoke(createOtsOmsInput));
    }
}
