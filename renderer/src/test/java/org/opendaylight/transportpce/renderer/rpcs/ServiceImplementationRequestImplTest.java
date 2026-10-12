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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.RpcResult;

@ExtendWith(MockitoExtension.class)
class ServiceImplementationRequestImplTest {
    @Mock
    private RendererServiceOperations rendererServiceOperations;
    private ServiceImplementationRequestImpl serviceImplementationRequest;
    private ServiceImplementationRequestInput input;

    @BeforeEach
    void setup() {
        serviceImplementationRequest = new ServiceImplementationRequestImpl(rendererServiceOperations, 1);
        input = new ServiceImplementationRequestInputBuilder()
            .setServiceName("service1")
            .build();
    }

    @Test
    void testSuccessfulImplementationReturnsOutput() throws Exception {
        ServiceImplementationRequestOutput expected = new ServiceImplementationRequestOutputBuilder().build();
        when(rendererServiceOperations.serviceImplementation(input, false))
            .thenReturn(Futures.immediateFuture(expected));

        RpcResult<ServiceImplementationRequestOutput> result = serviceImplementationRequest.invoke(input).get();

        assertTrue(result.isSuccessful());
        assertSame(expected, result.getResult());
    }

    @Test
    void testFailedImplementationFailsTheRpc() {
        IllegalStateException failure = new IllegalStateException("device unreachable");
        when(rendererServiceOperations.serviceImplementation(input, false))
            .thenReturn(Futures.immediateFailedFuture(failure));

        ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> output =
            serviceImplementationRequest.invoke(input);

        ExecutionException thrown = assertThrows(ExecutionException.class, output::get);
        assertSame(failure, thrown.getCause());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testInvokeDoesNotWaitForImplementation() throws Exception {
        SettableFuture<ServiceImplementationRequestOutput> pending = SettableFuture.create();

        // The mocked renderer returns a SettableFuture that is completed "by hand", meaning it stays pending
        // until the test calls set(...).
        when(rendererServiceOperations.serviceImplementation(input, false)).thenReturn(pending);

        ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> output =
            serviceImplementationRequest.invoke(input);

        // Check invoke() returns with the RPC future not yet done, meaning
        // invoke() must return BEFORE the renderer has finished
        assertFalse(output.isDone());

        ServiceImplementationRequestOutput expected = new ServiceImplementationRequestOutputBuilder().build();
        pending.set(expected);

        assertTrue(output.isDone());
        assertSame(expected, output.get().getResult());
    }

    @Test
    void testRequestsOverTheLimitAreRejected() throws Exception {
        SettableFuture<ServiceImplementationRequestOutput> pending = SettableFuture.create();
        when(rendererServiceOperations.serviceImplementation(input, false))
            .thenReturn(pending)
            .thenReturn(Futures.immediateFuture(new ServiceImplementationRequestOutputBuilder().build()));

        final ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> first =
            serviceImplementationRequest.invoke(input);
        // The limit is 1 and the first request is still in progress
        RpcResult<ServiceImplementationRequestOutput> rejected = serviceImplementationRequest.invoke(input).get();

        assertFalse(rejected.isSuccessful());
        assertEquals(ErrorTag.RESOURCE_DENIED, rejected.getErrors().get(0).getTag());
        verify(rendererServiceOperations, times(1)).serviceImplementation(input, false);

        // Once the first request completes, a new one is accepted
        pending.set(new ServiceImplementationRequestOutputBuilder().build());
        assertTrue(first.get().isSuccessful());
        assertTrue(serviceImplementationRequest.invoke(input).get().isSuccessful());
    }

    @Test
    void testFailedImplementationFreesItsPlace() throws Exception {
        when(rendererServiceOperations.serviceImplementation(input, false))
            .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("device unreachable")))
            .thenReturn(Futures.immediateFuture(new ServiceImplementationRequestOutputBuilder().build()));

        assertThrows(ExecutionException.class, () -> serviceImplementationRequest.invoke(input).get());
        assertTrue(serviceImplementationRequest.invoke(input).get().isSuccessful());
    }
}
