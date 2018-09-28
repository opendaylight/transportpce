/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.tapi.utils.TopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopologyImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImplTest.class);

    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 3;
    private boolean callbackRan;
    private DataStoreContext dataStoreContextUtil;

    @Before
    public void setUp() throws InterruptedException {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
        dataStoreContextUtil = new DataStoreContextImpl();
        TopologyDataUtils.writeTopologyFromFileToDatastore(dataStoreContextUtil);
        TopologyDataUtils.writePortmappingFromFileToDatastore(dataStoreContextUtil);
        LOG.info("setup done");
        Thread.sleep(1000);
    }

    @Test
    public void getTopologyDetailsWhenSuccessful() throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TopologyDataUtils.buildGetTopologyDetailsInput();
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(dataStoreContextUtil.getDataBroker());

        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);

        result.addListener(new Runnable() {
            @Override
            public void run() {
                callbackRan = true;
                endSignal.countDown();
            }
        }, executorService);

        endSignal.await();

        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        LOG.info("topo TAPI returned = {}", rpcResult.getResult().getTopology());
    }

}
