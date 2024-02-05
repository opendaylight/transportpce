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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserve;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveOutput;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CancelResourceReserveImpl implements CancelResourceReserve {
    private static final Logger LOG = LoggerFactory.getLogger(CancelResourceReserveImpl.class);
    private PathComputationService pathComputationService;

    public CancelResourceReserveImpl(final PathComputationService pathComputationService) {
        this.pathComputationService = pathComputationService;
    }

    @Override
    public ListenableFuture<RpcResult<CancelResourceReserveOutput>> invoke(CancelResourceReserveInput input) {
        LOG.info("RPC cancelResourceReserve request received");
        try {
            return RpcResultBuilder
                    .success(this.pathComputationService.cancelResourceReserve(input).get())
                    .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC cancelResourceReserve failed !", e);
        }
        return RpcResultBuilder.<CancelResourceReserveOutput>failed()
                .withError(ErrorType.RPC, "cancel-resource-reserve failed")
                .buildFuture();
    }

}
