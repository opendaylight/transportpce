/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.limit.OperationLimit;
import org.opendaylight.transportpce.common.executor.limit.OperationPermit;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Limits how many requests of an RPC can be in progress at once.
 *
 * <p>For RPCs that hand their work to the renderer and reply when it completes. The RPC no longer holds a RESTCONF
 * thread while it waits, so without a limit, requests against an unresponsive device would pile up in the renderer's
 * queue. Requests over the limit fail at once with an {@code application} / {@code resource-denied} error.
 *
 * <p>Only the RPC is limited. The service handler calls the renderer directly and is not affected.
 */
final class RequestLimit {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLimit.class);

    private final String rpcName;

    private final int maxRequests;

    private final OperationLimit limit;

    private final RpcRejections rpcRejections;

    /**
     * Creates a limit.
     *
     * @param rpcName name of the RPC, used in the error message and the log
     * @param maxRequests maximum number of requests in progress
     * @param rpcRejections builds the error for a rejected request
     */
    RequestLimit(String rpcName, int maxRequests, RpcRejections rpcRejections) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests must be positive, was " + maxRequests);
        }
        this.rpcName = rpcName;
        this.maxRequests = maxRequests;
        this.limit = new OperationLimit(maxRequests);
        this.rpcRejections = Objects.requireNonNull(rpcRejections, "rpcRejections cannot be null");
    }

    /**
     * Logs the incoming request and starts it if the limit allows it, as {@link #run(AsyncCallable)} does.
     *
     * @param serviceName name of the service the request is for, logged with the RPC's name
     * @param request starts the request and returns its result
     * @return the result of the request, or a failed RPC result if too many requests are in progress
     */
    <T> ListenableFuture<RpcResult<T>> run(String serviceName, AsyncCallable<RpcResult<T>> request) {
        LOG.info("Calling RPC {} request {}", rpcName, serviceName);
        return run(request);
    }

    /**
     * Starts the request if the limit allows it, and counts it as in progress until its future completes.
     *
     * <p>Cancelling the returned future does not cancel the request. A cancelled request would release its place
     * while its work stays queued or keeps running in the renderer, so requests could pile up there regardless of
     * the limit. The request instead keeps its place until its work completes.
     *
     * @param request starts the request and returns its result
     * @return the result of the request, or a failed RPC result if too many requests are in progress
     */
    <T> ListenableFuture<RpcResult<T>> run(AsyncCallable<RpcResult<T>> request) {
        Optional<OperationPermit> permit = limit.tryAcquire();
        if (permit.isEmpty()) {
            LOG.warn("Too many {} requests in progress (limit {}), rejecting", rpcName, maxRequests);
            return rpcRejections.tooManyRequests(rpcName);
        }
        // submitAsync turns an exception thrown while starting the request into a failed future, so the permit is
        // always released
        ListenableFuture<RpcResult<T>> result = Futures.submitAsync(request, MoreExecutors.directExecutor());
        result.addListener(permit.orElseThrow()::release, MoreExecutors.directExecutor());
        return Futures.nonCancellationPropagating(result);
    }
}
