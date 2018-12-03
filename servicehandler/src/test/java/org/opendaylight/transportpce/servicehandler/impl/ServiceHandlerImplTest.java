/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.stub.StubRendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceHandlerImplTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHandlerImplTest.class);

    private PathComputationService pathComputationService;
    private RendererServiceOperations rendererServiceOperations;
    private NetworkModelWavelengthService networkModelWavelengthService;
    private ServicehandlerImpl serviceHandler;

    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperationsMock;

    @Mock
    private PCEServiceWrapper pceServiceWrapperMock;

    @Mock
    private RendererServiceOperations rendererServiceOperationsMock;

    @Mock
    private ComplianceCheckResult complianceCheckResultMock;

    @Mock
    private Optional<Services> servicesOptionalMock;

    @InjectMocks
    private ServicehandlerImpl serviceHandlerImplMock;

    @Before
    public void setUp() {
        this.serviceHandler = new ServicehandlerImpl(getDataBroker(), this.pathComputationService,
                this.rendererServiceOperations, this.networkModelWavelengthService);
        this.serviceHandlerImplMock = new ServicehandlerImpl(getDataBroker(), this.pathComputationService, null, null);
        MockitoAnnotations.initMocks(this);
    }

    public ServiceHandlerImplTest() throws Exception {
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.pathComputationService = new PathComputationServiceImpl(getDataBroker(), notificationPublishService);
        PceTestUtils.writeTopologyIntoDataStore(getDataBroker(), getDataStoreContextUtil(),
                "topologyData/NW-simple-topology.xml");
        this.rendererServiceOperations =
                new StubRendererServiceOperations(this.networkModelWavelengthService, getDataBroker());
    }

    @Test
    public void testCreateServiceValid() throws ExecutionException, InterruptedException {

        ServiceCreateInput serviceInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();

        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceInput, true))
            .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("successful").build();
        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon2).build());
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(serviceInput.getServiceName(),
                State.InService, State.InService)).thenReturn(OperationResult.ok("successful"));

        Future<RpcResult<ServiceCreateOutput>> output0 = this.serviceHandlerImplMock.serviceCreate(serviceInput);
        Assert.assertNotNull(output0);
        Assert.assertTrue(output0.get().isSuccessful());
        Assert.assertEquals(output0.get().getResult(),
                ModelMappingUtils.createCreateServiceReply(serviceInput, ResponseCodes.FINAL_ACK_YES,
                        "Service rendered successfully !", ResponseCodes.RESPONSE_OK).get().getResult());
        Assert.assertEquals(0, output0.get().getErrors().size());
    }

    @Test
    public void createTempServiceHandlerServiceCreateValid() throws ExecutionException, InterruptedException {
        TempServiceCreateInput serviceInput = ServiceDataUtils.buildTempServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput =
                new PathComputationRequestOutputBuilder(PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createTempService(any(TempServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("successful").build();
        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon2).build());
        Mockito.when(this.serviceDataStoreOperationsMock.modifyTempService(serviceInput.getCommonId(), State.InService,
                State.InService)).thenReturn(OperationResult.ok("successful"));
        Future<RpcResult<TempServiceCreateOutput>> output0 =
                this.serviceHandlerImplMock.tempServiceCreate(serviceInput);
        Assert.assertNotNull(output0);
        Assert.assertTrue(output0.get().isSuccessful());
        Assert.assertEquals(output0.get().getResult(), ModelMappingUtils.createCreateServiceReply(serviceInput,
                ResponseCodes.FINAL_ACK_YES, "Service rendered successfully !", ResponseCodes.RESPONSE_OK).get()
                .getResult());
        Assert.assertEquals(0, output0.get().getErrors().size());
    }

    @Test
    public void createServiceHandlerInvalidIfNameIsEmpty() throws ExecutionException, InterruptedException {
        ServiceCreateInput emptyServiceNameInput = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder(emptyServiceNameInput);
        emptyServiceNameInput = builtInput.setServiceName("").build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(emptyServiceNameInput).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(emptyServiceNameInput, ResponseCodes.FINAL_ACK_YES,
                        "Service Name (common-id for Temp service) is not set", ResponseCodes.RESPONSE_FAILED).get()
                        .getResult());
    }

    @Test
    public void createServiceHandlerInvalidIfNameIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateInput nullServiceNameInput = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder(nullServiceNameInput);
        nullServiceNameInput = builtInput.setServiceName(null).build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(nullServiceNameInput).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(nullServiceNameInput, ResponseCodes.FINAL_ACK_YES,
                        "Service Name (common-id for Temp service) is not set", ResponseCodes.RESPONSE_FAILED).get()
                        .getResult());
    }

    @Test
    public void createTempServiceHandlerInvalidIfCommonIdIsEmpty() throws ExecutionException, InterruptedException {
        TempServiceCreateInput emptyServiceNameInput = ServiceDataUtils.buildTempServiceCreateInput();
        TempServiceCreateInputBuilder builtInput = new TempServiceCreateInputBuilder(emptyServiceNameInput);
        emptyServiceNameInput = builtInput.setCommonId("").build();
        Assert.assertEquals(this.serviceHandler.tempServiceCreate(emptyServiceNameInput).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(emptyServiceNameInput, ResponseCodes.FINAL_ACK_YES,
                        "Service Name (common-id for Temp service) is not set", ResponseCodes.RESPONSE_FAILED).get()
                        .getResult());
    }

    @Test
    public void createTempServiceHandlerInvalidIfCommonIdIsNull() throws ExecutionException, InterruptedException {
        TempServiceCreateInput emptyServiceNameInput = ServiceDataUtils.buildTempServiceCreateInput();
        TempServiceCreateInputBuilder builtInput = new TempServiceCreateInputBuilder(emptyServiceNameInput);
        emptyServiceNameInput = builtInput.setCommonId(null).build();
        Assert.assertEquals(this.serviceHandler.tempServiceCreate(emptyServiceNameInput).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(emptyServiceNameInput, ResponseCodes.FINAL_ACK_YES,
                                "Service Name (common-id for Temp service) is not set", ResponseCodes.RESPONSE_FAILED)
                        .get().getResult());
    }

    @Test
    public void createServiceHnadlerInvalidIfConTypeIsEmpty() throws ExecutionException, InterruptedException {
        ServiceCreateInput emptyConTypeInput = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder(emptyConTypeInput);
        emptyConTypeInput = builtInput.setConnectionType(null).build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(emptyConTypeInput).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(emptyConTypeInput, ResponseCodes.FINAL_ACK_YES,
                        "Service ConnectionType is not set", ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerInvalidIfSdncRequestHeaderNull() throws ExecutionException, InterruptedException {
        ServiceCreateInput emptySdncRequestHeader = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(emptySdncRequestHeader);
        emptySdncRequestHeader = buildInput.setSdncRequestHeader(null).build();
        ServiceCreateOutput result = this.serviceHandler.serviceCreate(emptySdncRequestHeader).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
    }

    @Test
    public void createServiceHandlerInvalidIfRequestIdEmpty() throws ExecutionException, InterruptedException {
        ServiceCreateInput emptyRequestId = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(emptyRequestId);
        emptyRequestId = buildInput
                .setSdncRequestHeader(
                        new SdncRequestHeaderBuilder(emptyRequestId.getSdncRequestHeader()).setRequestId("").build())
                .build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(emptyRequestId).get().getResult(),
                ModelMappingUtils
                        .createCreateServiceReply(emptyRequestId, ResponseCodes.FINAL_ACK_YES,
                                "Service sdncRequestHeader 'request-id' is not set", ResponseCodes.RESPONSE_FAILED)
                        .get().getResult());
    }

    @Test
    public void createServiceHandlerInvalidIfRequestIdNull() throws ExecutionException, InterruptedException {
        ServiceCreateInput nullRequestId = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(nullRequestId);
        nullRequestId = buildInput
                .setSdncRequestHeader(
                        new SdncRequestHeaderBuilder(nullRequestId.getSdncRequestHeader()).setRequestId(null).build())
                .build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(nullRequestId).get().getResult(),
                ModelMappingUtils
                        .createCreateServiceReply(nullRequestId, ResponseCodes.FINAL_ACK_YES,
                                "Service sdncRequestHeader 'request-id' is not set", ResponseCodes.RESPONSE_FAILED)
                        .get().getResult());
    }

    @Test
    public void serviceHandlerInvalidServiceActionIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateInput emptyServiceAction = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(emptyServiceAction);
        emptyServiceAction = buildInput.setSdncRequestHeader(
                new SdncRequestHeaderBuilder(emptyServiceAction.getSdncRequestHeader()).setRpcAction(null).build())
                .build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(emptyServiceAction).get().getResult(),
                ModelMappingUtils
                        .createCreateServiceReply(emptyServiceAction, ResponseCodes.FINAL_ACK_YES,
                                "Service sndc-request-header 'rpc-action' is not set ", ResponseCodes.RESPONSE_FAILED)
                        .get().getResult());
    }

    @Test
    public void serviceHandlerInvalidServiceActionIsNotCreate() throws ExecutionException, InterruptedException {
        ServiceCreateInput notCreateServiceAction = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notCreateServiceAction);
        notCreateServiceAction = buildInput
                .setSdncRequestHeader(new SdncRequestHeaderBuilder(notCreateServiceAction.getSdncRequestHeader())
                        .setRpcAction(RpcActions.ServiceFeasibilityCheck).build())
                .build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(notCreateServiceAction).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notCreateServiceAction, ResponseCodes.FINAL_ACK_YES,
                        "Service sdncRequestHeader rpc-action '"
                                + notCreateServiceAction.getSdncRequestHeader().getRpcAction() + "' not equal to '"
                                + RpcActions.ServiceCreate.name() + "'",
                        ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceAEndIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateInput notValidServiceAEnd = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notValidServiceAEnd);
        notValidServiceAEnd = buildInput.setServiceAEnd(null).build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(notValidServiceAEnd).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notValidServiceAEnd, ResponseCodes.FINAL_ACK_YES,
                        "SERVICEAEND is not set", ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceZEndIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateInput notValidServiceAEnd = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notValidServiceAEnd);
        notValidServiceAEnd = buildInput.setServiceZEnd(null).build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(notValidServiceAEnd).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notValidServiceAEnd, ResponseCodes.FINAL_ACK_YES,
                        "SERVICEZEND is not set", ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceAEndRateIsNull() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandler =
                new ServicehandlerImpl(getDataBroker(), this.pathComputationService, null, null);
        ServiceCreateInput notValidServiceAEnd = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notValidServiceAEnd);
        notValidServiceAEnd = buildInput.setServiceAEnd(ServiceDataUtils.getServiceAEndBuild().setServiceRate(null)
                .build()).build();
        Assert.assertEquals(servicehandler.serviceCreate(notValidServiceAEnd).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notValidServiceAEnd, ResponseCodes.FINAL_ACK_YES,
                        "Service " + ServiceEndpointType.SERVICEAEND + " rate is not set",
                        ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceZEndRateIsNull() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandler =
                new ServicehandlerImpl(getDataBroker(), this.pathComputationService, null, null);
        ServiceCreateInput notValidServiceZEnd = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notValidServiceZEnd);
        notValidServiceZEnd = buildInput.setServiceZEnd(ServiceDataUtils.getServiceZEndBuild().setServiceRate(null)
                .build()).build();
        Assert.assertEquals(servicehandler.serviceCreate(notValidServiceZEnd).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notValidServiceZEnd, ResponseCodes.FINAL_ACK_YES,
                        "Service " + ServiceEndpointType.SERVICEZEND + " rate is not set",
                        ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceAEndClliIsNull()
            throws ExecutionException, InterruptedException {
        ServiceCreateInput notValidServiceAEnd = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notValidServiceAEnd);
        notValidServiceAEnd = buildInput.setServiceAEnd(ServiceDataUtils.getServiceAEndBuild().setClli(null)
                .build()).build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(notValidServiceAEnd).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notValidServiceAEnd, ResponseCodes.FINAL_ACK_YES,
                        "Service" + ServiceEndpointType.SERVICEAEND + " clli format is not set",
                        ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceZEndClliIsNull()
            throws ExecutionException, InterruptedException, InvocationTargetException, IllegalAccessException {
        ServiceCreateInput notValidServiceZEnd = ServiceDataUtils.buildServiceCreateInput();
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(notValidServiceZEnd);
        notValidServiceZEnd = buildInput.setServiceZEnd(ServiceDataUtils.getServiceZEndBuild().setClli(null).build())
                .build();
        Assert.assertEquals(this.serviceHandler.serviceCreate(notValidServiceZEnd).get().getResult(),
                ModelMappingUtils.createCreateServiceReply(notValidServiceZEnd, ResponseCodes.FINAL_ACK_YES,
                        "Service" + ServiceEndpointType.SERVICEZEND + " clli format is not set",
                        ResponseCodes.RESPONSE_FAILED).get().getResult());
    }

    @Test
    public void createServiceHandlerNotValidServiceAEndAttributes()
            throws ExecutionException, InterruptedException, InvocationTargetException, IllegalAccessException {
        HashMap<String, Object> notValidData = new HashMap<>();
        notValidData.put("setServiceRate", 0L);
        notValidData.put("setServiceFormat", null);
        notValidData.put("setClli", "");
        notValidData.put("setTxDirection", null);
        notValidData.put("setRxDirection", null);
        for (Method method : org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
                .ServiceAEndBuilder.class.getDeclaredMethods()) {
            if (notValidData.containsKey(method.getName())) {
                ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(ServiceDataUtils
                        .buildServiceCreateInput());
                org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
                    .ServiceAEndBuilder serviceAEndBuilder = ServiceDataUtils.getServiceAEndBuild();
                method.invoke(serviceAEndBuilder, notValidData.get(method.getName()));
                ServiceCreateOutput result = this.serviceHandler
                        .serviceCreate(buildInput.setServiceAEnd(serviceAEndBuilder.build()).build()).get().getResult();
                Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                        ResponseCodes.FINAL_ACK_YES);
                Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(),
                        ResponseCodes.RESPONSE_FAILED);
            }
        }
    }

    @Test
    public void createServiceHandlerNotValidServiceZEndAttributes()
            throws ExecutionException, InterruptedException, InvocationTargetException, IllegalAccessException {
        HashMap<String, Object> notValidData = new HashMap<>();
        notValidData.put("setServiceRate", 0L);
        notValidData.put("setServiceFormat", null);
        notValidData.put("setClli", "");
        notValidData.put("setTxDirection", null);
        notValidData.put("setRxDirection", null);
        for (Method method : org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
                .ServiceZEndBuilder.class.getDeclaredMethods()) {
            if (notValidData.containsKey(method.getName())) {
                ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(ServiceDataUtils
                        .buildServiceCreateInput());
                org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
                    .ServiceZEndBuilder serviceZEndBuilder = ServiceDataUtils.getServiceZEndBuild();
                method.invoke(serviceZEndBuilder, notValidData.get(method.getName()));
                ServiceCreateOutput result = this.serviceHandler
                        .serviceCreate(buildInput.setServiceZEnd(serviceZEndBuilder.build()).build()).get().getResult();
                Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                        ResponseCodes.FINAL_ACK_YES);
                Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(),
                        ResponseCodes.RESPONSE_FAILED);
            }
        }
    }

    @Test
    public void createServiceHandlerNotValidTxDirectionPort()
            throws InvocationTargetException, IllegalAccessException, ExecutionException, InterruptedException {
        List<String> invalidData = Arrays.asList(null, "");
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        for (Method method : PortBuilder.class.getMethods()) {
            if (method.getName().startsWith("set") && !method.getName().contains("Slot")) {
                for (Object data : invalidData) {
                    PortBuilder portBuilder = new PortBuilder(
                            serviceCreateInput.getServiceAEnd().getTxDirection().getPort());
                    method.invoke(portBuilder, data);
                    ServiceCreateOutput result = getTxDirectionPortServiceCreateOutput(portBuilder.build(),
                            serviceCreateInput.getServiceAEnd().getTxDirection().getLgx());
                    Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                            ResponseCodes.FINAL_ACK_YES);
                    Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(),
                            ResponseCodes.RESPONSE_FAILED);
                }
            }
        }
    }

    @Test
    public void createServiceHandlerTxDirectionPortIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateOutput result = getTxDirectionPortServiceCreateOutput(null,
            ServiceDataUtils.buildServiceCreateInput().getServiceAEnd().getTxDirection().getLgx());
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void createServiceHandlerNotValidTxDirectionLgx()
            throws InvocationTargetException, IllegalAccessException, ExecutionException, InterruptedException {
        List<String> invalidData = Arrays.asList(null, "");
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        for (Method method : LgxBuilder.class.getMethods()) {
            if (method.getName().startsWith("set")) {
                for (Object data : invalidData) {
                    LgxBuilder lgxBuilder = new LgxBuilder(
                            serviceCreateInput.getServiceAEnd().getTxDirection().getLgx());
                    method.invoke(lgxBuilder, data);
                    ServiceCreateOutput result = getTxDirectionPortServiceCreateOutput(
                            serviceCreateInput.getServiceAEnd().getTxDirection().getPort(), lgxBuilder.build());
                    Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                            ResponseCodes.FINAL_ACK_YES);
                    Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(),
                            ResponseCodes.RESPONSE_FAILED);
                }
            }
        }
    }

    @Test
    public void createServiceHandlerTxDirectionLgxIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateOutput result = getTxDirectionPortServiceCreateOutput(
            ServiceDataUtils.buildServiceCreateInput().getServiceAEnd().getTxDirection().getPort(), null);
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    private ServiceCreateOutput getTxDirectionPortServiceCreateOutput(Port port, Lgx lgx)
            throws InterruptedException, ExecutionException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceAEndBuilder serviceAEndBuilder = ServiceDataUtils.getServiceAEndBuild();
        TxDirectionBuilder txDirectionBuilder = new TxDirectionBuilder(
                serviceCreateInput.getServiceAEnd().getTxDirection());
        txDirectionBuilder.setPort(port);
        txDirectionBuilder.setLgx(lgx);
        serviceAEndBuilder.setTxDirection(txDirectionBuilder.build());
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(serviceCreateInput);
        this.serviceHandler = new ServicehandlerImpl(getDataBroker(), this.pathComputationService, null, null);
        return serviceHandler.serviceCreate(buildInput.setServiceAEnd(serviceAEndBuilder.build()).build()).get()
                .getResult();
    }

    @Test
    public void createServiceHandlerNotValidRxDirectionPort()
            throws InvocationTargetException, IllegalAccessException, ExecutionException, InterruptedException {
        List<String> invalidData = Arrays.asList(null, "");
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        for (Method method : PortBuilder.class.getMethods()) {
            if (method.getName().startsWith("set") && !method.getName().contains("Slot")) {
                for (Object data : invalidData) {
                    PortBuilder portBuilder = new PortBuilder(
                            serviceCreateInput.getServiceAEnd().getRxDirection().getPort());
                    method.invoke(portBuilder, data);
                    ServiceCreateOutput result = getRxDirectionPortServiceCreateOutput(portBuilder.build(),
                            serviceCreateInput.getServiceAEnd().getRxDirection().getLgx());
                    Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                            ResponseCodes.FINAL_ACK_YES);
                    Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(),
                            ResponseCodes.RESPONSE_FAILED);
                }
            }
        }
    }

    @Test
    public void createServiceHandlerRxDirectionPortIsNull()
            throws ExecutionException, InterruptedException {
        ServiceCreateOutput result = getRxDirectionPortServiceCreateOutput(null,
            ServiceDataUtils.buildServiceCreateInput().getServiceAEnd().getRxDirection().getLgx());
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void createServiceHandlerNotValidRxDirectionLgx()
            throws InvocationTargetException, IllegalAccessException, ExecutionException, InterruptedException {
        List<String> invalidData = Arrays.asList(null, "");
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        for (Method method : LgxBuilder.class.getMethods()) {
            if (method.getName().startsWith("set")) {
                for (Object data : invalidData) {
                    LgxBuilder lgxBuilder = new LgxBuilder(
                            serviceCreateInput.getServiceAEnd().getRxDirection().getLgx());
                    method.invoke(lgxBuilder, data);
                    ServiceCreateOutput result = getRxDirectionPortServiceCreateOutput(
                            serviceCreateInput.getServiceAEnd().getRxDirection().getPort(), lgxBuilder.build());
                    Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                            ResponseCodes.FINAL_ACK_YES);
                    Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(),
                            ResponseCodes.RESPONSE_FAILED);
                }
            }
        }
    }

    @Test
    public void createServiceHandlerRxDirectionLgxIsNull() throws ExecutionException, InterruptedException {
        ServiceCreateOutput result = getRxDirectionPortServiceCreateOutput(
            ServiceDataUtils.buildServiceCreateInput().getServiceAEnd().getRxDirection().getPort(), null);
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void createServiceHandlerResponseCodesNotPassed() throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();

        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_FAILED).setResponseMessage("failed").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
        verify(this.pceServiceWrapperMock).performPCE(serviceCreateInput, true);
    }

    @Test
    public void createServiceHandlerOperationResultNotPassed() throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class)))
                .thenReturn(OperationResult.failed(
                        "Failed to create service " + serviceCreateInput.getServiceName() + " to Service List"));
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
        verify(this.serviceDataStoreOperationsMock).createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class));
    }

    @Test
    public void createServiceHandlerOperationServicePathSaveResultNotPassed()
            throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class)))
                .thenReturn(OperationResult.failed("Failed to create servicePath " + serviceCreateInput.getServiceName()
                        + " to ServicePath List"));
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
        verify(this.serviceDataStoreOperationsMock).createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class));
    }

    @Test
    public void createServiceHandlerModifyServicePassed() throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("successful").build();

        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon2).build());
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(serviceCreateInput.getServiceName(),
                State.InService, State.InService)).thenReturn(OperationResult.ok("successful"));
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_OK);
        verify(this.serviceDataStoreOperationsMock).modifyService(serviceCreateInput.getServiceName(), State.InService,
                State.InService);
    }

    @Test
    public void createServiceHandlerModifyServiceNotPassed() throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("successful").build();

        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon2).build());
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(serviceCreateInput.getServiceName(),
                State.InService, State.InService)).thenReturn(OperationResult.failed("failure"));
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_OK);
        verify(this.serviceDataStoreOperationsMock).modifyService(serviceCreateInput.getServiceName(), State.InService,
                State.InService);
    }

    @Test
    public void createServiceHandlerServiceImplementationNotPassed() throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_FAILED).setResponseMessage("failure").build();

        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon2).build());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(serviceCreateInput.getServiceName()))
                .thenReturn(OperationResult.ok("successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(serviceCreateInput.getServiceName()))
                .thenReturn(OperationResult.ok("successful"));
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
        verify(this.serviceDataStoreOperationsMock).deleteService(serviceCreateInput.getServiceName());
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(serviceCreateInput.getServiceName());
    }

    @Test
    public void createServiceHandlerServiceImplementationNotPassed2() throws ExecutionException, InterruptedException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.pceServiceWrapperMock.performPCE(serviceCreateInput, true))
                .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_FAILED).setResponseMessage("failure").build();

        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon2).build());
        Mockito.when(this.serviceDataStoreOperationsMock.deleteService(serviceCreateInput.getServiceName()))
                .thenReturn(OperationResult.failed("successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.deleteServicePath(serviceCreateInput.getServiceName()))
                .thenReturn(OperationResult.failed("successful"));
        ServiceCreateOutput result = this.serviceHandlerImplMock.serviceCreate(serviceCreateInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
        verify(this.serviceDataStoreOperationsMock).deleteService(serviceCreateInput.getServiceName());
        verify(this.serviceDataStoreOperationsMock).deleteServicePath(serviceCreateInput.getServiceName());
    }

    private ServiceCreateOutput getRxDirectionPortServiceCreateOutput(Port port, Lgx lgx)
            throws InterruptedException, ExecutionException {
        ServiceCreateInput serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceAEndBuilder serviceAEndBuilder = ServiceDataUtils.getServiceAEndBuild();
        RxDirectionBuilder rxDirectionBuilder = new RxDirectionBuilder(
                serviceCreateInput.getServiceAEnd().getRxDirection());
        rxDirectionBuilder.setPort(port);
        rxDirectionBuilder.setLgx(lgx);
        serviceAEndBuilder.setRxDirection(rxDirectionBuilder.build());
        ServiceCreateInputBuilder buildInput = new ServiceCreateInputBuilder(serviceCreateInput);
        this.serviceHandler = new ServicehandlerImpl(getDataBroker(), this.pathComputationService, null, null);
        return serviceHandler.serviceCreate(buildInput.setServiceAEnd(serviceAEndBuilder.build()).build()).get()
                .getResult();
    }

    @Test
    public void deleteServiceInvalidIfServiceNameIsEmpty() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder
                .setServiceDeleteReqInfo(
                        new ServiceDeleteReqInfoBuilder(builder.getServiceDeleteReqInfo()).setServiceName("").build())
                .build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceInvalidIfServiceNameIsNull() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder
                .setServiceDeleteReqInfo(
                        new ServiceDeleteReqInfoBuilder(builder.getServiceDeleteReqInfo()).setServiceName(null).build())
                .build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteTempServiceInvalidIfCommonIdIsEmpty() throws ExecutionException, InterruptedException {
        TempServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildTempServiceDeleteInput();
        TempServiceDeleteInputBuilder builder = new TempServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder.setCommonId("").build();
        TempServiceDeleteOutput result = this.serviceHandler.tempServiceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteTempServiceInvalidIfCommonIdIsNull() throws ExecutionException, InterruptedException {
        TempServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildTempServiceDeleteInput();
        TempServiceDeleteInputBuilder builder = new TempServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder.setCommonId(null).build();
        TempServiceDeleteOutput result = this.serviceHandler.tempServiceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceInvalidIfSdncRequestHeaderIsNull() throws ExecutionException, InterruptedException {
        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder.setSdncRequestHeader(null).build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceInvalidIfSdncRequestHeaderRequestIdIsNull()
            throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder
                .setSdncRequestHeader(
                        new SdncRequestHeaderBuilder(builder.getSdncRequestHeader()).setRequestId(null).build())
                .build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceInvalidIfSdncRequestHeaderRequestIdIsEmpty()
            throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder.setSdncRequestHeader(
                new SdncRequestHeaderBuilder(builder.getSdncRequestHeader()).setRequestId("").build()).build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceInvalidIfSdncRequestHeaderServiceActionIsNull()
            throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder
                .setSdncRequestHeader(
                        new SdncRequestHeaderBuilder(builder.getSdncRequestHeader()).setRpcAction(null).build())
                .build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceInvalidIfSdncRequestHeaderServiceActionIsNotDelete()
            throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder(serviceDeleteInput);
        serviceDeleteInput = builder.setSdncRequestHeader(new SdncRequestHeaderBuilder(builder.getSdncRequestHeader())
                .setRpcAction(RpcActions.ServiceCreate).build()).build();
        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceIfServiceHandlerCompliancyCheckNotPassed()
            throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();

        Mockito.when(this.complianceCheckResultMock.hasPassed()).thenReturn(false);

        ServiceDeleteOutput result = this.serviceHandlerImplMock.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceNotPresent() throws ExecutionException, InterruptedException {

        ServiceDeleteOutput result = this.serviceHandler.serviceDelete(ServiceDataUtils.buildServiceDeleteInput()).get()
                .getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceIfServiceNotPresent() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        Mockito.when(this.servicesOptionalMock.isPresent()).thenReturn(false);
        ServiceDeleteOutput result = this.serviceHandlerImplMock.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteTempServiceIfTempServiceNotPresent() throws ExecutionException, InterruptedException {
        TempServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildTempServiceDeleteInput();
        Mockito.when(this.servicesOptionalMock.isPresent()).thenReturn(false);
        TempServiceDeleteOutput result =
                this.serviceHandlerImplMock.tempServiceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServiceNotPassed() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        Optional<Services> service = Optional.of(new ServicesBuilder().setServiceName("service 1").build());
        Mockito.when(this.serviceDataStoreOperationsMock.getService("service 1")).thenReturn(service);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput input = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(serviceDeleteInput));
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_FAILED).setResponseMessage("success").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.rendererServiceOperationsMock.serviceDelete(input)).thenReturn(output);
        ServiceDeleteOutput result = this.serviceHandlerImplMock.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_FAILED);
    }

    @Test
    public void deleteServicePathNotPassed() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        Optional<Services> service = Optional.of(new ServicesBuilder().setServiceName("service 1").build());
        Mockito.when(this.serviceDataStoreOperationsMock.getService("service 1")).thenReturn(service);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput input = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(serviceDeleteInput));
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.rendererServiceOperationsMock.serviceDelete(input)).thenReturn(output);
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteServicePath(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.failed("failed"));
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteService(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.failed("failed"));
        ServiceDeleteOutput result = this.serviceHandlerImplMock.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_OK);
    }

    @Test
    public void deleteServiceOperationNotPassed() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        Optional<Services> service = Optional.of(new ServicesBuilder().setServiceName("service 1").build());
        Mockito.when(this.serviceDataStoreOperationsMock.getService("service 1")).thenReturn(service);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput input = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(serviceDeleteInput));
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.rendererServiceOperationsMock.serviceDelete(input)).thenReturn(output);
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteServicePath(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.ok("success"));
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteService(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.ok("success"));
        ServiceDeleteOutput result = this.serviceHandlerImplMock.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_OK);
    }

    @Test
    public void deleteServiceIfServicePresentAndValid() throws ExecutionException, InterruptedException {

        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        Optional<Services> service = Optional.of(new ServicesBuilder().setServiceName("service 1").build());
        Mockito.when(this.serviceDataStoreOperationsMock.getService("service 1")).thenReturn(service);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput input = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(serviceDeleteInput));
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteOutput output = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer
                .rev171017.ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.rendererServiceOperationsMock.serviceDelete(input)).thenReturn(output);
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteServicePath(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.ok("success"));
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteService(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.ok("success"));
        ServiceDeleteOutput result = this.serviceHandlerImplMock.serviceDelete(serviceDeleteInput).get().getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_OK);
    }

    @Test
    public void deleteTempServiceIfServicePresentAndValid() throws ExecutionException, InterruptedException {

        TempServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildTempServiceDeleteInput();
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
            .Services> service = Optional.of(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014
                    .temp.service.list.ServicesBuilder().setCommonId("service 1").build());
        Mockito.when(this.serviceDataStoreOperationsMock.getTempService("service 1")).thenReturn(service);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput input = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(serviceDeleteInput));
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteOutput output = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer
                .rev171017.ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.rendererServiceOperationsMock.serviceDelete(input)).thenReturn(output);
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteServicePath(serviceDeleteInput.getCommonId()))
                .thenReturn(OperationResult.ok("success"));
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteTempService(serviceDeleteInput.getCommonId()))
                .thenReturn(OperationResult.ok("success"));
        TempServiceDeleteOutput result = this.serviceHandlerImplMock.tempServiceDelete(serviceDeleteInput).get()
                .getResult();
        Assert.assertEquals(result.getConfigurationResponseCommon().getAckFinalIndicator(),
                ResponseCodes.FINAL_ACK_YES);
        Assert.assertEquals(result.getConfigurationResponseCommon().getResponseCode(), ResponseCodes.RESPONSE_OK);
    }

    @Test
    public void rerouteServiceIsNotPresent() throws ExecutionException, InterruptedException {

        ServiceRerouteInput input = ServiceDataUtils.buildServiceRerouteInput();
        ServiceRerouteOutput result = this.serviceHandler.serviceReroute(input).get().getResult();
        Assert.assertEquals(result.getStatus(), RpcStatus.Failed);
        Assert.assertEquals(result.getStatusMessage(), "Service 'service 1' is not present");

    }

    @Test
    public void rerouteServiceIfserviceIsPresent() throws ExecutionException, InterruptedException {

        ServiceRerouteInput serviceRerouteinput = ServiceDataUtils.buildServiceRerouteInput();
        ServiceDeleteInput serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        ServiceCreateInput serviceInput = ServiceDataUtils.buildServiceCreateInput();

        /** Mock RPC service-delete. */
        Services serviceMock = ModelMappingUtils.mappingServices(serviceInput, null);
        Optional<Services> service = Optional.of(serviceMock);
        Mockito.when(this.serviceDataStoreOperationsMock.getService(any(String.class))).thenReturn(service);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput input = ModelMappingUtils.createServiceDeleteInput(serviceRerouteinput, service.get());
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteOutput output = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer
                .rev171017.ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon).build();
        Mockito.when(this.rendererServiceOperationsMock.serviceDelete(input)).thenReturn(output);
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteServicePath(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.ok("success"));
        Mockito.when(this.serviceDataStoreOperationsMock
                .deleteService(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName()))
                .thenReturn(OperationResult.ok("success"));

        ConfigurationResponseCommon configurationResponseCommon2 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("success").build();
        PathComputationRequestOutput pathComputationRequestOutput = new PathComputationRequestOutputBuilder(
                PceTestData.getPCE_simpletopology_test1_result((long) 5))
                        .setConfigurationResponseCommon(configurationResponseCommon2).build();

        /** Mock RPC service-create. */
        Mockito.when(this.pceServiceWrapperMock.performPCE(any(ServiceCreateInput.class), any(Boolean.class)))
            .thenReturn(pathComputationRequestOutput);
        Mockito.when(this.serviceDataStoreOperationsMock.createService(any(ServiceCreateInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        Mockito.when(this.serviceDataStoreOperationsMock.createServicePath(any(ServiceInput.class),
                any(PathComputationRequestOutput.class))).thenReturn(OperationResult.ok("Successful"));
        ConfigurationResponseCommon configurationResponseCommon3 = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setRequestId("1")
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("successful").build();
        Mockito.when(
                this.rendererServiceOperationsMock.serviceImplementation(any(ServiceImplementationRequestInput.class)))
                .thenReturn(new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon3).build());
        Mockito.when(this.serviceDataStoreOperationsMock.modifyService(any(String.class), any(State.class),
                any(State.class))).thenReturn(OperationResult.ok("successful"));

        ServiceRerouteOutput result = this.serviceHandlerImplMock.serviceReroute(serviceRerouteinput).get().getResult();
        Assert.assertEquals(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RpcStatus.Successful,
                result.getStatus());
        Assert.assertEquals("Service reroute successfully !", result.getStatusMessage());
    }
}
