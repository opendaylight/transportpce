/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.power.PowerMgmtImpl;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;

class OlmPowerServiceImplTest  extends AbstractTest {

    private DeviceTransactionManager deviceTransactionManager;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private MappingUtils mappingUtils;
    private OlmPowerService olmPowerService;
    private DataBroker dataBroker;

    @BeforeEach
    void setUp() {
        this.dataBroker = mock(DataBroker.class);
        this.powerMgmt = mock(PowerMgmtImpl.class);
        this.deviceTransactionManager = mock(DeviceTransactionManager.class);
        this.portMapping = mock(PortMapping.class);
        this.mappingUtils = mock(MappingUtils.class);
        this.openRoadmInterfaces = mock(OpenRoadmInterfaces.class);
        this.olmPowerService = new OlmPowerServiceImpl(this.dataBroker, this.powerMgmt, this.deviceTransactionManager,
                this.portMapping, this.mappingUtils, this.openRoadmInterfaces);
    }

    @Test
    void testGetPm() {
        when(this.mappingUtils.getOpenRoadmVersion(anyString()))
            .thenReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1);
        when(this.deviceTransactionManager.getDataFromDevice(anyString(), any(), any(), anyLong(), any()))
            .thenReturn(Optional.of(OlmPowerServiceRpcImplUtil.getCurrentPmList121()));

        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        GetPmOutput result = this.olmPowerService.getPm(input);
        assertEquals(
            org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity._15min,
            result.getGranularity());
        assertEquals(
            PmNamesEnum.OpticalPowerInput.toString(),
            result.getMeasurements().stream().findFirst().orElseThrow().getPmparameterName());
        assertEquals(
            String.valueOf(3.0),
            result.getMeasurements().stream().findFirst().orElseThrow().getPmparameterValue());
        assertEquals(
            "ots-deg1",
            result.getResourceIdentifier().getResourceName());
    }

    @Test
    void testServicePowerSetupSuccess() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        when(this.powerMgmt.setPower(any(), any())).thenReturn(true);
        ServicePowerSetupOutput result = this.olmPowerService.servicePowerSetup(input);
        assertEquals(new ServicePowerSetupOutputBuilder().setResult("Success").build(), result);
        assertEquals("Success", result.getResult());
    }

    @Test
    void testServicePowerSetupFailed() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        when(this.powerMgmt.setPower(any(), any())).thenReturn(false);
        ServicePowerSetupOutput output = this.olmPowerService.servicePowerSetup(input);
        assertEquals("OLM power setup failed (OLM power setup failed due to an unknown error.)", output.getResult());
    }

    @Test
    void testServicePowerTurnDownSuccess() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        when(this.powerMgmt.powerTurnDown(any())).thenReturn(true);
        ServicePowerTurndownOutput output = this.olmPowerService.servicePowerTurndown(input);
        assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Success").build(), output);
        assertEquals("Success", output.getResult());
    }

    @Test
    void testServicePowerTurnDownFailed() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        when(this.powerMgmt.powerTurnDown(any())).thenReturn(false);
        ServicePowerTurndownOutput output = this.olmPowerService.servicePowerTurndown(input);
        assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Failed").build(), output);
        assertEquals("Failed", output.getResult());
    }

    @Test
    void testServicePowerReset() {
        ServicePowerResetInput input = OlmPowerServiceRpcImplUtil.getServicePowerResetInput();
        ServicePowerResetOutput output = this.olmPowerService.servicePowerReset(input);
        assertEquals(null, output);
    }
}
