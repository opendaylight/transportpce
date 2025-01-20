/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.CreateConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.DeleteConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceNotificationHandler;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiConnectivityDataUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDelete;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class TapiConnectivityImplTest extends AbstractTest {

    @Mock
    private PathComputationService pathComputationService;
    @Mock
    private RendererServiceOperations rendererServiceOperations;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private TapiPceNotificationHandler tapipceNotificationHandler;
    @Mock
    private TapiRendererNotificationHandler tapirendererNotificationHandler;
    @Mock
    private TapiServiceNotificationHandler tapiserviceNotificationHandler;
    @Mock
    private PceListener pceListenerImpl;
    @Mock
    private RendererListener rendererListenerImpl;
    @Mock
    private NetworkListener networkModelListenerImpl;
    @Mock
    public CatalogDataStoreOperations catalogDataStoreOperations;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private RpcService rpcService;
    @Mock
    private ServiceCreate serviceCreate;
    @Mock
    private ServiceDelete serviceDelete;

    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityImplTest.class);
    private static ServiceDataStoreOperations serviceDataStoreOperations;
    private static TapiContext tapiContext;
    private static TopologyUtils topologyUtils;
    private static ConnectivityUtils connectivityUtils;
    private static TapiInitialORMapping tapiInitialORMapping;
    private static NetworkTransactionService networkTransactionService;
    private static TapiLink tapilink;
    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;

    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        // Need to have datastore populated to enable the mapping from TAPI to OR
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II.toIdentifier());
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);

        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapilink = new TapiLinkImpl(networkTransactionService, new TapiContext(networkTransactionService));
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getDataStoreContextUtil().getDataBroker());
        tapiContext = new TapiContext(networkTransactionService);
        topologyUtils = new TopologyUtils(networkTransactionService, getDataStoreContextUtil().getDataBroker(),
            tapilink);
        connectivityUtils = new ConnectivityUtils(serviceDataStoreOperations, new HashMap<>(), tapiContext,
            networkTransactionService);
        tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
            tapiContext, serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        LOG.info("setup done");
    }

    @Test
    void createConnServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        when(rpcService.getRpc(any()))
            .thenReturn(new CreateConnectivityServiceImpl(rpcService, tapiContext, connectivityUtils,
                    tapipceNotificationHandler, tapirendererNotificationHandler));
        ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> result = rpcService
                .getRpc(CreateConnectivityService.class).invoke(new CreateConnectivityServiceInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<CreateConnectivityServiceOutput> rpcResult = result.get();
        assertEquals(ErrorType.RPC, rpcResult.getErrors().get(0).getErrorType());
    }

    @Test
    void createConnServiceShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        ConfigurationResponseCommon crc = new ConfigurationResponseCommonBuilder()
                .setRequestId("request 1")
                .setResponseCode("OK")
                .setAckFinalIndicator("requestProcessed").build();
        when(rpcService.getRpc(any())).thenReturn(serviceCreate);
        doReturn(RpcResultBuilder
                .success(new ServiceCreateOutputBuilder()
                    .setConfigurationResponseCommon(crc)
                    .build())
                .buildFuture())
            .when(serviceCreate).invoke(any());
        CreateConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceCreateInput();

        ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> result =
                    new CreateConnectivityServiceImpl(rpcService, tapiContext, connectivityUtils,
                        tapipceNotificationHandler, tapirendererNotificationHandler)
                .invoke(input);
        LOG.info("INPUT= {}", input.toString());
        RpcResult<CreateConnectivityServiceOutput> rpcResult = result.get();
        assertTrue(rpcResult.isSuccessful());
    }

    @Test
    void deleteConnServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        when(rpcService.getRpc(any()))
            .thenReturn(new DeleteConnectivityServiceImpl(rpcService, tapiContext, networkTransactionService));
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result = rpcService
                .getRpc(DeleteConnectivityService.class).invoke(new DeleteConnectivityServiceInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        assertEquals(ErrorType.RPC, rpcResult.getErrors().get(0).getErrorType());
    }

    @Test
    void deleteConnServiceShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        when(rpcService.getRpc(any()))
            .thenReturn(new DeleteConnectivityServiceImpl(rpcService, tapiContext, networkTransactionService));
        DeleteConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceDeleteInput1();
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result = rpcService
                .getRpc(DeleteConnectivityService.class).invoke(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        assertEquals(ErrorType.RPC, rpcResult.getErrors().get(0).getErrorType());
    }

    @Test
    void deleteConnServiceShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        ConfigurationResponseCommon crc = new ConfigurationResponseCommonBuilder()
            .setRequestId("request 1")
            .setResponseCode("OK")
            .setAckFinalIndicator("requestProcessed").build();
        when(rpcService.getRpc(any())).thenReturn(serviceDelete);
        doReturn(RpcResultBuilder
                .success(new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .ServiceDeleteOutputBuilder()
                    .setConfigurationResponseCommon(crc)
                    .build())
                .buildFuture())
            .when(serviceDelete).invoke(any());
        ServiceCreateInput createInput = TapiConnectivityDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);
        tapiContext.updateConnectivityContext(TapiConnectivityDataUtils.createConnService(), new HashMap<>());
        DeleteConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceDeleteInput();
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
                new DeleteConnectivityServiceImpl(rpcService, tapiContext, networkTransactionService).invoke(input);
        LOG.debug("RESULT = {}", result.toString());
        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        assertTrue(rpcResult.isSuccessful());
    }
}
