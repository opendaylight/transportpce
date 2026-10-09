/*
 * Copyright © 2019 Orange , Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.executor.DefaultRpcRejections;
import org.opendaylight.transportpce.common.executor.ExecutorPoolManager;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.CreateOtsOms;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.RendererRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePath;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

@ExtendWith(MockitoExtension.class)
public class DeviceRendererRPCImplTest {
    @Mock
    private DeviceRendererService deviceRendererService;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private DeviceRendererService deviceRenderer;
    @Mock
    private OtnDeviceRendererService otnDeviceRendererService;
    @Spy
    private ServicePathInput servicePathInput;
    @Mock
    private CreateOtsOmsInput createOtsOmsInput;
    @Mock
    private RendererRollbackInput rendererRollbackInput;
    private ServicePath servicePath;
    private RendererRollback rendererRollback;
    private CreateOtsOms createOtsOms;

    @BeforeEach
    void setup() {
        servicePath = new ServicePathImpl(
                deviceRenderer,
                MoreExecutors.newDirectExecutorService(),
                new DefaultRpcRejections());
        rendererRollback = new RendererRollbackImpl(deviceRenderer);
        createOtsOms = new CreateOtsOmsImpl(deviceRenderer);
    }

    @Test
    void testRpcRegistration() {
        ExecutorPoolManager pools = mock(ExecutorPoolManager.class);
        when(pools.executorService(any(), any(ThreadPoolName.class)))
                .thenReturn(MoreExecutors.newDirectExecutorService());

        new DeviceRendererRPCImpl(rpcProviderService, deviceRenderer, otnDeviceRendererService, pools);
        verify(rpcProviderService, times(1)).registerRpcImplementations(
                any(ServicePathImpl.class), any(OtnServicePathImpl.class), any(RendererRollbackImpl.class),
                any(CreateOtsOmsImpl.class));
    }

    @Test
    void testCloseShutsDownTheExecutors() {
        ExecutorPoolManager pools = mock(ExecutorPoolManager.class);
        when(pools.executorService(any(), any(ThreadPoolName.class)))
                .thenReturn(MoreExecutors.newDirectExecutorService());

        new DeviceRendererRPCImpl(mock(RpcProviderService.class, RETURNS_MOCKS), deviceRenderer,
                otnDeviceRendererService, pools).close();

        verify(pools).close();
    }

    @Test
    void testServicePathCreateOption() {
        when(servicePathInput.getOperation()).thenReturn(Action.Create);
        servicePath.invoke(servicePathInput);
        verify(deviceRenderer, times(1)).setupServicePath(servicePathInput, null);
    }

    @Test
    void testServicePathDeleteOption() {
        when(servicePathInput.getOperation()).thenReturn(Action.Delete);
        servicePath.invoke(servicePathInput);
        verify(deviceRenderer, times(1)).deleteServicePath(servicePathInput);
    }

    @Test
    void testCancellingTheReplyKeepsThePoolThreadUntilTheDeviceWorkEnds() throws Exception {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        try {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicBoolean interrupted = new AtomicBoolean();
            when(servicePathInput.getOperation()).thenReturn(Action.Delete);
            when(deviceRenderer.deleteServicePath(servicePathInput)).thenAnswer(invocation -> {
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
                return new ServicePathOutputBuilder().setSuccess(true).build();
            });
            ListenableFuture<RpcResult<ServicePathOutput>> reply =
                    new ServicePathImpl(deviceRenderer, executor, new DefaultRpcRejections()).invoke(servicePathInput);
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
    void testRendererRollback() {
        when(deviceRenderer.rendererRollback(rendererRollbackInput))
            .thenReturn(new RendererRollbackOutputBuilder().build());
        rendererRollback.invoke(rendererRollbackInput);
        verify(deviceRenderer, times(1)).rendererRollback(rendererRollbackInput);
    }

    @Test
    void testCreateOtsOms() throws OpenRoadmInterfaceException {
        when(createOtsOmsInput.getNodeId()).thenReturn("nodeId");
        when(createOtsOmsInput.getLogicalConnectionPoint()).thenReturn("logicalConnectionPoint");
        when(deviceRenderer.createOtsOms(createOtsOmsInput)).thenReturn(null);
        createOtsOms.invoke(createOtsOmsInput);
        verify(deviceRenderer, times(1)).createOtsOms(createOtsOmsInput);
    }

    @Test
    void testCreateOtsOmsReturnException()
            throws OpenRoadmInterfaceException, InterruptedException, ExecutionException {
        when(createOtsOmsInput.getNodeId()).thenReturn("nodeId");
        when(createOtsOmsInput.getLogicalConnectionPoint()).thenReturn("logicalConnectionPoint");
        when(deviceRenderer.createOtsOms(createOtsOmsInput)).thenThrow(OpenRoadmInterfaceException.class);
        ListenableFuture<RpcResult<CreateOtsOmsOutput>> result = createOtsOms.invoke(createOtsOmsInput);
        assertTrue(result.isDone());
        assertFalse(result.get().isSuccessful());
        assertNull(result.get().getResult());
        assertEquals("to create oms and ots interfaces", result.get().getErrors().get(0).getMessage());
    }
}
