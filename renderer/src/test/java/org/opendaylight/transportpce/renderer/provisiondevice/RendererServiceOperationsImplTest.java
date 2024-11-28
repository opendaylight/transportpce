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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.provisiondevice.notification.NotificationSender;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.history.History;
import org.opendaylight.transportpce.renderer.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.renderer.utils.ServiceDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPm;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndown;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@ExtendWith(MockitoExtension.class)
public class RendererServiceOperationsImplTest {

    @Mock
    private DeviceRendererService deviceRenderer;
    @Mock
    private OtnDeviceRendererService otnDeviceRendererService;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private PortMapping portMapping;
    @Mock
    private RpcService rpcService;
    @Mock
    private ServicePowerSetup servicePowerSetup;
    @Mock
    private ServicePowerTurndown servicePowerTurndown;
    @Mock
    private GetPm getPm;
    @Mock
    private Nodes node;
    @Mock
    private NodeInfo nodeInfo;
    private RendererServiceOperationsImpl rendererServiceOperations;


    @BeforeEach
    void setUp() throws OpenRoadmInterfaceException {
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.rendererServiceOperations = new RendererServiceOperationsImpl(deviceRenderer, otnDeviceRendererService,
                dataBroker, new NotificationSender(notificationPublishService), portMapping, rpcService);
    }

