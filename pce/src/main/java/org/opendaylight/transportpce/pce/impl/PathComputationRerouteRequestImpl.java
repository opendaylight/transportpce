/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PathComputationRerouteRequestImpl implements PathComputationRerouteRequest {
    private static final Logger LOG = LoggerFactory.getLogger(PathComputationRerouteRequestImpl.class);
    private PathComputationService pathComputationService;

    public PathComputationRerouteRequestImpl(final PathComputationService pathComputationService) {
        this.pathComputationService = pathComputationService;
    }

    @Override
    public ListenableFuture<RpcResult<PathComputationRerouteRequestOutput>> invoke(
                PathComputationRerouteRequestInput input) {
        LOG.info("RPC path computation reroute request received");
        LOG.debug("input parameters are : input = {}", input);
        try {
            return RpcResultBuilder
                    .success(this.pathComputationService.pathComputationRerouteRequest(input).get())
                    .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC path computation request failed !", e);
        }
        return RpcResultBuilder.<PathComputationRerouteRequestOutput>failed()
                .withError(ErrorType.RPC, "path-computation-reroute-request failed")
                .buildFuture();
    }

}
