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
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
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
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiConnectivityDataUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
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
    private TapiPceListenerImpl tapipceListenerImpl;
    @Mock
    private TapiRendererListenerImpl tapirendererListenerImpl;
    @Mock
    private TapiServiceHandlerListenerImpl tapiserviceHandlerListenerImpl;
    @Mock
    private PceListener pceListenerImpl;
    @Mock
    private RendererListener rendererListenerImpl;
    @Mock
    private NetworkListener networkModelListenerImpl;

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
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);

        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapilink = new TapiLinkImpl(networkTransactionService);
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
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations);

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl);

        ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> result =
            tapiConnectivity.createConnectivityService(new CreateConnectivityServiceInputBuilder().build());
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
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations);

        CreateConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceCreateInput();
        when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl);
        ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> result =
            tapiConnectivity.createConnectivityService(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<CreateConnectivityServiceOutput> rpcResult = result.get();
        assertTrue(rpcResult.isSuccessful());
    }

    @Test
    void deleteConnServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations);

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl);

        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
            tapiConnectivity.deleteConnectivityService(new DeleteConnectivityServiceInputBuilder().build());
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
        DeleteConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceDeleteInput1();
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations);

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl);
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
            tapiConnectivity.deleteConnectivityService(input);
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
        when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));

        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations);

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl);

        ServiceCreateInput createInput = TapiConnectivityDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);
        tapiContext.updateConnectivityContext(TapiConnectivityDataUtils.createConnService(), new HashMap<>());

        DeleteConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceDeleteInput();
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
            tapiConnectivity.deleteConnectivityService(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        assertTrue(rpcResult.isSuccessful());
    }
}