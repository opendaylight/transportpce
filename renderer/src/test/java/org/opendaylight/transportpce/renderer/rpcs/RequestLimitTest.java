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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

class RequestLimitTest {

    @Test
    void testLimitMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new RequestLimit("test-rpc", 0));
    }

    @Test
    void testRequestUnderTheLimitRuns() throws Exception {
        RequestLimit limit = new RequestLimit("test-rpc", 1);

        RpcResult<String> result = limit.run(() -> RpcResultBuilder.success("done").buildFuture()).get();

        assertTrue(result.isSuccessful());
        assertEquals("done", result.getResult());
    }

    @Test
    void testRequestsOverTheLimitAreRejectedWithoutRunning() throws Exception {
        RequestLimit limit = new RequestLimit("test-rpc", 1);
        SettableFuture<RpcResult<String>> pending = SettableFuture.create();
        limit.run(() -> pending);
        AtomicInteger started = new AtomicInteger();

        RpcResult<String> rejected = limit.run(() -> {
            started.incrementAndGet();
            return RpcResultBuilder.success("done").buildFuture();
        }).get();

        assertFalse(rejected.isSuccessful());
        assertEquals(0, started.get());
        RpcError error = rejected.getErrors().get(0);
        assertEquals(ErrorType.APPLICATION, error.getErrorType());
        assertEquals(ErrorTag.RESOURCE_DENIED, error.getTag());
        assertEquals("Too many test-rpc requests in progress", error.getMessage());
    }

    @Test
    void testCompletedRequestFreesItsPlace() throws Exception {
        RequestLimit limit = new RequestLimit("test-rpc", 1);
        SettableFuture<RpcResult<String>> pending = SettableFuture.create();
        ListenableFuture<RpcResult<String>> first = limit.run(() -> pending);
        assertFalse(first.isDone());

        pending.set(RpcResultBuilder.success("first").build());

        assertTrue(limit.run(() -> RpcResultBuilder.success("second").buildFuture()).get().isSuccessful());
    }

    @Test
    void testCancelledReplyKeepsItsPlaceUntilTheRequestCompletes() throws Exception {
        RequestLimit limit = new RequestLimit("test-rpc", 1);
        SettableFuture<RpcResult<String>> pending = SettableFuture.create();
        ListenableFuture<RpcResult<String>> first = limit.run(() -> pending);

        assertTrue(first.cancel(true));

        assertFalse(pending.isCancelled());
        assertFalse(limit.run(() -> RpcResultBuilder.success("second").buildFuture()).get().isSuccessful());
        pending.set(RpcResultBuilder.success("first").build());
        assertTrue(limit.run(() -> RpcResultBuilder.success("third").buildFuture()).get().isSuccessful());
    }

    @Test
    void testFailedRequestFreesItsPlace() throws Exception {
        RequestLimit limit = new RequestLimit("test-rpc", 1);
        IllegalStateException failure = new IllegalStateException("device unreachable");

        ListenableFuture<RpcResult<String>> failed = limit.run(() -> Futures.immediateFailedFuture(failure));

        ExecutionException thrown = assertThrows(ExecutionException.class, failed::get);
        assertSame(failure, thrown.getCause());
        assertTrue(limit.run(() -> RpcResultBuilder.success("done").buildFuture()).get().isSuccessful());
    }

    @Test
    void testRequestThatThrowsWhileStartingFreesItsPlace() throws Exception {
        RequestLimit limit = new RequestLimit("test-rpc", 1);
        IllegalStateException failure = new IllegalStateException("could not start");

        ListenableFuture<RpcResult<String>> failed = limit.run(() -> {
            throw failure;
        });

        ExecutionException thrown = assertThrows(ExecutionException.class, failed::get);
        assertSame(failure, thrown.getCause());
        assertTrue(limit.run(() -> RpcResultBuilder.success("done").buildFuture()).get().isSuccessful());
    }
}
