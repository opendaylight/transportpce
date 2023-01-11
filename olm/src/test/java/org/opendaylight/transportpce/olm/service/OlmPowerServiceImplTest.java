/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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

public class OlmPowerServiceImplTest  extends AbstractTest {

    private DeviceTransactionManager deviceTransactionManager;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private MappingUtils mappingUtils;
    private OlmPowerService olmPowerService;
    private DataBroker dataBroker;

    @Before
    public void setUp() {
        this.dataBroker = Mockito.mock(DataBroker.class);
        this.powerMgmt = Mockito.mock(PowerMgmtImpl.class);
        this.deviceTransactionManager = Mockito.mock(DeviceTransactionManager.class);
        this.portMapping = Mockito.mock(PortMapping.class);
        this.mappingUtils = Mockito.mock(MappingUtils.class);
        this.openRoadmInterfaces = Mockito.mock(OpenRoadmInterfaces.class);
        this.olmPowerService = new OlmPowerServiceImpl(this.dataBroker, this.powerMgmt,
                this.deviceTransactionManager, this.portMapping, this.mappingUtils, this.openRoadmInterfaces);
    }

    @Test
    public void dummyTest() {
        OlmPowerServiceImpl olmPowerServiceImpl = (OlmPowerServiceImpl) this.olmPowerService;
        olmPowerServiceImpl.init();
        olmPowerServiceImpl.close();
    }

    @Test
    public void testGetPm() {
        Mockito.when(this.mappingUtils.getOpenRoadmVersion(Mockito.anyString()))
                .thenReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1);
        Mockito.when(this.deviceTransactionManager.getDataFromDevice(Mockito.anyString(), Mockito.any(),
                        Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(Optional.of(OlmPowerServiceRpcImplUtil.getCurrentPmList121()));

        GetPmInput input = OlmPowerServiceRpcImplUtil.getGetPmInput();
        GetPmOutput result = this.olmPowerService.getPm(input);
        Assert.assertEquals(
                org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity._15min,
                result.getGranularity());
        Assert.assertEquals(PmNamesEnum.OpticalPowerInput.toString(),
                result.getMeasurements().stream().findFirst().get().getPmparameterName());
        Assert.assertEquals(String.valueOf(3.0),
                result.getMeasurements().stream().findFirst().get().getPmparameterValue());
        Assert.assertEquals("ots-deg1",
                result.getResourceIdentifier().getResourceName());
    }

    @Test
    public void testServicePowerSetupSuccess() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        Mockito.when(this.powerMgmt.setPower(Mockito.any())).thenReturn(true);
        ServicePowerSetupOutput result = this.olmPowerService.servicePowerSetup(input);
        Assert.assertEquals(new ServicePowerSetupOutputBuilder().setResult("Success").build(), result);
        Assert.assertEquals("Success", result.getResult());
    }

    @Test
    public void testServicePowerSetupFailed() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        Mockito.when(this.powerMgmt.setPower(Mockito.any())).thenReturn(false);
        ServicePowerSetupOutput output = this.olmPowerService.servicePowerSetup(input);
        Assert.assertEquals("Failed", output.getResult());
    }

    @Test
    public void testServicePowerTurnDownSuccess() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        Mockito.when(this.powerMgmt.powerTurnDown(Mockito.any())).thenReturn(true);
        ServicePowerTurndownOutput output = this.olmPowerService.servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Success").build(), output);
        Assert.assertEquals("Success", output.getResult());
    }

    @Test
    public void testServicePowerTurnDownFailed() {
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        Mockito.when(this.powerMgmt.powerTurnDown(Mockito.any())).thenReturn(false);
        ServicePowerTurndownOutput output = this.olmPowerService.servicePowerTurndown(input);
        Assert.assertEquals(new ServicePowerTurndownOutputBuilder().setResult("Failed").build(), output);
        Assert.assertEquals("Failed", output.getResult());
    }

    @Test
    public void testServicePowerReset() {
        ServicePowerResetInput input = OlmPowerServiceRpcImplUtil.getServicePowerResetInput();
        ServicePowerResetOutput output = this.olmPowerService.servicePowerReset(input);
        Assert.assertEquals(null, output);
    }
}
