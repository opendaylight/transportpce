/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co.routing.ServiceIdentifierListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.response.parameters.PathDescriptionBuilder;
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

    @BeforeEach
    void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    private ServiceReconfigureInput buildServiceConfigurationInput() {
        return new ServiceReconfigureInputBuilder()
            .setNewServiceName("service 1").setServiceName("service 1").setCommonId("common id")
            .setConnectionType(ConnectionType.Service).setCustomer("customer").setCustomerContact("customer contact")
            .setDueDate(new DateAndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                .format(OffsetDateTime.now(ZoneOffset.UTC))))
            .setEndDate(new DateAndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                .format(OffsetDateTime.now(ZoneOffset.UTC).plusDays(10))))
            .setNcCode("nc node").setNciCode("nci node").setSecondaryNciCode("secondry").setOperatorContact("operator")
            .setServiceAEnd(ServiceDataUtils.getServiceAEndBuildReconfigure().build())
            .setServiceZEnd(ServiceDataUtils.getServiceZEndBuildReconfigure().build())
            .setHardConstraints(new HardConstraintsBuilder()
                .setCustomerCode(Set.of("Some customer-code"))
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209
                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service")
                            .build()))
                    .build())
                .build())
            .setSoftConstraints(new SoftConstraintsBuilder()
                .setCustomerCode(Set.of("Some customer-code"))
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209
                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service")
                            .build()))
                    .build())
                .build())
            .build();
    }

    private PathComputationRequestOutput buildPathComputationOutput() {
        return new PathComputationRequestOutputBuilder()
            .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1")
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setResponseCode(ResponseCodes.RESPONSE_OK)
                .setResponseMessage("PCE calculation in progress")
                .build())
            .setResponseParameters(new ResponseParametersBuilder()
                .setPathDescription(new PathDescriptionBuilder()
                    .setAToZDirection(new AToZDirectionBuilder()
                        .setAToZWavelengthNumber(Uint32.valueOf(1))
                        .setRate(Uint32.valueOf(1))
                        .build())
                    .setZToADirection(new ZToADirectionBuilder()
                        .setZToAWavelengthNumber(Uint32.valueOf(1))
                        .setRate(Uint32.valueOf(1))
                        .build())
                    .build())
                .build())
            .build();
    }

    @Test
    void mappingServiceNotNullServiceReconfigureInput() {
        Services services = ModelMappingUtils.mappingServices(null, buildServiceConfigurationInput());
        assertEquals("service 1", services.getServiceName());
    }

    @Test
    void mappingServiceValid() {
        Services services = ModelMappingUtils.mappingServices(ServiceDataUtils.buildServiceCreateInput(), null);
        assertEquals("service 1", services.getServiceName());
    }

    @Test
    //TODO : is this unit test relevant ?
    void mappingServicesPathNullServiceCreateInput() {
        ServicePaths services = ModelMappingUtils.mappingServicePaths(null, buildPathComputationOutput());
        assertEquals(new ServicePathsBuilder().withKey(new ServicePathsKey("unknown")).build(), services);
    }

    @Test
    void mappingServicePathWithServiceInputWithHardConstraints() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInputWithHardConstraints();
        ServiceInput serviceInput = new ServiceInput(createInput);
        ServicePaths services = ModelMappingUtils.mappingServicePaths(serviceInput, buildPathComputationOutput());
        assertEquals(serviceInput.getServiceName(), services.getServicePathName());
        assertNotNull(services.getHardConstraints());
    }

    @Test
    void mappingServicePathWithServiceInputWithSoftConstraints() {
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInputWithSoftConstraints();
        ServiceInput serviceInput = new ServiceInput(createInput);
        ServicePaths services = ModelMappingUtils.mappingServicePaths(serviceInput, buildPathComputationOutput());
        assertEquals(serviceInput.getServiceName(), services.getServicePathName());
        assertNotNull(services.getSoftConstraints());
    }

    @Test
    void createServiceDeleteInputWithServiceRerouteInput() {
        ServiceRerouteInput serviceRerouteinput = new ServiceRerouteInputBuilder().setServiceName("reroute").build();
        Services services = new ServicesBuilder()
                    .withKey(new ServicesKey("reroute"))
                    .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("123").build())
                    .build();
        var serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(serviceRerouteinput, services);
        assertEquals("reroute", serviceDeleteInput.getServiceName());
        assertEquals("123", serviceDeleteInput.getServiceHandlerHeader().getRequestId());
    }

    @Test
    void  createServiceDeleteInputWithServiceReconfigureInput() {
        ServiceReconfigureInput serviceReconfigureInput = new ServiceReconfigureInputBuilder()
                .setServiceName("reconf")
                .build();
        var serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(serviceReconfigureInput);
        assertEquals("reconf", serviceDeleteInput.getServiceName());
        assertEquals("reconf-reconfigure", serviceDeleteInput.getServiceHandlerHeader().getRequestId());
    }

    @Test
    void createServiceDeleteInputWithServiceRestorationInput() {
        Services services = new ServicesBuilder()
                .withKey(new ServicesKey("rest"))
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("123").build())
                .build();
        ServiceRestorationInput serviceRestorationInput = new ServiceRestorationInputBuilder()
                .setServiceName("rest")
                .build();
        var serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(serviceRestorationInput, services);
        assertEquals("rest", serviceDeleteInput.getServiceName());
        assertEquals("123", serviceDeleteInput.getServiceHandlerHeader().getRequestId());
    }

    @Test
    void createDeleteServiceReplyWithServiceDeleteInputWithSdncHeader()
            throws ExecutionException, InterruptedException  {
        ServiceDeleteInput input = new ServiceDeleteInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build())
                .build();
        ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDeleteOutputF =
            ModelMappingUtils.createDeleteServiceReply(input, "ack", "message", "200");
        serviceDeleteOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceDeleteOutput> serviceDeleteOutput = serviceDeleteOutputF.get();
        assertEquals("200", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertEquals("12", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createDeleteServiceReplyWithServiceDeleteInputWithoutSdncHeader()
            throws ExecutionException, InterruptedException  {
        ServiceDeleteInput input = new ServiceDeleteInputBuilder().build();
        ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDeleteOutputF =
            ModelMappingUtils.createDeleteServiceReply(input, "ack", "message", "200");
        serviceDeleteOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceDeleteOutput> serviceDeleteOutput = serviceDeleteOutputF.get();
        assertEquals("200", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceDeleteOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertNull(serviceDeleteOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createCreateServiceReplyWithServiceCreatInputWithSdncRequestHeader()
            throws ExecutionException, InterruptedException {
        ServiceCreateInput input = new ServiceCreateInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build())
                .build();
        ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertEquals("12", serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createCreateServiceReplyWithServiceCreatInputWithoutSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        ServiceCreateInput input = new ServiceCreateInputBuilder().build();
        ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertNull(serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createCreateServiceReplyWithTempServiceCreatInputWithSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        TempServiceCreateInput input = new TempServiceCreateInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build())
                .build();
        ListenableFuture<RpcResult<TempServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<TempServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertEquals("12", serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createCreateServiceReplyWithTempServiceCreatInputWithoutSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        TempServiceCreateInput input = new TempServiceCreateInputBuilder().build();
        ListenableFuture<RpcResult<TempServiceCreateOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<TempServiceCreateOutput> serviceCreatOutput = serviceCreatOutputF.get();
        assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertNull(serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createCreateServiceReplyWithServiceFeasibilityCheckInputWithSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        ServiceFeasibilityCheckInput input = new ServiceFeasibilityCheckInputBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("12").build())
                .build();
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceFeasibilityCheckOutput> serviceCreatOutput = serviceCreatOutputF.get();
        assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertEquals("12", serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void createCreateServiceReplyWithServiceFeasibilityCheckInputWithoutSdncRequestHeader()
        throws ExecutionException, InterruptedException {
        ServiceFeasibilityCheckInput input = new ServiceFeasibilityCheckInputBuilder().build();
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceCreatOutputF =
            ModelMappingUtils.createCreateServiceReply(input, "ack", "message", "200");
        serviceCreatOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
        RpcResult<ServiceFeasibilityCheckOutput> serviceCreatOutput = serviceCreatOutputF.get();
        assertEquals("200", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals("ack", serviceCreatOutput.getResult().getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals("message", serviceCreatOutput.getResult().getConfigurationResponseCommon().getResponseMessage());
        assertNull(serviceCreatOutput.getResult().getConfigurationResponseCommon().getRequestId());
    }

    @Test
    void testCreateRestoreServiceReply() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceRestorationOutput>> serviceRestorationOutputF =
            ModelMappingUtils.createRestoreServiceReply("message");
        serviceRestorationOutputF.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();
    }
}
