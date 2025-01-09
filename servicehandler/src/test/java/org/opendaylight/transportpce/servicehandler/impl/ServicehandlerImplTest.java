/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.utils.CatalogDataUtils;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddOpenroadmOperationalModesToCatalogOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.AddSpecificOperationalModesToCatalogOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

@ExtendWith(MockitoExtension.class)
public class ServicehandlerImplTest extends AbstractTest {
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private PathComputationService pathComputationService;
    @Mock
    private RendererServiceOperations rendererServiceOperations;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private PceListener pceListenerImpl;
    @Mock
    private RendererListener rendererListenerImpl;
    @Mock
    private NetworkListener networkModelListenerImpl;

    @Mock
    private PathDescription pathDescription;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private CatalogDataStoreOperations catalogDataStoreOperations;
    private ServiceCreateInput serviceCreateInput;
    private ServiceDeleteInput serviceDeleteInput;
    private ServiceReconfigureInput serviceReconfigureInput;
    private ServiceRestorationInput serviceRestorationInput;
    private ServiceRerouteInput serviceRerouteInput;
    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private PCEServiceWrapper pceServiceWrapper;
    private RendererServiceWrapper rendererServiceWrapper;

    @BeforeEach
    void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getNewDataBroker());
        this.catalogDataStoreOperations = new CatalogDataStoreOperationsImpl(
                new NetworkTransactionImpl(getDataBroker()),
                getDataStoreContextUtil().getBindingDOMCodecServices());
        serviceCreateInput = ServiceDataUtils.buildServiceCreateInput();
        serviceDeleteInput = ServiceDataUtils.buildServiceDeleteInput();
        serviceReconfigureInput = ServiceDataUtils.buildServiceReconfigureInput();
        serviceRestorationInput = ServiceDataUtils.buildServiceRestorationInput();
        serviceRerouteInput = ServiceDataUtils.buildServiceRerouteInput();
        pathDescription = ServiceDataUtils.createPathDescription(0,1,0,1);
        pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.rendererServiceWrapper = new RendererServiceWrapper(rendererServiceOperations, notificationPublishService);
    }

    @Test
    void testRpcRegistration() {
        new ServicehandlerImpl(rpcProviderService, serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, catalogDataStoreOperations, pathComputationService, rendererServiceOperations,
                notificationPublishService);
        verify(rpcProviderService, times(1)).registerRpcImplementations(any(ClassToInstanceMap.class));
    }

    @Test
    void createServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =
            new ServiceCreateImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                    networkModelListenerImpl, pceServiceWrapper, notificationPublishService)
                .invoke(new ServiceCreateInputBuilder().build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void createServiceShouldBeFailedWithServiceAlreadyExist() throws ExecutionException, InterruptedException {
        final ServiceDataStoreOperations serviceDSOperations = mock(ServiceDataStoreOperations.class);
        when(serviceDSOperations.getService(serviceCreateInput.getServiceName()))
                .thenReturn(Optional.of(
                        new ServicesBuilder()
                                .setServiceName(serviceCreateInput.getServiceName())
                                .build()));
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =
            new ServiceCreateImpl(serviceDSOperations, pceListenerImpl, rendererListenerImpl,
                    networkModelListenerImpl, pceServiceWrapper, notificationPublishService)
                .invoke(serviceCreateInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void createServiceShouldBeSuccessfulWhenPerformPCESuccessful() throws ExecutionException, InterruptedException {
        when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =
                new ServiceCreateImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper, notificationPublishService)
                    .invoke(serviceCreateInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void deleteServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result =
                new ServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper, notificationPublishService)
                    .invoke(new ServiceDeleteInputBuilder()
                            .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                                    .setServiceName("")
                                    .build())
                            .build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void deleteServiceShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result =
                new ServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper, notificationPublishService)
                    .invoke(serviceDeleteInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void deleteServiceShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        serviceDataStoreOperations.createService(serviceCreateInput);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result =
                new ServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper, notificationPublishService)
                    .invoke(serviceDeleteInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceFeasibilityCheckShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> result =
                new ServiceFeasibilityCheckImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper)
                    .invoke(new ServiceFeasibilityCheckInputBuilder().build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceFeasibilityCheckShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> result =
                new ServiceFeasibilityCheckImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper)
                    .invoke(ServiceDataUtils.buildServiceFeasibilityCheckInput());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceReconfigureShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result =
                new ServiceReconfigureImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper)
                    .invoke(new ServiceReconfigureInputBuilder().setServiceName("").build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
    }


    @Test
    void serviceReconfigureShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        //action -> service reconfigure
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result =
                new ServiceReconfigureImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper)
                    .invoke(serviceReconfigureInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
    }

    @Test
    void serviceReconfigureShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        // serviceReconfigure is calling service delete method in renderer
        when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to reconfigure
        serviceDataStoreOperations.createService(serviceCreateInput);
        //service reconfigure test action
        //ServiceReconfigureInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result =
                new ServiceReconfigureImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper)
                    .invoke(serviceReconfigureInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
    }

    @Test
    void serviceReRestorationShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result =
                new ServiceRestorationImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper)
                    .invoke(new ServiceRestorationInputBuilder()
                            .setServiceName("")
                            .build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
    }

    @Test
    void serviceRestorationShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        //action -> service restore
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result =
                new ServiceRestorationImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper)
                    .invoke(serviceRestorationInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
    }

    @Test
    void serviceRestorationShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        // serviceRestoration is calling service delete method in renderer
        when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to restore
        serviceDataStoreOperations.createService(serviceCreateInput);
        //service Restoration test action
        //ServiceRestorationInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result =
                new ServiceRestorationImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper)
                    .invoke(serviceRestorationInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
    }

    @Test
    void serviceRerouteShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result =
                new ServiceRerouteImpl(serviceDataStoreOperations, pceServiceWrapper)
                    .invoke(new ServiceRerouteInputBuilder()
                            .setServiceName("")
                            .build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceRerouteShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        //action -> service reconfigure
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result =
                new ServiceRerouteImpl(serviceDataStoreOperations, pceServiceWrapper)
                    .invoke(serviceRerouteInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void serviceRerouteShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        when(pathComputationService.pathComputationRerouteRequest(any()))
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
        serviceDataStoreOperations.createService(serviceCreateInput);
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result =
                new ServiceRerouteImpl(serviceDataStoreOperations, pceServiceWrapper)
                    .invoke(serviceRerouteInput);
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void tempServiceDeleteShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result =
                new TempServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        rendererServiceWrapper)
                    .invoke(new TempServiceDeleteInputBuilder()
                            .setCommonId("")
                            .build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        RpcResult<TempServiceDeleteOutput> rpcResult = result.get();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals(
            LogMessages.SERVICE_NON_COMPLIANT,
            rpcResult.getResult().getConfigurationResponseCommon().getResponseMessage());
    }

    @Test
    void tempServiceDeleteShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result =
                new TempServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        rendererServiceWrapper)
                    .invoke(ServiceDataUtils.buildTempServiceDeleteInput());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void tempServiceDeleteShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create temp service to delete in the temp delete action
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        serviceDataStoreOperations.createTempService(createInput, pathDescription);
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result =
                new TempServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        rendererServiceWrapper)
                    .invoke(ServiceDataUtils.buildTempServiceDeleteInput(createInput.getCommonId()));
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void tempServiceCreateShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<TempServiceCreateOutput>> result =
                new TempServiceCreateImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper)
                    .invoke(new TempServiceCreateInputBuilder().build());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    void tempServiceCreateShouldBeFailedWithServiceAlreadyExist() throws ExecutionException, InterruptedException {
        final ServiceDataStoreOperations serviceDSOperations = mock(ServiceDataStoreOperations.class);
        when(serviceDSOperations.getTempService(any()))
            .thenReturn(Optional.of(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list
                            .ServicesBuilder()
                        .setCommonId("bad_commonId")
                        .build()));
        ListenableFuture<RpcResult<TempServiceCreateOutput>> result =
                new TempServiceCreateImpl(serviceDSOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper)
                    .invoke(ServiceDataUtils.buildTempServiceCreateInput());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
        assertEquals(
            "Service 'Temp (commonId)' already exists in datastore",
            result.get().getResult().getConfigurationResponseCommon().getResponseMessage());
    }

    @Test
    void tempServiceCreateShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ListenableFuture<RpcResult<TempServiceCreateOutput>> result =
                new TempServiceCreateImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper)
                    .invoke(ServiceDataUtils.buildTempServiceCreateInput());
        result.addListener(() -> endSignal.countDown(), executorService);
        endSignal.await();
        assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void addOpenroadmOperationalModesToCatalogShouldBeFailedWithEmptyInput()
            throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<AddOpenroadmOperationalModesToCatalogOutput>> result =
                new AddOpenroadmOperationalModesToCatalogImpl(catalogDataStoreOperations)
                    .invoke(new AddOpenroadmOperationalModesToCatalogInputBuilder().build());
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void addSpecificOperationalModesToCatalogShouldBeFailedWithEmptyInput()
            throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<AddSpecificOperationalModesToCatalogOutput>> result =
                new AddSpecificOperationalModesToCatalogImpl(catalogDataStoreOperations)
                    .invoke(new AddSpecificOperationalModesToCatalogInputBuilder().build());
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void addOpenroadmOperationalModesToCatalogShouldBeSuccessfulWhenAddORToCatalog()
            throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<AddOpenroadmOperationalModesToCatalogOutput>> result =
                new AddOpenroadmOperationalModesToCatalogImpl(catalogDataStoreOperations)
                    .invoke(CatalogDataUtils.buildAddORToCatalogInput());
        Assert.assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void addSpecificOperationalModesToCatalogShouldBeSuccessfulWhenAddSpecificToCatalog()
            throws ExecutionException, InterruptedException {
        ListenableFuture<RpcResult<AddSpecificOperationalModesToCatalogOutput>> result =
                new AddSpecificOperationalModesToCatalogImpl(catalogDataStoreOperations)
                    .invoke(CatalogDataUtils.buildAddSpecificToCatalogInput());
        Assert.assertEquals(
            ResponseCodes.RESPONSE_OK,
            result.get().getResult().getConfigurationResponseCommon().getResponseCode());
    }
}
