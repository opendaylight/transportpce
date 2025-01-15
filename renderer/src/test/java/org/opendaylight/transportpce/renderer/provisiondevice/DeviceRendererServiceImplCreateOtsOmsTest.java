/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.renderer.utils.CreateOtsOmsDataUtils;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;

@ExtendWith(MockitoExtension.class)
public class DeviceRendererServiceImplCreateOtsOmsTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private OpenRoadmInterfaces openRoadmInterfaces;
    @Mock
    private CrossConnect crossConnect;
    @Mock
    private MappingUtils mappingUtils;
    @Mock
    private PortMapping portMapping;
    private DeviceRendererService deviceRendererService;
    private CreateOtsOmsInput input;

    @BeforeEach
    void setup() {
        deviceRendererService = new DeviceRendererServiceImpl(dataBroker, deviceTransactionManager, openRoadmInterfaces,
                crossConnect, mappingUtils, portMapping);
        input = CreateOtsOmsDataUtils.buildCreateOtsOms();
    }

    @Test
    void testCreateOtsOmsFailsWhenDeviceIsNotMounted() throws OpenRoadmInterfaceException {
        when(deviceTransactionManager.isDeviceMounted(any())).thenReturn(false);
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        assertFalse(result.getSuccess());
        assertEquals("node 1 is not mounted on the controller", result.getResult());
    }

    @Test
    void testCreateOtsOmsFailsWhenDeviceIsMountedWithNoMapping() throws OpenRoadmInterfaceException {
        when(deviceTransactionManager.isDeviceMounted(any())).thenReturn(true);
        when(portMapping.getMapping(any(), any())).thenReturn(null);
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        assertFalse(result.getSuccess());
        assertEquals("Logical Connection point logical point does not exist for node 1", result.getResult());
    }

    @Test
    void testCreateOtsOms() throws OpenRoadmInterfaceException, InterruptedException, ExecutionException {
        when(deviceTransactionManager.isDeviceMounted(any())).thenReturn(true);
        when(mappingUtils.getOpenRoadmVersion(any())).thenReturn(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1);
        Mapping mapping = MountPointUtils.createMapping(input.getNodeId(), input.getLogicalConnectionPoint());
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        assertTrue(result.getSuccess());
    }
}
