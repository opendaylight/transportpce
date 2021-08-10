/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531.RpcStatus;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class ServicehandlerImplTest extends AbstractTest  {

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
    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
        MockitoAnnotations.openMocks(this);
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getNewDataBroker());
    }

    @Test
    public void createServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
            new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =
            servicehandlerImpl.serviceCreate(new ServiceCreateInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceCreateOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void createServiceShouldBeSuccessfulWhenPreformPCESuccessful()
        throws ExecutionException, InterruptedException {
        ServiceCreateInput input = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl =
            new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =  servicehandlerImpl.serviceCreate(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceCreateOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
            new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result =
            servicehandlerImpl.serviceDelete(new ServiceDeleteInputBuilder()
                .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder().setServiceName("").build()).build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServiceDeleteInput input = ServiceDataUtils.buildServiceDeleteInput();
        ServicehandlerImpl servicehandlerImpl =
            new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result = servicehandlerImpl.serviceDelete(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getNewDataBroker();
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl =
            new ServicehandlerImpl(dataBroker, pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                serviceDataStoreOperations);
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);
        ServiceDeleteInput input = ServiceDataUtils.buildServiceDeleteInput();
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result = servicehandlerImpl.serviceDelete(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }


    @Test
    public void serviceFeasibilityCheckShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> result =
                servicehandlerImpl.serviceFeasibilityCheck(new ServiceFeasibilityCheckInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceFeasibilityCheckOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceFeasibilityCheckShouldBeSuccessfulWhenPreformPCESuccessful()
            throws ExecutionException, InterruptedException {
        ServiceFeasibilityCheckInput input = ServiceDataUtils.buildServiceFeasibilityCheckInput();
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> result =
            servicehandlerImpl.serviceFeasibilityCheck(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceFeasibilityCheckOutput> rpcResult = result.get();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceReconfigureShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result =
                servicehandlerImpl.serviceReconfigure(new ServiceReconfigureInputBuilder().setServiceName("").build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceReconfigureOutput> rpcResult = result.get();
        Assert.assertEquals(
                RpcStatus.Failed, rpcResult.getResult().getStatus());
    }


    @Test
    public void serviceReconfigureShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServiceReconfigureInput input = ServiceDataUtils.buildServiceReconfigureInput();

        //action -> service reconfigure
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result = servicehandlerImpl.serviceReconfigure(input);

        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceReconfigureOutput> rpcResult = result.get();
        //ServiceReconfigureOutput doesn't have ConfigurationResponseCommon but have RpcStatus directly
        Assert.assertEquals(
                RpcStatus.Failed, rpcResult.getResult().getStatus());
    }

    @Test
    public void serviceReconfigureShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getNewDataBroker();

        //mocking
        // serviceReconfigure is calling service delete method in renderer
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to reconfigure
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(dataBroker, pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);

        //service reconfigure test action
        ServiceReconfigureInput input = ServiceDataUtils.buildServiceReconfigureInput();
        //ServiceReconfigureInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceReconfigureOutput>> result = servicehandlerImpl.serviceReconfigure(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceReconfigureOutput> rpcResult = result.get();
        Assert.assertEquals(
                RpcStatus.Successful, rpcResult.getResult().getStatus());
    }

    @Test
    public void serviceReRestorationShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result =
                servicehandlerImpl.serviceRestoration(new ServiceRestorationInputBuilder().setServiceName("").build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceRestorationOutput> rpcResult = result.get();
        Assert.assertEquals(
                RpcStatus.Failed, rpcResult.getResult().getStatus());
    }


    @Test
    public void serviceRestorationShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServiceRestorationInput input = ServiceDataUtils.buildServiceRestorationInput();

        //action -> service restore
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result = servicehandlerImpl.serviceRestoration(input);

        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceRestorationOutput> rpcResult = result.get();
        //ServiceRestorationOutput doesn't have ConfigurationResponseCommon but have RpcStatus directly
        Assert.assertEquals(
                RpcStatus.Failed, rpcResult.getResult().getStatus());
    }

    @Test
    public void serviceRestorationShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getNewDataBroker();

        //mocking
        // serviceRestoration is calling service delete method in renderer
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to restore
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(dataBroker, pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);

        //service Restoration test action
        ServiceRestorationInput input = ServiceDataUtils.buildServiceRestorationInput();
        //ServiceRestorationInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceRestorationOutput>> result = servicehandlerImpl.serviceRestoration(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceRestorationOutput> rpcResult = result.get();
        Assert.assertEquals(
                RpcStatus.Successful, rpcResult.getResult().getStatus());
    }

    @Test
    public void serviceRerouteShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result =
                servicehandlerImpl.serviceReroute(new ServiceRerouteInputBuilder().setServiceName("").build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceRerouteOutput> rpcResult = result.get();
        Assert.assertEquals(
                RpcStatus.Failed, rpcResult.getResult().getStatus());
    }

    @Test
    public void serviceRerouteShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServiceRerouteInput input = ServiceDataUtils.buildServiceRerouteInput();

        //action -> service reconfigure
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result = servicehandlerImpl.serviceReroute(input);

        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceRerouteOutput> rpcResult = result.get();
        //ServiceRerouteOutput doesn't have ConfigurationResponseCommon but have RpcStatus directly
        Assert.assertEquals(
                RpcStatus.Failed, rpcResult.getResult().getStatus());
    }

    @Test
    public void serviceRerouteShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getNewDataBroker();

        //mocking
        // serviceReroute is calling service delete method in renderer
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));
        //create service to be rerouted later
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(dataBroker, pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ServiceCreateInput createInput = ServiceDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);

        //service reroute test action
        ServiceRerouteInput input = ServiceDataUtils.buildServiceRerouteInput();
        //ServiceRerouteInput is created with the same service information that is created before
        ListenableFuture<RpcResult<ServiceRerouteOutput>> result = servicehandlerImpl.serviceReroute(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceRerouteOutput> rpcResult = result.get();
        Assert.assertEquals(
                RpcStatus.Successful, rpcResult.getResult().getStatus());
    }

    @Test
    public void tempServiceDeleteShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result =
                servicehandlerImpl.tempServiceDelete(new TempServiceDeleteInputBuilder()
                        .setCommonId("").build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<TempServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
        Assert.assertEquals(
            LogMessages.SERVICE_NON_COMPLIANT,
            rpcResult.getResult().getConfigurationResponseCommon().getResponseMessage());
    }

    @Test
    public void tempServiceDeleteShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        TempServiceDeleteInput input = ServiceDataUtils.buildTempServiceDeleteInput();
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result = servicehandlerImpl.tempServiceDelete(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<TempServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void tempServiceDeleteShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getNewDataBroker();
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));

        //create temp service to delete in the temp delete action
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(dataBroker, pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        TempServiceCreateInput createInput = ServiceDataUtils.buildTempServiceCreateInput();
        serviceDataStoreOperations.createTempService(createInput);


        TempServiceDeleteInput input = ServiceDataUtils.buildTempServiceDeleteInput(createInput.getCommonId());
        ListenableFuture<RpcResult<TempServiceDeleteOutput>> result = servicehandlerImpl.tempServiceDelete(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<TempServiceDeleteOutput> rpcResult = result.get();
        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void tempServiceCreateShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);
        ListenableFuture<RpcResult<TempServiceCreateOutput>> result =
                servicehandlerImpl.tempServiceCreate(new TempServiceCreateInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<TempServiceCreateOutput> rpcResult = result.get();
        Assert.assertEquals(
            ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }


    @Test
    public void tempServiceCreateShouldBeSuccessfulWhenPreformPCESuccessful()
            throws ExecutionException, InterruptedException {
        TempServiceCreateInput input = ServiceDataUtils.buildTempServiceCreateInput();
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));

        ServicehandlerImpl servicehandlerImpl =
                new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                        notificationPublishService, pceListenerImpl, rendererListenerImpl, networkModelListenerImpl,
                        serviceDataStoreOperations);

        ListenableFuture<RpcResult<TempServiceCreateOutput>> result =  servicehandlerImpl.tempServiceCreate(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<TempServiceCreateOutput> rpcResult = result.get();

        Assert.assertEquals(
                ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

}
