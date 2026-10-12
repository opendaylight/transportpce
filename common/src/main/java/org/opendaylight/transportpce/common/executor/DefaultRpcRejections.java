/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRpcRejections implements RpcRejections {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRpcRejections.class);

    @Override
    public <T> ListenableFuture<RpcResult<T>> rejected(
            ListeningExecutorService executor,
            ThreadPoolName pool,
            RejectedExecutionException cause) {

        if (executor.isShutdown()) {
            LOG.warn("Executor {} has been shut down", pool);
            return resourceDenied("The process " + pool.poolName() + " has been shut down");
        }
        LOG.warn("Too many {} requests in progress, rejecting", pool, cause);
        return tooManyRequests(pool.poolName());
    }

    /**
     * Returns the error for a request rejected because too many requests of the same kind are in progress.
     *
     * @param requestName name of the requests, named in the error message
     * @param <T> type of the RPC output
     * @return an already failed RPC result
     */
    public static <T> ListenableFuture<RpcResult<T>> tooManyRequests(String requestName) {
        return resourceDenied("Too many " + requestName + " requests in progress");
    }

    private static <T> ListenableFuture<RpcResult<T>> resourceDenied(String message) {
        return RpcResultBuilder.<T>failed()
                .withError(
                        ErrorType.APPLICATION,
                        ErrorTag.RESOURCE_DENIED,
                        message)
                .buildFuture();
    }
}
