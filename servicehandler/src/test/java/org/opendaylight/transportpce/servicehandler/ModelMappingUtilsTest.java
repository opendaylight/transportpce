/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ModelMappingUtilsTest extends AbstractTest {

    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
    }

    @After
    public void tearDown() {
        executorService.shutdownNow();
    }

    private ServiceReconfigureInput buildServiceConfigurationInput() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime offsetDateTime2 = offsetDateTime.plusDays(10);
        return new ServiceReconfigureInputBuilder().setNewServiceName("service 1")
            .setServiceName("service 1").setCommonId("common id").setConnectionType(ConnectionType.Service)
            .setCustomer("customer").setCustomerContact("customer contact").setDueDate(new DateAndTime(
                    dtf.format(offsetDateTime)))
            .setEndDate(new DateAndTime(dtf.format(offsetDateTime2)))
            .setNcCode("nc node").setNciCode("nci node").setSecondaryNciCode("secondry").setOperatorContact("operator")
            .setServiceAEnd(ServiceDataUtils.getServiceAEndBuildReconfigure().build())
            .setServiceZEnd(ServiceDataUtils.getServiceZEndBuildReconfigure().build())
            .setHardConstraints(new HardConstraintsBuilder()
                        .setCoRoutingOrGeneral(new CoRoutingBuilder()
                                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                                        .constrains.rev190329.constraints.co.routing.or.general.co.routing
                                        .CoRoutingBuilder().setExistingService(
                                        Arrays.asList("Some existing-service")).build())
                                .build())
                        .setCustomerCode(Arrays.asList("Some customer-code"))
                        .build())
                .setSoftConstraints(new SoftConstraintsBuilder()
                        .setCoRoutingOrGeneral(new CoRoutingBuilder()
                                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                                        .constrains.rev190329.constraints.co.routing.or.general.co.routing
                                        .CoRoutingBuilder().setExistingService(
                                        Arrays.asList("Some existing-service")).build())
                                .build())
                        .setCustomerCode(Arrays.asList("Some customer-code"))
                        .build())
                .build();
    }

    private PathComputationRequestOutput buildPathComputationOutput() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1").setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress").build();
        ResponseParameters responseParameters = new ResponseParametersBuilder()
            .setPathDescription(new PathDescriptionBuilder()
                .setAToZDirection(new AToZDirectionBuilder().setAToZWavelengthNumber(Uint32.valueOf(1))
                    .setRate(Uint32.valueOf(1)).build())
                .setZToADirection(new ZToADirectionBuilder().setZToAWavelengthNumber(Uint32.valueOf(1))
                    .setRate(Uint32.valueOf(1)).build()).build())
            .build();
        return new PathComputationRequestOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon)
            .setResponseParameters(responseParameters).build();
    }

    @Test
    //TODO : is this unit test relevant ?
    public void mappingServicesNullServiceCreateInput() {
        Services services = ModelMappingUtils.mappingServices(null, null);
        Assert.assertEquals(new ServicesBuilder().withKey(new ServicesKey("unknown")).build(), services);
    }

    @Test
    public void mappingServiceNotNullServiceReconfigureInput() {
        Services services = ModelMappingUtils.mappingServices(null, buildServiceConfigurationInput());
        Assert.assertEquals("service 1", services.getServiceName());
    }

    @Test
    public void mappingServiceValid() {
        Services services = ModelMappingUtils.mappingServices(ServiceDataUtils.buildServiceCreateInput(),
                null);
        Assert.assertEquals("service 1", services.getServiceName());
    }

    @Test
    //TODO : is this unit test relevant ?
    public void mappingServicesPathNullServiceCreateInput() {
        ServicePaths services = ModelMappingUtils.mappingServicePaths(null, buildPathComputationOutput());
        Assert.assertEquals(new ServicePathsBuilder().withKey(new ServicePathsKey("unknown")).build(), services);
    }

    @Test
    public void mappingServicePathWithServiceInputWithHardConstraints() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInputWithHardConstraints();
        ServiceInput serviceInput = new ServiceInput(createInput);
        ServicePaths services = ModelMappingUtils.mappingServicePaths(serviceInput, buildPathComputationOutput());
        Assert.assertEquals(serviceInput.getServiceName(), services.getServicePathName());
        Assert.assertNotNull(services.getHardConstraints());
    }

    @Test
    public void mappingServicePathWithServiceInputWithSoftConstraints() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInputWithSoftConstraints();
        ServiceInput serviceInput = new ServiceInput(createInput);
        ServicePaths services = ModelMappingUtils.mappingServicePaths(serviceInput, buildPathComputationOutput());
        Assert.assertEquals(serviceInput.getServiceName(), services.getServicePathName());
        Assert.assertNotNull(services.getSoftConstraints());
    }

    @Test
    public void createServiceDeleteInputWithServiceRerouteInput() {
        ServiceRerouteInput serviceRerouteinput = new ServiceRerouteInputBuilder().setServiceName("reroute").build();
        Services services = new ServicesBuilder()
                .withKey(new ServicesKey("reroute"))
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("123").build()).build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteInput
            serviceDeleteInput =
                    ModelMappingUtils.createServiceDeleteInput(serviceRerouteinput, services);
        Assert.assertEquals("reroute", serviceDeleteInput.getServiceName());
        Assert.assertEquals("123", serviceDeleteInput.getServiceHandlerHeader().getRequestId());
    }

    @Test
    public void  createServiceDeleteInputWithServiceReconfigureInput() {
        ServiceReconfigureInput serviceReconfigureInput = new  ServiceReconfigureInputBuilder()
                .setServiceName("reconf").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteInput
            serviceDeleteInput =
                    ModelMappingUtils.createServiceDeleteInput(serviceReconfigureInput);
        Assert.assertEquals("reconf", serviceDeleteInput.getServiceName());
        Assert.assertEquals("reconf-reconfigure", serviceDeleteInput.getServiceHandlerHeader().getRequestId());
    }

    @Test
    public void createServiceDeleteInputWithServiceRestorationInput() {
        Services services = new ServicesBuilder()
                .withKey(new ServicesKey("rest"))
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("123").build()).build();
        ServiceRestorationInput serviceRestorationInput =
            new ServiceRestorationInputBuilder().setServiceName("rest").build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteInput
            serviceDeleteInput =
                    ModelMappingUtils.createServiceDeleteInput(serviceRestorationInput, services);
        Assert.assertEquals("rest", serviceDeleteInput.getServiceName());
        Assert.assertEquals("123", serviceDeleteInput.getServiceHandlerHeader().getRequestId());
    }

    @Test
    public void createDeleteServiceReplyWithServiceDeleteInputWithSdncHeader()
        throws ExecutionException, InterruptedException  {
        ServiceDeleteInput input = new ServiceDeleteInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build()).build();
        ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDeleteOutputF =
            ModelMappingUtils.createDeleteServiceReply(input, "ack", "message", "200");
        serviceDeleteOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceDeleteOutput> serviceDeleteOutput = serviceDeleteOutputF.get();
        Assert.assertEquals("200", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertEquals("12", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createDeleteServiceReplyWithServiceDeleteInputWithoutSdncHeader()
            throws ExecutionException, InterruptedException  {
        ServiceDeleteInput input = new ServiceDeleteInputBuilder().build();
        ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDeleteOutputF =
            ModelMappingUtils.createDeleteServiceReply(input, "ack", "message", "200");
        serviceDeleteOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceDeleteOutput> serviceDeleteOutput = serviceDeleteOutputF.get();
        Assert.assertEquals("200", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertNull(serviceDeleteOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createCreateServiceReplyWithServiceCreatInputWithSdncRequestHeader()
            throws ExecutionException, InterruptedException {
        ServiceCreateInput input =
            new ServiceCreateInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build()).build();
        ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        Assert.assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertEquals("12", serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createCreateServiceReplyWithServiceCreatInputWithoutSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        ServiceCreateInput input = new ServiceCreateInputBuilder().build();
        ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        Assert.assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertNull(serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createCreateServiceReplyWithTempServiceCreatInputWithSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        TempServiceCreateInput input =
            new TempServiceCreateInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build()).build();
        ListenableFuture<RpcResult<TempServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<TempServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        Assert.assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertEquals("12", serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createCreateServiceReplyWithTempServiceCreatInputWithoutSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        TempServiceCreateInput input = new TempServiceCreateInputBuilder().build();
        ListenableFuture<RpcResult<TempServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<TempServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        Assert.assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertNull(serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createCreateServiceReplyWithServiceFeasibilityCheckInputWithSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        ServiceFeasibilityCheckInput input =
            new ServiceFeasibilityCheckInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build()).build();
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceFeasibilityCheckOutput> serviceCreatOutput = serviceCreatOutputF.get();
        Assert.assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertEquals("12", serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void createCreateServiceReplyWithServiceFeasibilityCheckInputWithoutSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        ServiceFeasibilityCheckInput input = new ServiceFeasibilityCheckInputBuilder().build();
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceFeasibilityCheckOutput> serviceCreatOutput = serviceCreatOutputF.get();
        Assert.assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            "ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
            "message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        Assert.assertNull(serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    public void testCreateRestoreServiceReply() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceRestorationOutput>> serviceRestorationOutputF =
            ModelMappingUtils.createRestoreServiceReply("message", RpcStatus.Failed);
        serviceRestorationOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceRestorationOutput> serviceRestorationOutput = serviceRestorationOutputF.get();
        Assert.assertEquals("message", serviceRestorationOutput.getResult().getStatusMessage());
    }
}
