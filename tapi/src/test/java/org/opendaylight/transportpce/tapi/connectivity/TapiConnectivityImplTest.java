package org.opendaylight.transportpce.tapi.connectivity;

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
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TapiConnectivityImplTest extends AbstractTest {

    @Mock
    private OrgOpenroadmServiceService serviceHandler;

    @Mock
    private TapiPceListenerImpl pceListenerImpl;

    @Mock
    private TapiRendererListenerImpl rendererListenerImpl;

    @Mock
    private TapiServiceHandlerListenerImpl serviceHandlerListenerImpl;

    private ServiceDataStoreOperations serviceDataStoreOperations;
    public static TapiContext tapiContext;
    public static ConnectivityUtils connectivityUtils;
    public static NetworkTransactionService networkTransactionService;
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
        networkTransactionService = new NetworkTransactionImpl(
            new RequestProcessor(getDataStoreContextUtil().getDataBroker()));
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getDataStoreContextUtil().getDataBroker());
        tapiContext = new TapiContext(networkTransactionService);
        connectivityUtils = new ConnectivityUtils(serviceDataStoreOperations, new HashMap<>(), tapiContext);
    }

    @Test
    public void createConnServiceShouldBeFailedWithEmptyInput() throws ExecutionException, InterruptedException {
        TapiConnectivityImpl tapiConnectivity = new TapiConnectivityImpl(serviceHandler, tapiContext, connectivityUtils,
            pceListenerImpl, rendererListenerImpl, serviceHandlerListenerImpl);

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
}
