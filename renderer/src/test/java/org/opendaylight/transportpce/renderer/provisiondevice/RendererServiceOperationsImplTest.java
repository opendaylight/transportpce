/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.opendaylight.transportpce.renderer.provisiondevice.notification.NotificationSender;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class RendererServiceOperationsImplTest extends AbstractTest {

    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private final DeviceRendererService deviceRenderer = mock(DeviceRendererService.class);
    private final OtnDeviceRendererService otnDeviceRendererService = mock(OtnDeviceRendererService.class);
    private final PortMapping portMapping = mock(PortMapping.class);
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
        this.openRoadmInterfaces = spy(this.openRoadmInterfaces);
    }

    @BeforeEach
    void setUp() throws OpenRoadmInterfaceException {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        doNothing().when(this.openRoadmInterfaces).postEquipmentState(anyString(), anyString(), anyBoolean());
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.olmService = spy(this.olmService);
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(deviceRenderer, otnDeviceRendererService,
                this.olmService, getDataBroker(), new NotificationSender(notificationPublishService), portMapping);
    }

    @Test
    void serviceImplementationTerminationPointAsResourceTtp() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestOutput result =
                this.rendererServiceOperations.serviceImplementation(input, false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourceTtp2() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        doReturn(RpcResultBuilder.failed().buildFuture()).when(this.olmService).servicePowerSetup(any());
        ServiceImplementationRequestOutput result =
                this.rendererServiceOperations.serviceImplementation(input, false).get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourcePp() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.PP_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourceNetwork() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourceClient() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.CLIENT_TOKEN);
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourceNoMapping() throws InterruptedException, ExecutionException {
        String[] interfaceTokens = {
            StringConstants.NETWORK_TOKEN,
            StringConstants.CLIENT_TOKEN,
            StringConstants.TTP_TOKEN,
            StringConstants.PP_TOKEN
        };

        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("failed")
            .setSuccess(false);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());

        for (String tpToken : interfaceTokens) {
            ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(tpToken);
            ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                            false)
                .get();
            assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
        }
    }

    @Test
    void serviceImplementationRollbackAllNecessary() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
//        writePortMapping(input, StringConstants.NETWORK_TOKEN);
        doReturn(RpcResultBuilder.failed().buildFuture()).when(this.olmService).servicePowerSetup(any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder()
                .setPmparameterName("FECUncorrectableBlocks")
                .setPmparameterValue("1")
                .build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
                .setNodeId("node1")
                .setMeasurements(measurementsList)
                .build();
        doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(this.olmService).getPm(any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive2() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder()
                .setPmparameterName("FECUncorrectableBlocks")
                .setPmparameterValue("1")
                .build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
                .setNodeId("node1")
                .setMeasurements(measurementsList)
                .build();

        when(this.olmService.getPm(any())).thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive3() throws InterruptedException, ExecutionException {
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder()
                .setPmparameterName("FECUncorrectableBlocks")
                .setPmparameterValue("1")
                .build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(measurementsList).build();
        GetPmOutput getPmOutput2 = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(new ArrayList<>())
            .build();

        GetPmInput getPmInputZ = createGetPmInput("XPONDER-2-3", StringConstants.NETWORK_TOKEN);
        GetPmInput getPmInputA = createGetPmInput("XPONDER-1-2", StringConstants.NETWORK_TOKEN);

        when(this.olmService.getPm(eq(getPmInputZ))).thenReturn(RpcResultBuilder.success(getPmOutput2).buildFuture());
        when(this.olmService.getPm(eq(getPmInputA))).thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceActive() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        GetPmOutput getPmOutput1 = null;
        when(this.olmService.getPm(any())).thenReturn(RpcResultBuilder.success(getPmOutput1).buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceActive2() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setMeasurements(new ArrayList<>()).build();
        when(this.olmService.getPm(any())).thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive4() throws InterruptedException, ExecutionException {
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder()
                .setPmparameterName("preFECCorrectedErrors")
                .setPmparameterValue("1")
                .build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
                .setNodeId("node1")
                .setMeasurements(measurementsList)
                .build();

        doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(this.olmService).getPm(any());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
            .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive5() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder()
                .setPmparameterName("preFECCorrectedErrors")
                .setPmparameterValue("112000000000d")
                .build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder()
                .setNodeId("node1")
                .setMeasurements(measurementsList)
                .build();

        doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(this.olmService).getPm(any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input,
                false).get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    private GetPmInput createGetPmInput(String nodeId, String tp) {
        return new GetPmInputBuilder()
                .setNodeId(nodeId)
                .setGranularity(PmGranularity._15min)
                .setResourceIdentifier(new ResourceIdentifierBuilder().setResourceName(tp + "-OTU").build())
                .setResourceType(ResourceTypeEnum.Interface)
                .build();
    }
}