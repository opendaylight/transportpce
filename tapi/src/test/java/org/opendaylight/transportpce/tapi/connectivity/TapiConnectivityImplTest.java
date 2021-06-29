/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import static org.mockito.ArgumentMatchers.any;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
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
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiConnectivityDataUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceOutput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private PceListenerImpl pceListenerImpl;

    @Mock
    private RendererListenerImpl rendererListenerImpl;

    @Mock
    private NetworkModelListenerImpl networkModelListenerImpl;

    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityImplTest.class);
    public static ServiceDataStoreOperations serviceDataStoreOperations;
    public static TapiContext tapiContext;
    public static TopologyUtils topologyUtils;
    public static ConnectivityUtils connectivityUtils;
    public static TapiInitialORMapping tapiInitialORMapping;
    public static NetworkTransactionService networkTransactionService;
    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;

    @Before
    public void setUp() throws InterruptedException, ExecutionException {
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

        callbackRan = false;
        MockitoAnnotations.openMocks(this);

        networkTransactionService = new NetworkTransactionImpl(
            new RequestProcessor(getDataStoreContextUtil().getDataBroker()));
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getDataStoreContextUtil().getDataBroker());
        tapiContext = new TapiContext(networkTransactionService);
        topologyUtils = new TopologyUtils(networkTransactionService, getDataStoreContextUtil().getDataBroker());
        connectivityUtils = new ConnectivityUtils(serviceDataStoreOperations, new HashMap<>(), tapiContext);
        tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
            tapiContext, serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        LOG.info("setup done");
    }

    @Test
    public void createConnServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations, "ServiceHandler");

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl, tapiserviceHandlerListenerImpl);

        ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> result =
            tapiConnectivity.createConnectivityService(new CreateConnectivityServiceInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<CreateConnectivityServiceOutput> rpcResult = result.get();
        Assert.assertEquals(
            RpcError.ErrorType.RPC, rpcResult.getErrors().get(0).getErrorType());
    }

    @Test
    public void createConnServiceShouldBeSuccessfulWhenPerformPCESuccessful()
            throws ExecutionException, InterruptedException {
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations, "ServiceHandler");

        CreateConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceCreateInput();
        Mockito.when(pathComputationService.pathComputationRequest(any())).thenReturn(Futures.immediateFuture(any()));

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl, tapiserviceHandlerListenerImpl);
        ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> result =
            tapiConnectivity.createConnectivityService(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<CreateConnectivityServiceOutput> rpcResult = result.get();
        Assert.assertTrue(rpcResult.isSuccessful());
    }

    @Test
    public void deleteConnServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations, "ServiceHandler");

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl, tapiserviceHandlerListenerImpl);

        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
            tapiConnectivity.deleteConnectivityService(new DeleteConnectivityServiceInputBuilder().build());
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        Assert.assertEquals(
            RpcError.ErrorType.RPC, rpcResult.getErrors().get(0).getErrorType());
    }

    @Test
    public void deleteConnServiceShouldBeFailedWithNonExistService() throws ExecutionException, InterruptedException {
        DeleteConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceDeleteInput1();
        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations, "ServiceHandler");

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl, tapiserviceHandlerListenerImpl);
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
            tapiConnectivity.deleteConnectivityService(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        Assert.assertEquals(
            RpcError.ErrorType.RPC, rpcResult.getErrors().get(0).getErrorType());
    }

    @Test
    public void deleteConnServiceShouldBeSuccessForExistingService() throws ExecutionException, InterruptedException {
        Mockito.when(rendererServiceOperations.serviceDelete(any(), any())).thenReturn(Futures.immediateFuture(any()));

        OrgOpenroadmServiceService serviceHandler = new ServicehandlerImpl(getNewDataBroker(), pathComputationService,
            rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
            networkModelListenerImpl, serviceDataStoreOperations, "ServiceHandler");

        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            tapipceListenerImpl, tapirendererListenerImpl, tapiserviceHandlerListenerImpl);

        ServiceCreateInput createInput = TapiConnectivityDataUtils.buildServiceCreateInput();
        serviceDataStoreOperations.createService(createInput);
        tapiContext.updateConnectivityContext(TapiConnectivityDataUtils.createConnService(), new HashMap<>());

        DeleteConnectivityServiceInput input = TapiConnectivityDataUtils.buildConnServiceDeleteInput();
        ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> result =
            tapiConnectivity.deleteConnectivityService(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<DeleteConnectivityServiceOutput> rpcResult = result.get();
        Assert.assertTrue(rpcResult.isSuccessful());
    }
}
