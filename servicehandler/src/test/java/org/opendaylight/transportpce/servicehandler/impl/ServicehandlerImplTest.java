/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRerouteRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ServicehandlerImplTest extends AbstractTest {

    @Mock
    private PathComputationService pathComputationService;

    @Mock
    private RendererServiceOperations rendererServiceOperations;

    @Mock
    private NotificationPublishService notificationPublishService;

    @Mock
    private PceListenerImpl pceListenerImpl;

    @Mock
    private RendererListenerImpl rendererListenerImpl;

    @Mock
    private NetworkModelListenerImpl networkModelListenerImpl;

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private ServiceCreateInput serviceCreateInput;
    private ServiceDeleteInput serviceDeleteInput;
    private ServiceReconfigureInput serviceReconfigureInput;
    private ServiceRestorationInput serviceRestorationInput;
    private ServiceRerouteInput serviceRerouteInput;
    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        MockitoAnnotations.openMocks(this);
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getNewDataBroker());
        serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        serviceReconfigureInput = ServiceDataUtils.buildServiceReconfigureInput();
        serviceRestorationInput = ServiceDataUtils.buildServiceRestorationInput();
        serviceRerouteInput = ServiceDataUtils.buildServiceRerouteInput();
    }

    @Test
    public void createServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =
                servicehandlerImpl.serviceCreate(new ServiceCreateInputBuilder().build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void createServiceShouldBeFailedWithServiceAlreadyExist() throws ExecutionException,
            InterruptedException {
        final ServiceDataStoreOperations serviceDSOperations = mock(ServiceDataStoreOperations.class);
        Mockito.when(serviceDSOperations.getService(serviceCreateInput.getServiceName()))
                .thenReturn(Optional.of(
                        new ServicesBuilder()
                                .setServiceName(serviceCreateInput.getServiceName())
                                .build()));
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDSOperations);
        ListenableFuture<RpcResult<ServiceCreateOutput>> result = servicehandlerImpl.serviceCreate(serviceCreateInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void createServiceShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceCreateOutput>> result = servicehandlerImpl.serviceCreate(serviceCreateInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result = servicehandlerImpl.serviceDelete(
                new ServiceDeleteInputBuilder()
                        .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                                .setServiceName("")
                                .build())
                        .build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result = servicehandlerImpl.serviceDelete(serviceDeleteInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        serviceDataStoreOperations.createService(serviceCreateInput);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result = servicehandlerImpl.serviceDelete(serviceDeleteInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }


    @Test
    public void serviceFeasibilityCheckShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> result =
                servicehandlerImpl.serviceFeasibilityCheck(new ServiceFeasibilityCheckInputBuilder().build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceFeasibilityCheckShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> result =
                servicehandlerImpl.serviceFeasibilityCheck(ServiceDataUtils.buildServiceFeasibilityCheckInput());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceReconfigureShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result =
                servicehandlerImpl.serviceReconfigure(new ServiceReconfigureInputBuilder().setServiceName("").build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
    }


    @Test
    public void serviceReconfigureShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        //action -> service reconfigure
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result = servicehandlerImpl.serviceReconfigure(
                serviceReconfigureInput);

        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
    }

    @Test
    public void serviceReconfigureShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        // serviceReconfigure is calling service delete method in renderer
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to reconfigure
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        serviceDataStoreOperations.createService(serviceCreateInput);

        //service reconfigure test action
        //ServiceReconfigureInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result = servicehandlerImpl.serviceReconfigure(
                serviceReconfigureInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
    }

    @Test
    public void serviceReRestorationShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result =
                servicehandlerImpl.serviceRestoration(new ServiceRestorationInputBuilder().setServiceName("").build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
    }


    @Test
    public void serviceRestorationShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        //action -> service restore
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result = servicehandlerImpl.serviceRestoration(
                serviceRestorationInput);

        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
    }

    @Test
    public void serviceRestorationShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        // serviceRestoration is calling service delete method in renderer
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to restore
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        serviceDataStoreOperations.createService(serviceCreateInput);

        //service Restoration test action
        //ServiceRestorationInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result = servicehandlerImpl.serviceRestoration(
                serviceRestorationInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
    }

    @Test
    public void serviceRerouteShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result =
                servicehandlerImpl.serviceReroute(new ServiceRerouteInputBuilder().setServiceName("").build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();

        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceRerouteShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        //action -> service reconfigure
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result = servicehandlerImpl.serviceReroute(
                serviceRerouteInput);

        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();

        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceRerouteShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        Mockito.when(pathComputationService.pathComputationRerouteRequest(any()))
                .thenReturn(Futures.immediateFuture(new PathComputationRerouteRequestOutputBuilder()
                        .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                                .setResponseCode(ResponseCodes.RESPONSE_OK)
                                .build())
                        .build()));
        Map<AToZKey, AToZ> atoz = Map.of(
                new AToZKey("0"),
                new AToZBuilder()
                        .setId("0")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdC")
                                        .setTpId("TpIdC1")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build(),
                new AToZKey("1"),
                new AToZBuilder()
                        .setId("1")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdD")
                                        .setTpId("TpIdCD")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build(), new AToZKey("2"),
                new AToZBuilder()
                        .setId("2")
                        .setResource(new ResourceBuilder()
                                .setResource(new TerminationPointBuilder()
                                        .setTpNodeId("tpNodeIdA")
                                        .setTpId("TpIdA1")
                                        .build())
                                .setState(State.InService)
                                .build())
                        .build()

        );
        serviceDataStoreOperations.createServicePath(new ServiceInput(serviceCreateInput),
                new PathComputationRequestOutputBuilder()
                        .setResponseParameters(new ResponseParametersBuilder()
                                .setPathDescription(new PathDescriptionBuilder()
                                        .setAToZDirection(new AToZDirectionBuilder()
                                                .setAToZ(atoz)
                                                .setRate(Uint32.valueOf(1))
                                                .build())
                                        .setZToADirection(new ZToADirectionBuilder()
                                                .setRate(Uint32.valueOf(1))
                                                .build())
                                        .build())
                                .build())
                        .build());

        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        serviceDataStoreOperations.createService(serviceCreateInput);
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result = servicehandlerImpl.serviceReroute(
                serviceRerouteInput);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();

        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void tempServiceDeleteShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result =
                servicehandlerImpl.tempServiceDelete(new TempServiceDeleteInputBuilder().setCommonId("").build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();

        RpcResult<TempServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_FAILED,
                rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
                LogMessages.SERVICE_NON_COMPLIANT,
                rpcResult.getResult().getConfigurationResponseCommon().getResponseMessage());
    }

    @Test
    public void tempServiceDeleteShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result = servicehandlerImpl.tempServiceDelete(
                ServiceDataUtils.buildTempServiceDeleteInput());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void tempServiceDeleteShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));

        //create temp service to delete in the temp delete action
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        serviceDataStoreOperations.createTempService(createInput);

        TempServiceDeleteInput input = ServiceDataUtils.buildTempServiceDeleteInput(createInput.getCommonId());
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result = servicehandlerImpl.tempServiceDelete(input);
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void tempServiceCreateShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);
        ListenableFuture<RpcResult<TempServiceCreateOutput>> result =
                servicehandlerImpl.tempServiceCreate(new TempServiceCreateInputBuilder().build());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }


    @Test
    public void tempServiceCreateShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));

        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, serviceDataStoreOperations);

        ListenableFuture<RpcResult<TempServiceCreateOutput>> result = servicehandlerImpl.tempServiceCreate(
                ServiceDataUtils.buildTempServiceCreateInput());
        result.addListener(() -> endSignal.countDown(), executorService);

        endSignal.await();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

}
