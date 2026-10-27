/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.rpc.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.pool.ThreadPoolName;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Tests that get-pm reads the device on the given executor instead of the calling thread, and turns a rejected
 * submission into the error built by {@link RpcRejections}.
 */
@ExtendWith(MockitoExtension.class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class GetPmImplTest {

    private static final GetPmInput INPUT = new GetPmInputBuilder().setNodeId("XPDR-A1").build();

    @Mock
    private OlmPowerService olmPowerService;
    @Mock
    private RpcRejections rpcRejections;

    private ListeningExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        release = new CountDownLatch(1);
    }

    @AfterEach
    void cleanup() {
        release.countDown();
        executor.shutdownNow();
    }

    @Test
    void testGetPmDoesNotWaitForTheDevice() throws Exception {
        GetPmOutput expected = new GetPmOutputBuilder().build();
        CountDownLatch reading = new CountDownLatch(1);
        when(olmPowerService.getPm(any())).thenAnswer(invocation -> {
            reading.countDown();
            release.await();
            return expected;
        });

        ListenableFuture<RpcResult<GetPmOutput>> output =
            new GetPmImpl(olmPowerService, executor, rpcRejections).invoke(INPUT);

        // invoke() must return while the worker is still blocked reading the device
        assertTrue(reading.await(5, TimeUnit.SECONDS), "the read did not start");
        assertFalse(output.isDone());

        release.countDown();
        RpcResult<GetPmOutput> result = output.get();
        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testRejectionIsDelegatedToRpcRejections() {
        // A shut-down executor rejects every submit()
        ListeningExecutorService rejecting = MoreExecutors.newDirectExecutorService();
        rejecting.shutdown();
        ListenableFuture<RpcResult<GetPmOutput>> rejection = SettableFuture.create();
        when(rpcRejections.<GetPmOutput>rejected(same(rejecting), same(ThreadPoolName.OLM_GET_PM),
                any(RejectedExecutionException.class)))
            .thenReturn(rejection);

        ListenableFuture<RpcResult<GetPmOutput>> output =
            new GetPmImpl(olmPowerService, rejecting, rpcRejections).invoke(INPUT);

        assertSame(rejection, output);
        verifyNoInteractions(olmPowerService);
    }
}
