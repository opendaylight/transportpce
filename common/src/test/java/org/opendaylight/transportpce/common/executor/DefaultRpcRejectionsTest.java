/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

class DefaultRpcRejectionsTest {

    private final RpcRejections rpcRejections = new DefaultRpcRejections();
    private ListeningExecutorService executor;

    @BeforeEach
    void setup() {
        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @AfterEach
    void cleanup() {
        executor.shutdownNow();
    }

    @Test
    void testFullPoolIsReportedAsTooManyRequests() throws Exception {
        ListenableFuture<RpcResult<String>> result =
                rpcRejections.rejected(executor, ThreadPoolName.OLM_GET_PM,
                        new RejectedExecutionException("queue full"));

        assertTrue(result.isDone());
        assertResourceDenied(result.get(), "Too many olm-get-pm requests in progress");
    }

    @Test
    void testShutDownPoolIsReportedAsShutDown() throws Exception {
        executor.shutdown();

        ListenableFuture<RpcResult<String>> result =
                rpcRejections.rejected(executor, ThreadPoolName.OLM_GET_PM,
                        new RejectedExecutionException("shut down"));

        assertTrue(result.isDone());
        assertResourceDenied(result.get(), "The process olm-get-pm has been shut down");
    }

    @Test
    void testTooManyRequestsNamesTheRequests() throws Exception {
        ListenableFuture<RpcResult<String>> result = DefaultRpcRejections.tooManyRequests("service-delete");

        assertTrue(result.isDone());
        assertResourceDenied(result.get(), "Too many service-delete requests in progress");
    }

    private static void assertResourceDenied(RpcResult<String> result, String message) {
        assertFalse(result.isSuccessful());
        assertNull(result.getResult());
        assertEquals(1, result.getErrors().size());
        RpcError error = result.getErrors().get(0);
        assertEquals(ErrorType.APPLICATION, error.getErrorType());
        assertEquals(ErrorTag.RESOURCE_DENIED, error.getTag());
        assertEquals(message, error.getMessage());
    }
}
