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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPm;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the implementation of the 'get-pm' RESTCONF service, which
 * is one of the external APIs into the olm application.
 *
 *<p>This operation traverse through current PM list and gets PM for
 * given NodeId and Resource name
 */
public final class GetPmImpl implements GetPm {
    private static final Logger LOG = LoggerFactory.getLogger(GetPmImpl.class);
    private final OlmPowerService olmPowerService;
    private final ListeningExecutorService executor;
    private final RpcRejections rpcRejections;

    public GetPmImpl(OlmPowerService olmPowerService, ListeningExecutorService executor, RpcRejections rpcRejections) {
        this.olmPowerService = requireNonNull(olmPowerService);
        this.executor = requireNonNull(executor);
        this.rpcRejections = requireNonNull(rpcRejections);
    }

    @Override
    public ListenableFuture<RpcResult<GetPmOutput>> invoke(final GetPmInput input) {
        if (input.getNodeId() == null) {
            LOG.error("getPm: NodeId can not be null");
            return RpcResultBuilder.<GetPmOutput>failed()
                    .withError(ErrorType.RPC, "Error with input parameters")
                    .buildFuture();
        }

        try {
            return executor.submit(() -> RpcResultBuilder.success(olmPowerService.getPm(input)).build());
        } catch (RejectedExecutionException e) {
            return rpcRejections.rejected(executor, ThreadPoolName.OLM_GET_PM, e);
        }
    }
}
