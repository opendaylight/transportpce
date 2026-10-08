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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Tests that the spanloss RPCs run their calculation on the given executor instead of the calling thread, and
 * turn a rejected submission into the error built by {@link RpcRejections}.
 */
@ExtendWith(MockitoExtension.class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class CalculateSpanlossImplTest {

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

    private static ListeningExecutorService rejectingExecutor() {
        // A shut-down executor rejects every submit()
        ListeningExecutorService rejecting = MoreExecutors.newDirectExecutorService();
        rejecting.shutdown();
        return rejecting;
    }

    @Test
    void testSpanlossBaseDoesNotWaitForTheCalculation() throws Exception {
        CalculateSpanlossBaseOutput expected = new CalculateSpanlossBaseOutputBuilder().build();
        when(olmPowerService.calculateSpanlossBase(any())).thenAnswer(invocation -> {
            release.await();
            return expected;
        });

        ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> output =
            new CalculateSpanlossBaseImpl(olmPowerService, executor, rpcRejections)
                .invoke(new CalculateSpanlossBaseInputBuilder().build());

        // invoke() must return while the calculation is still running
        assertFalse(output.isDone());

        release.countDown();
        RpcResult<CalculateSpanlossBaseOutput> result = output.get();
        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testSpanlossCurrentDoesNotWaitForTheCalculation() throws Exception {
        CalculateSpanlossCurrentOutput expected = new CalculateSpanlossCurrentOutputBuilder().build();
        when(olmPowerService.calculateSpanlossCurrent(any())).thenAnswer(invocation -> {
            release.await();
            return expected;
        });

        ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> output =
            new CalculateSpanlossCurrentImpl(olmPowerService, executor, rpcRejections)
                .invoke(new CalculateSpanlossCurrentInputBuilder().build());

        assertFalse(output.isDone());

        release.countDown();
        RpcResult<CalculateSpanlossCurrentOutput> result = output.get();
        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testSpanlossBaseFailureFailsTheRpc() {
        IllegalStateException failure = new IllegalStateException("device unreachable");
        when(olmPowerService.calculateSpanlossBase(any())).thenThrow(failure);

        ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> output =
            new CalculateSpanlossBaseImpl(olmPowerService, executor, rpcRejections)
                .invoke(new CalculateSpanlossBaseInputBuilder().build());

        ExecutionException thrown = assertThrows(ExecutionException.class, output::get);
        assertSame(failure, thrown.getCause());
    }

    @Test
    void testSpanlossBaseRejectionIsDelegatedToRpcRejections() {
        ListeningExecutorService rejecting = rejectingExecutor();
        ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> rejection = SettableFuture.create();
        when(rpcRejections.<CalculateSpanlossBaseOutput>rejected(same(rejecting), same(ThreadPoolName.OLM_SPANLOSS),
                any(RejectedExecutionException.class)))
            .thenReturn(rejection);

        CalculateSpanlossBaseInput input = new CalculateSpanlossBaseInputBuilder().build();
        ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> output =
            new CalculateSpanlossBaseImpl(olmPowerService, rejecting, rpcRejections).invoke(input);

        assertSame(rejection, output);
        verifyNoInteractions(olmPowerService);
    }

    @Test
    void testSpanlossCurrentRejectionIsDelegatedToRpcRejections() {
        ListeningExecutorService rejecting = rejectingExecutor();
        ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> rejection = SettableFuture.create();
        when(rpcRejections.<CalculateSpanlossCurrentOutput>rejected(same(rejecting), same(ThreadPoolName.OLM_SPANLOSS),
                any(RejectedExecutionException.class)))
            .thenReturn(rejection);

        CalculateSpanlossCurrentInput input = new CalculateSpanlossCurrentInputBuilder().build();
        ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> output =
            new CalculateSpanlossCurrentImpl(olmPowerService, rejecting, rpcRejections).invoke(input);

        assertSame(rejection, output);
        verify(rpcRejections).rejected(same(rejecting), same(ThreadPoolName.OLM_SPANLOSS),
                any(RejectedExecutionException.class));
        verifyNoInteractions(olmPowerService);
    }
}
