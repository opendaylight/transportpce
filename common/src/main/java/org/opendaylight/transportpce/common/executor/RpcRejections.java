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
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Turns tasks rejected by a bounded pool into RPC errors for the client.
 */
public interface RpcRejections {

    /**
     * Returns the error for a task that the pool rejected.
     *
     * @param executor the executor that rejected the task, which tells a full pool from one that has been shut down
     * @param pool the pool that rejected the task, named in the error message
     * @param cause the rejection
     * @param <T> type of the RPC output
     * @return an already failed RPC result
     */
    <T> ListenableFuture<RpcResult<T>> rejected(
            ListeningExecutorService executor,
            ThreadPoolName pool,
            RejectedExecutionException cause);
}
