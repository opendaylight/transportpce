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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl710;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion710;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface121;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface710;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmOtnInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmOtnInterface710;
import org.opendaylight.transportpce.renderer.stub.OlmServiceStub;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.renderer.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.renderer.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.stub.MountPointServiceStub;
import org.opendaylight.transportpce.test.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.ServicePathOutputBuilder;
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
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@Ignore
public class RendererServiceOperationsImplTest extends AbstractTest {

    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private RendererServiceOperationsImpl rendererServiceOperations;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private DeviceRendererService deviceRenderer;
    private OtnDeviceRendererService otnDeviceRendererService;
    private PortMapping portMapping;
    private OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private CrossConnect crossConnect;
    private TransportpceOlmService olmService;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;
    private PortMappingVersion710 portMappingVersion710;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;
    private CrossConnectImpl710 crossConnectImpl710;

    private void setMountPoint(MountPoint mountPoint) {
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(this.mountPointService, 3000);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        this.openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(deviceTransactionManager);
        this.openRoadmInterfacesImpl710 = new OpenRoadmInterfacesImpl710(deviceTransactionManager);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMappingVersion22 =
            new PortMappingVersion221(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
            new PortMappingVersion121(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion710 =
            new PortMappingVersion710(getDataBroker(), deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion710, this.portMappingVersion22,
            this.portMappingVersion121);
        OpenRoadmInterface121 openRoadmInterface121 = new OpenRoadmInterface121(portMapping,openRoadmInterfaces);
        OpenRoadmInterface221 openRoadmInterface221 = new OpenRoadmInterface221(portMapping,openRoadmInterfaces);
        OpenRoadmInterface710 openRoadmInterface710 = new OpenRoadmInterface710(portMapping,openRoadmInterfaces);
        OpenRoadmOtnInterface221 openRoadmOTNInterface221 = new OpenRoadmOtnInterface221(portMapping,
            openRoadmInterfaces);
        OpenRoadmOtnInterface710 openRoadmOtnInterface710 = new OpenRoadmOtnInterface710(portMapping,
            openRoadmInterfaces);
        this.openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(this.mappingUtils,openRoadmInterface121,
            openRoadmInterface221, openRoadmInterface710, openRoadmOTNInterface221, openRoadmOtnInterface710);
        this.crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        this.crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(deviceTransactionManager, this.mappingUtils, this.crossConnectImpl121,
            this.crossConnectImpl221, this.crossConnectImpl710);
    }

    @Before
    public void setUp() throws OpenRoadmInterfaceException {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        this.deviceRenderer = new DeviceRendererServiceImpl(getDataBroker(), this.deviceTransactionManager,
            openRoadmInterfaceFactory, openRoadmInterfaces, crossConnect, portMapping, null);
        this.otnDeviceRendererService = new OtnDeviceRendererServiceImpl(openRoadmInterfaceFactory, this.crossConnect,
            openRoadmInterfaces, this.deviceTransactionManager, null);
        Mockito.doNothing().when(this.openRoadmInterfaces).postEquipmentState(Mockito.anyString(),
            Mockito.anyString(), Mockito.anyBoolean());
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.olmService = Mockito.spy(this.olmService);
        this.deviceRenderer = Mockito.spy(this.deviceRenderer);
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(this.deviceRenderer,
            this.otnDeviceRendererService, this.olmService, getDataBroker(), notificationPublishService, null);

        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        Mockito.doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(Mockito.any(),
            Mockito.any());
    }

    @Test
    public void serviceImplementationTerminationPointAsResourceTtp() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        writePortMapping(input, StringConstants.TTP_TOKEN);
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
        writePortMapping(input, StringConstants.TTP_TOKEN);
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
        writePortMapping(input, StringConstants.PP_TOKEN);
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
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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
     //       .buildServiceImplementationRequestInputTerminationPointResource(OpenRoadmInterfacesImpl.CLIENT_TOKEN);
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.CLIENT_TOKEN);
     //   writePortMapping(input, OpenRoadmInterfacesImpl.CLIENT_TOKEN);
        writePortMapping(input, StringConstants.CLIENT_TOKEN);
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

    private void writePortMapping(ServiceImplementationRequestInput input, String tpToken) {
        MountPointUtils.writeMapping(
            input.getServiceAEnd().getNodeId(),
            input.getServiceAEnd().getNodeId() + "-" + tpToken,
            this.deviceTransactionManager
        );
        MountPointUtils.writeMapping(
            input.getServiceZEnd().getNodeId(),
            input.getServiceZEnd().getNodeId() + "-"
                + input.getServiceAEnd().getNodeId() + "-" + tpToken,
            this.deviceTransactionManager
        );
        MountPointUtils.writeMapping(
            input.getServiceAEnd().getNodeId(),
            input.getServiceAEnd().getNodeId() + "-"
                + input.getServiceZEnd().getNodeId() + "-" + tpToken,
            this.deviceTransactionManager
        );
        MountPointUtils.writeMapping(
            input.getServiceZEnd().getNodeId(),
            input.getServiceZEnd().getNodeId() + "-" + tpToken,
            this.deviceTransactionManager
        );
    }

    @Test
    public void serviceImplementationRollbackAllNecessary() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
        Measurements measurements = new MeasurementsBuilder().setPmparameterName("FECUncorrectableBlocks")
            .setPmparameterValue("1").build();
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(measurements);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(measurementsList).build();
        GetPmOutput getPmOutput2 = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(new ArrayList<>()).build();

        Mockito.when(this.olmService.getPm(Mockito.any()))
            .thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceInActive3() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceActive() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(new ArrayList<>()).build();
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
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input).get();
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationServiceInActive5() throws InterruptedException, ExecutionException {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        writePortMapping(input, StringConstants.NETWORK_TOKEN);
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
