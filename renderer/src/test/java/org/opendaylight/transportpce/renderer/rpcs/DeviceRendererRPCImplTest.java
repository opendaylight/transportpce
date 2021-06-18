/*
 * Copyright © 2019 Orange , Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.ServicePathInput;

@Ignore
public class DeviceRendererRPCImplTest extends AbstractTest {
    private final DeviceRendererService deviceRenderer = Mockito.mock(DeviceRendererService.class);
    private final OtnDeviceRendererService otnDeviceRenderer = Mockito.mock(OtnDeviceRendererService.class);
    private final ServicePathInput servicePathInput = Mockito.spy(ServicePathInput.class);
    private final CreateOtsOmsInput createOtsOmsInput = Mockito.mock(CreateOtsOmsInput.class);
    private final RendererRollbackInput rendererRollbackInput = Mockito.mock(RendererRollbackInput.class);
    private DeviceRendererRPCImpl deviceRendererRPC = null;

    @Before
    public void setup() {

        deviceRendererRPC = new DeviceRendererRPCImpl(deviceRenderer, otnDeviceRenderer);
    }


    @Test
    public void testServicePathCreateOption() {

        Mockito.when(servicePathInput.getOperation()).thenReturn(Action.Create);
        deviceRendererRPC.servicePath(servicePathInput);
        Mockito.verify(deviceRenderer, Mockito.times(1)).setupServicePath(servicePathInput, null);

    }

    @Test
    public void testServicePathDeleteOption() {

        Mockito.when(servicePathInput.getOperation()).thenReturn(Action.Create);
        deviceRendererRPC.servicePath(servicePathInput);
        Mockito.verify(deviceRenderer, Mockito.times(1)).deleteServicePath(servicePathInput);

    }

    @Test
    public void testRendererRollback() {
        Mockito.when(deviceRenderer.rendererRollback(rendererRollbackInput))
                .thenReturn(new RendererRollbackOutputBuilder().build());
        deviceRendererRPC.rendererRollback(rendererRollbackInput);
        Mockito.verify(deviceRenderer, Mockito.times(1)).rendererRollback(rendererRollbackInput);
    }

    @Test
    public void testCreateOtsOms() throws OpenRoadmInterfaceException {

        Mockito.when(createOtsOmsInput.getNodeId()).thenReturn("nodeId");
        Mockito.when(createOtsOmsInput.getLogicalConnectionPoint()).thenReturn("logicalConnectionPoint");
        Mockito.when(deviceRenderer.createOtsOms(createOtsOmsInput)).thenReturn(null);
        deviceRendererRPC.createOtsOms(createOtsOmsInput);
        Mockito.verify(deviceRenderer, Mockito.times(1)).createOtsOms(createOtsOmsInput);


    }

    @Test
    public void testCreateOtsOmsReturnException() throws OpenRoadmInterfaceException {

        Mockito.when(createOtsOmsInput.getNodeId()).thenReturn("nodeId");
        Mockito.when(createOtsOmsInput.getLogicalConnectionPoint()).thenReturn("logicalConnectionPoint");
        Mockito.when(deviceRenderer.createOtsOms(createOtsOmsInput)).thenThrow(OpenRoadmInterfaceException.class);
        Assert.assertNull(deviceRendererRPC.createOtsOms(createOtsOmsInput));


    }


}
