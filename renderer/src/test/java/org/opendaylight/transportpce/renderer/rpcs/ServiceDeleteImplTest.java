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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutputBuilder;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.RpcResult;

@ExtendWith(MockitoExtension.class)
class ServiceDeleteImplTest {
    @Mock
    private RendererServiceOperations rendererServiceOperations;
    private ServiceDeleteImpl serviceDelete;
    private ServiceDeleteInput input;

    @BeforeEach
    void setup() {
        serviceDelete = new ServiceDeleteImpl(rendererServiceOperations, 1);
        input = new ServiceDeleteInputBuilder()
            .setServiceName("service1")
            .build();
    }

    @Test
    void testSuccessfulDeleteReturnsOutput() throws Exception {
        ServiceDeleteOutput expected = new ServiceDeleteOutputBuilder().build();
        when(rendererServiceOperations.serviceDelete(input, null))
            .thenReturn(Futures.immediateFuture(expected));

        RpcResult<ServiceDeleteOutput> result = serviceDelete.invoke(input).get();

        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testFailedDeleteFailsTheRpc() {
        IllegalStateException failure = new IllegalStateException("device unreachable");
        when(rendererServiceOperations.serviceDelete(input, null))
            .thenReturn(Futures.immediateFailedFuture(failure));

        ListenableFuture<RpcResult<ServiceDeleteOutput>> output = serviceDelete.invoke(input);

        ExecutionException thrown = assertThrows(ExecutionException.class, output::get);
        assertSame(failure, thrown.getCause());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testInvokeDoesNotWaitForDelete() throws Exception {
        SettableFuture<ServiceDeleteOutput> pending = SettableFuture.create();
        when(rendererServiceOperations.serviceDelete(input, null)).thenReturn(pending);

        ListenableFuture<RpcResult<ServiceDeleteOutput>> output = serviceDelete.invoke(input);

        // invoke() must return before the renderer has finished
        assertFalse(output.isDone());

        ServiceDeleteOutput expected = new ServiceDeleteOutputBuilder().build();
        pending.set(expected);

        assertTrue(output.isDone());
        assertSame(expected, output.get().getResult());
    }

    @Test
    void testRequestsOverTheLimitAreRejected() throws Exception {
        SettableFuture<ServiceDeleteOutput> pending = SettableFuture.create();
        when(rendererServiceOperations.serviceDelete(input, null))
            .thenReturn(pending)
            .thenReturn(Futures.immediateFuture(new ServiceDeleteOutputBuilder().build()));

        final ListenableFuture<RpcResult<ServiceDeleteOutput>> first = serviceDelete.invoke(input);
        // The limit is 1 and the first request is still in progress
        RpcResult<ServiceDeleteOutput> rejected = serviceDelete.invoke(input).get();

        assertFalse(rejected.isSuccessful());
        assertEquals(ErrorTag.RESOURCE_DENIED, rejected.getErrors().get(0).getTag());
        verify(rendererServiceOperations, times(1)).serviceDelete(input, null);

        // Once the first request completes, a new one is accepted
        pending.set(new ServiceDeleteOutputBuilder().build());
        assertTrue(first.get().isSuccessful());
        assertTrue(serviceDelete.invoke(input).get().isSuccessful());
    }

    @Test
    void testFailedDeleteFreesItsPlace() throws Exception {
        when(rendererServiceOperations.serviceDelete(input, null))
            .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("device unreachable")))
            .thenReturn(Futures.immediateFuture(new ServiceDeleteOutputBuilder().build()));

        assertThrows(ExecutionException.class, () -> serviceDelete.invoke(input).get());
        assertTrue(serviceDelete.invoke(input).get().isSuccessful());
    }
}
