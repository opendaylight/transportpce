/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.rpc.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrent;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public class CalculateSpanlossCurrentImpl implements CalculateSpanlossCurrent {
    private final OlmPowerService olmPowerService;
    private final ListeningExecutorService executor;
    private final RpcRejections rpcRejections;

    public CalculateSpanlossCurrentImpl(
            final OlmPowerService olmPowerService,
            final ListeningExecutorService executor,
            final RpcRejections rpcRejections) {
        this.olmPowerService = requireNonNull(olmPowerService);
        this.executor = requireNonNull(executor);
        this.rpcRejections = requireNonNull(rpcRejections);
    }

    @Override
    public ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> invoke(CalculateSpanlossCurrentInput input) {
        try {
            // Returns at once; the spanloss calculation runs later on an executor thread
            return executor.submit(
                () -> RpcResultBuilder.success(olmPowerService.calculateSpanlossCurrent(input)).build());
        } catch (RejectedExecutionException e) {
            return rpcRejections.rejected(executor, ThreadPoolName.OLM_SPANLOSS, e);
        }
    }

}
