/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.renderer.ServicePathInputData;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRenderingResult;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRenderingResult;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.link.tp.LinkTpBuilder;

/**
 * Tests that a service path delete that fails on some nodes still reports the link termination points it found, so
 * service-delete can free them in the network model.
 */
@ExtendWith(MockitoExtension.class)
class DeviceRenderingTaskTest {

    private static final List<LinkTp> LINK_TPS = List.of(
        new LinkTpBuilder().setNodeId("XPDR-A1").setTpId("XPDR1-NETWORK1").build());

    @Mock
    private DeviceRendererService deviceRenderer;
    @Mock
    private OtnDeviceRendererService otnDeviceRenderer;

    @Test
    void failedDeleteKeepsLinkTps() throws Exception {
        when(deviceRenderer.deleteServicePath(any())).thenReturn(new ServicePathOutputBuilder()
            .setSuccess(false)
            .setResult("Could not read node XPDR-C1")
            .setLinkTp(LINK_TPS)
            .build());
        ServicePathInputData inputData = new ServicePathInputData(
            new ServicePathInputBuilder()
                .setServiceName("service1")
                .setOperation(Action.Delete)
                .build(),
            null);

        DeviceRenderingResult result =
            new DeviceRenderingTask(deviceRenderer, inputData, ServicePathDirection.A_TO_Z, null).call();

        assertFalse(result.isSuccess());
        assertEquals("Could not read node XPDR-C1", result.getResultMessage());
        assertEquals(LINK_TPS, result.getOtnLinkTps());
    }

    @Test
    void failedOtnDeleteKeepsLinkTps() throws Exception {
        when(otnDeviceRenderer.deleteOtnServicePath(any(), anyString())).thenReturn(new OtnServicePathOutputBuilder()
            .setSuccess(false)
            .setResult("Could not read node XPDR-C1")
            .setLinkTp(LINK_TPS)
            .build());
        OtnServicePathInput input = new OtnServicePathInputBuilder()
            .setServiceName("service1")
            .setOperation(Action.Delete)
            .build();

        OtnDeviceRenderingResult result = new OtnDeviceRenderingTask(otnDeviceRenderer, input, "ODU4").call();

        assertFalse(result.isSuccess());
        assertEquals(LINK_TPS, result.getOtnLinkTps());
    }
}
