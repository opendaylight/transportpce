/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.*;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;

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

    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, null);
        ListenableFuture<RpcResult<ServiceCreateOutput>> result =  servicehandlerImpl.serviceCreate(new ServiceCreateInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<ServiceCreateOutput> rpcResult = result.get();
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void createServiceShouldBeSuccessfulWhenPreformPCESuccessful() throws ExecutionException, InterruptedException {
        ServiceCreateInput input = ServiceDataUtils.buildServiceCreateInput();
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, null);
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
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, null);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> result = servicehandlerImpl.serviceDelete(new ServiceDeleteInputBuilder()
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
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        ServiceDeleteInput input = ServiceDataUtils.buildServiceDeleteInput();
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(getNewDataBroker(), pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, null);
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
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void deleteServiceShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        DataBroker dataBroker = getNewDataBroker();
        Mockito.when(rendererServiceOperations.serviceDelete(any())).thenReturn(Futures.immediateFuture(any()));
        ServicehandlerImpl servicehandlerImpl = new ServicehandlerImpl(dataBroker, pathComputationService, rendererServiceOperations,
                notificationPublishService, pceListenerImpl, rendererListenerImpl, null);
        ServiceDataStoreOperationsImpl serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(dataBroker);
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
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, rpcResult.getResult().getConfigurationResponseCommon().getResponseCode());
    }
}