    @Test
    void serviceImplementationTerminationPointAsResourceTtp() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        when(deviceRenderer.setupServicePath(any(), any(), any()))
                .thenReturn(new ServicePathOutputBuilder().setResult("success").setSuccess(true).build());

        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        when(rpcService.getRpc(GetPm.class)).thenReturn(getPm);
        doReturn(RpcResultBuilder
                .success(new ServicePowerSetupOutputBuilder().setResult(ResponseCodes.SUCCESS_RESULT).build())
                .buildFuture()).when(servicePowerSetup).invoke(any());
        doReturn(RpcResultBuilder.success(new GetPmOutputBuilder().setNodeId("node id").build()).buildFuture())
                .when(getPm).invoke(any());
        when(portMapping.getNode(any())).thenReturn(node);
        when(node.getNodeInfo()).thenReturn(nodeInfo);
        when(nodeInfo.getNodeType()).thenReturn(NodeTypes.Xpdr);

        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourceTtp2() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.TTP_TOKEN);
        ServicePathOutput mockServicePathOutput = new ServicePathOutputBuilder()
                .setResult("success")
                .setSuccess(true)
                .build();
        doReturn(mockServicePathOutput).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        ServicePowerSetupOutput mockServicePowerSetupOutput = new ServicePowerSetupOutputBuilder()
                .setResult("result")
                .build();
        doReturn(RpcResultBuilder.failed().withResult(mockServicePowerSetupOutput).buildFuture())
            .when(servicePowerSetup).invoke(any());

        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        doReturn(RpcResultBuilder
                .success(new ServicePowerTurndownOutputBuilder()
                        .setResult("result")
                        .build())
                .buildFuture())
            .when(servicePowerTurndown).invoke(any());
        when(this.deviceRenderer.rendererRollback(any(History.class)))
            .thenReturn(new RendererRollbackOutputBuilder().setSuccess(true).build());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationTerminationPointAsResourceNoMapping() throws InterruptedException, ExecutionException {
        // when no mapping available, 100GE between transponders must be implemented

        when(deviceRenderer.setupServicePath(any(), any(), any()))
                .thenReturn(new ServicePathOutputBuilder().setResult("success").setSuccess(true).build());

        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        when(rpcService.getRpc(GetPm.class)).thenReturn(getPm);
        doReturn(RpcResultBuilder
                .success(new ServicePowerSetupOutputBuilder().setResult(ResponseCodes.SUCCESS_RESULT).build())
                .buildFuture()).when(servicePowerSetup).invoke(any());
        doReturn(RpcResultBuilder.success(new GetPmOutputBuilder().setNodeId("node id").build()).buildFuture())
                .when(getPm).invoke(any());
        when(portMapping.getNode(any())).thenReturn(node);
        when(node.getNodeInfo()).thenReturn(nodeInfo);
        when(nodeInfo.getNodeType()).thenReturn(NodeTypes.Xpdr);
        String[] interfaceTokens = { StringConstants.NETWORK_TOKEN, StringConstants.CLIENT_TOKEN,
            StringConstants.TTP_TOKEN, StringConstants.PP_TOKEN };
        for (String tpToken : interfaceTokens) {
            ServiceImplementationRequestInput input = ServiceDataUtils
                    .buildServiceImplementationRequestInputTerminationPointResource(tpToken);
            ServiceImplementationRequestOutput result = this.rendererServiceOperations
                    .serviceImplementation(input, false).get();
            assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
        }
    }

    @Test
    void serviceImplementationRollbackAllNecessary() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        when(deviceRenderer.setupServicePath(any(), any(), any()))
                .thenReturn(new ServicePathOutputBuilder().setResult("success").setSuccess(true).build());
        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        doReturn(RpcResultBuilder
                .success(new ServicePowerSetupOutputBuilder().setResult(ResponseCodes.RESPONSE_FAILED).build())
                .buildFuture()).when(servicePowerSetup).invoke(any());
        doReturn(RpcResultBuilder
                .success(new ServicePowerTurndownOutputBuilder().setResult(ResponseCodes.SUCCESS_RESULT).build())
                .buildFuture()).when(servicePowerTurndown).invoke(any());
        when(deviceRenderer.rendererRollback(any(History.class)))
                .thenReturn(new RendererRollbackOutputBuilder().setSuccess(true).build());

        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Disabled("Disabled until we understand the author objective...")
    @Test
    void serviceImplementationServiceInActive() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder().setPmparameterName("FECUncorrectableBlocks")
                .setPmparameterValue("1").build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(measurementsList).build();
        doReturn(RpcResultBuilder.success(getPmOutput).buildFuture()).when(getPm).invoke(any());
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Disabled("Disabled until we understand the author objective...")
    @Test
    void serviceImplementationServiceInActive3() throws InterruptedException, ExecutionException {
        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder().setPmparameterName("FECUncorrectableBlocks")
                .setPmparameterValue("1").build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(measurementsList).build();
        GetPmOutput getPmOutput2 = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(new ArrayList<>())
                .build();

        GetPmInput getPmInputZ = createGetPmInput("XPONDER-2-3", StringConstants.NETWORK_TOKEN);
        GetPmInput getPmInputA = createGetPmInput("XPONDER-1-2", StringConstants.NETWORK_TOKEN);

        when(getPm.invoke(eq(getPmInputZ))).thenReturn(RpcResultBuilder.success(getPmOutput2).buildFuture());
        when(getPm.invoke(eq(getPmInputA))).thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        ServicePathOutputBuilder mockOutputBuilder = new ServicePathOutputBuilder().setResult("success")
                .setSuccess(true);
        doReturn(mockOutputBuilder.build()).when(this.deviceRenderer).setupServicePath(any(), any(), any());
        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceActive() throws InterruptedException, ExecutionException {
        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        when(deviceRenderer.setupServicePath(any(), any(), any()))
                .thenReturn(new ServicePathOutputBuilder().setResult("success").setSuccess(true).build());

        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        when(rpcService.getRpc(GetPm.class)).thenReturn(getPm);
        doReturn(RpcResultBuilder
                .success(new ServicePowerSetupOutputBuilder().setResult(ResponseCodes.SUCCESS_RESULT).build())
                .buildFuture()).when(servicePowerSetup).invoke(any());
        GetPmOutput getPmOutput1 = null;
        when(getPm.invoke(any())).thenReturn(RpcResultBuilder.success(getPmOutput1).buildFuture());

        when(portMapping.getNode(any())).thenReturn(node);
        when(node.getNodeInfo()).thenReturn(nodeInfo);
        when(nodeInfo.getNodeType()).thenReturn(NodeTypes.Xpdr);

        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive4() throws InterruptedException, ExecutionException {
        when(deviceRenderer.setupServicePath(any(), any(), any()))
                .thenReturn(new ServicePathOutputBuilder().setResult("success").setSuccess(true).build());

        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        when(rpcService.getRpc(GetPm.class)).thenReturn(getPm);
        doReturn(RpcResultBuilder
                .success(new ServicePowerSetupOutputBuilder().setResult(ResponseCodes.SUCCESS_RESULT).build())
                .buildFuture()).when(servicePowerSetup).invoke(any());

        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(
                new MeasurementsBuilder().setPmparameterName("preFECCorrectedErrors").setPmparameterValue("1").build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(measurementsList).build();
        when(getPm.invoke(any())).thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        when(portMapping.getNode(any())).thenReturn(node);
        when(node.getNodeInfo()).thenReturn(nodeInfo);
        when(nodeInfo.getNodeType()).thenReturn(NodeTypes.Xpdr);

        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceImplementationServiceInActive5() throws InterruptedException, ExecutionException {
        when(deviceRenderer.setupServicePath(any(), any(), any()))
                .thenReturn(new ServicePathOutputBuilder().setResult("success").setSuccess(true).build());
        when(rpcService.getRpc(ServicePowerSetup.class)).thenReturn(servicePowerSetup);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        when(rpcService.getRpc(GetPm.class)).thenReturn(getPm);
        doReturn(RpcResultBuilder
                .success(new ServicePowerSetupOutputBuilder().setResult(ResponseCodes.SUCCESS_RESULT).build())
                .buildFuture()).when(servicePowerSetup).invoke(any());

        List<Measurements> measurementsList = new ArrayList<Measurements>();
        measurementsList.add(new MeasurementsBuilder().setPmparameterName("preFECCorrectedErrors")
                .setPmparameterValue("112000000000d").build());
        GetPmOutput getPmOutput = new GetPmOutputBuilder().setNodeId("node1").setMeasurements(measurementsList).build();
        when(getPm.invoke(any())).thenReturn(RpcResultBuilder.success(getPmOutput).buildFuture());
        when(portMapping.getNode(any())).thenReturn(node);
        when(node.getNodeInfo()).thenReturn(nodeInfo);
        when(nodeInfo.getNodeType()).thenReturn(NodeTypes.Xpdr);
        when(rpcService.getRpc(ServicePowerTurndown.class)).thenReturn(servicePowerTurndown);
        doReturn(RpcResultBuilder
                .success(new ServicePowerTurndownOutputBuilder()
                        .setResult("result")
                        .build())
                .buildFuture())
            .when(servicePowerTurndown).invoke(any());
        when(this.deviceRenderer.rendererRollback(any(History.class)))
            .thenReturn(new RendererRollbackOutputBuilder().setSuccess(true).build());

        ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(StringConstants.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input, false)
                .get();
        assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());
    }

    private GetPmInput createGetPmInput(String nodeId, String tp) {
        return new GetPmInputBuilder().setNodeId(nodeId).setGranularity(PmGranularity._15min)
                .setResourceIdentifier(new ResourceIdentifierBuilder().setResourceName(tp + "-OTU").build())
                .setResourceType(ResourceTypeEnum.Interface).build();
    }
}