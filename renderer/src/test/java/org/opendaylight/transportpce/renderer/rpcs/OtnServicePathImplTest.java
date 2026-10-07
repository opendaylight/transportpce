/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

@ExtendWith(MockitoExtension.class)
class OtnServicePathImplTest {

    @Mock
    private OtnDeviceRendererService otnDeviceRendererService;
    @Mock
    private RpcRejections rpcRejections;

    private OtnServicePathImpl otnServicePath(ListeningExecutorService executor) {
        return new OtnServicePathImpl(otnDeviceRendererService, executor, rpcRejections);
    }

    private static OtnServicePathInput input(Action operation) {
        // "ODU" at rate 100 maps to service type ODU4 in ServiceTypes.getOtnServiceType()
        return new OtnServicePathInputBuilder()
            .setOperation(operation)
            .setServiceFormat("ODU")
            .setServiceRate(Uint32.valueOf(100))
            .build();
    }

    @Test
    void testCreateRunsSetupOnExecutor() throws Exception {
        OtnServicePathInput input = input(Action.Create);
        OtnServicePathOutput expected = new OtnServicePathOutputBuilder().setSuccess(true).build();
        when(otnDeviceRendererService.setupOtnServicePath(input, StringConstants.SERVICE_TYPE_ODU4))
            .thenReturn(expected);

        RpcResult<OtnServicePathOutput> result =
            otnServicePath(MoreExecutors.newDirectExecutorService()).invoke(input).get();

        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testDeleteRunsDeleteOnExecutor() throws Exception {
        OtnServicePathInput input = input(Action.Delete);
        OtnServicePathOutput expected = new OtnServicePathOutputBuilder().setSuccess(true).build();
        when(otnDeviceRendererService.deleteOtnServicePath(input, StringConstants.SERVICE_TYPE_ODU4))
            .thenReturn(expected);

        RpcResult<OtnServicePathOutput> result =
            otnServicePath(MoreExecutors.newDirectExecutorService()).invoke(input).get();

        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testCancellingTheReplyKeepsThePoolThreadUntilTheDeviceWorkEnds() throws Exception {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        try {
            OtnServicePathInput input = input(Action.Delete);
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicBoolean interrupted = new AtomicBoolean();
            when(otnDeviceRendererService.deleteOtnServicePath(input, StringConstants.SERVICE_TYPE_ODU4))
                .thenAnswer(invocation -> {
                    started.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        interrupted.set(true);
                    }
                    return new OtnServicePathOutputBuilder().setSuccess(true).build();
                });
            ListenableFuture<RpcResult<OtnServicePathOutput>> reply = otnServicePath(executor).invoke(input);
            assertTrue(started.await(5, TimeUnit.SECONDS));

            assertTrue(reply.cancel(true));

            // The device work goes on, so the pool's only thread stays busy until it ends
            ListenableFuture<?> next = executor.submit(() -> { });
            assertThrows(TimeoutException.class, () -> next.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            next.get(5, TimeUnit.SECONDS);
            assertFalse(interrupted.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testMissingArgumentsAreRejectedWithoutTouchingDevices() throws Exception {
        OtnServicePathInput input = new OtnServicePathInputBuilder()
            .setOperation(Action.Create)
            .build();

        RpcResult<OtnServicePathOutput> result =
            otnServicePath(MoreExecutors.newDirectExecutorService()).invoke(input).get();

        assertEquals("Invalid operation", result.getResult().getResult());
        verifyNoInteractions(otnDeviceRendererService, rpcRejections);
    }

    @Test
    void testRejectedSubmissionIsDelegatedToRpcRejections() {
        // A shut-down executor rejects every submit(), so no mocked executor is needed
        ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        executor.shutdown();
        ListenableFuture<RpcResult<OtnServicePathOutput>> rejection = SettableFuture.create();
        when(rpcRejections.<OtnServicePathOutput>rejected(same(executor), same(ThreadPoolName.OTN_SERVICE_PATH),
                any(RejectedExecutionException.class)))
            .thenReturn(rejection);

        ListenableFuture<RpcResult<OtnServicePathOutput>> output =
            otnServicePath(executor).invoke(input(Action.Create));

        assertSame(rejection, output);
        verify(rpcRejections).rejected(same(executor), same(ThreadPoolName.OTN_SERVICE_PATH),
                any(RejectedExecutionException.class));
        verifyNoInteractions(otnDeviceRendererService);
    }
}
