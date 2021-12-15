/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.renderer.stub.OlmServiceStub;
import org.opendaylight.transportpce.renderer.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.renderer.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.stub.MountPointServiceStub;
import org.opendaylight.transportpce.test.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210930.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class RendererServiceOperationsImplTest extends AbstractTest {

    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private final DeviceRendererService deviceRenderer = Mockito.mock(DeviceRendererService.class);
    private final OtnDeviceRendererService otnDeviceRendererService = Mockito.mock(OtnDeviceRendererService.class);
    private final PortMapping portMapping = Mockito.mock(PortMapping.class);
    private RendererServiceOperationsImpl rendererServiceOperations;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private TransportpceOlmService olmService;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;

    private void setMountPoint(MountPoint mountPoint) {
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(this.mountPointService, 3000);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
    }

    @Before
    public void setUp() throws OpenRoadmInterfaceException {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        Mockito.doNothing().when(this.openRoadmInterfaces).postEquipmentState(Mockito.anyString(),
            Mockito.anyString(), Mockito.anyBoolean());
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.olmService = Mockito.spy(this.olmService);
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(deviceRenderer,
            otnDeviceRendererService, this.olmService, getDataBroker(), notificationPublishService, portMapping);
    }

    @Test
    public void serviceImplementationTerminationPointAsResourceTtp() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourceTtp2() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        Mockito.doReturn(RpcResultBuilder.failed().buildFuture()).when(this.olmService)
            .servicePowerSetup(Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourcePp() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.PP_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourceNetwork()
            throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourceClient()
            throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.CLIENT_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceImplementationTerminationPointAsResourceNoMapping()
            throws InterruptedException, ExecutionException {

        String[] interfaceTokens = {
            StringConstants.NETWORK_TOKEN,
            StringConstants.CLIENT_TOKEN,
            StringConstants.TTP_TOKEN,
            StringConstants.PP_TOKEN
        };

        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("failed")
            .setSuccess(false);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());

        for (String tpToken : interfaceTokens) {
            ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(tpToken);
            ServiceImplementationRequestOutput result =
                this.rendererServiceOperations.serviceImplementation(input).get();
            Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.getConfigurationResponseCommon().getResponseCode());
        }
    }

    @Test
    public void serviceImplementationRollbackAllNecessary() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
//        writePortMapping(input, StringConstants.NETWORK_TOKEN);
        Mockito.doReturn(RpcResultBuilder.failed().buildFuture()).when(this.olmService)
            .servicePowerSetup(Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }

    private GetPmInput createGetPmInput(String nodeId, String tp) {
        GetPmInputBuilder getPmIpBldr = new GetPmInputBuilder();
        getPmIpBldr.setNodeId(nodeId);
        getPmIpBldr.setGranularity(PmGranularity._15min);
        ResourceIdentifierBuilder rsrcBldr = new ResourceIdentifierBuilder();
        rsrcBldr.setResourceName(tp + "-OTU");
        getPmIpBldr.setResourceIdentifier(rsrcBldr.build());
        getPmIpBldr.setResourceType(ResourceTypeEnum.Interface);
        return getPmIpBldr.build();
    }

    @Test
    public void serviceImplementationServiceInActive() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        Measurements measurements = new MeasurementsBuilder().setPmparameterName("FECUncorrectableBlocks")
            .setPmparameterValue("1").build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(measurementsList).build();
        Mockito.doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(this.olmService)
            .getPm(Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceInActive2() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        Measurements measurements = new MeasurementsBuilder().setPmparameterName("FECUncorrectableBlocks")
            .setPmparameterValue("1").build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(measurementsList).build();

        Mockito.when(this.olmService.getPm(Mockito.any()))
            .thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceInActive3() throws InterruptedException, ExecutionException {

        Measurements measurements = new MeasurementsBuilder().setPmparameterName("FECUncorrectableBlocks")
            .setPmparameterValue("1").build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(measurementsList).build();
        GetPmOutput getPmOutput2 = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(new ArrayList<>()).build();

        GetPmInput getPmInputZ = createGetPmInput("XPONDER-2-3",
            StringConstants.NETWORK_TOKEN);
        GetPmInput getPmInputA = createGetPmInput("XPONDER-1-2",
            StringConstants.NETWORK_TOKEN);

        Mockito.when(this.olmService.getPm(Mockito.eq(getPmInputZ)))
            .thenReturn(RpcResultBuilder.success(getPmOutput2).buildFuture());
        Mockito.when(this.olmService.getPm(Mockito.eq(getPmInputA)))
            .thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceActive() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        GetPmOutput getPmOutput1 = null;
        Mockito.when(this.olmService.getPm(Mockito.any())).thenReturn(RpcResultBuilder.success(getPmOutput1)
            .buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceActive2() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setMeasurements(new ArrayList<>()).build();
        Mockito.when(this.olmService.getPm(Mockito.any())).thenReturn(RpcResultBuilder.success(getPmOutput)
            .buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceInActive4() throws InterruptedException, ExecutionException {

        Measurements measurements = new MeasurementsBuilder().setPmparameterName("preFECCorrectedErrors")
            .setPmparameterValue("1").build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(measurementsList).build();

        Mockito.doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(this.olmService)
            .getPm(Mockito.any());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceInActive5() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        Measurements measurements = new MeasurementsBuilder().setPmparameterName("preFECCorrectedErrors")
            .setPmparameterValue("112000000000d").build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(measurementsList).build();

        Mockito.doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(this.olmService)
            .getPm(Mockito.any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }
}
