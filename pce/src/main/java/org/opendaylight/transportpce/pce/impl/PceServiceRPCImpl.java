/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.impl;

import java.util.concurrent.Future;

import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveOutput;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PceService;
/*
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.service.path.rpc.result.PathDescriptionBuilder;
///// check well PathDescriptionBuilder import
//---------------------------------------------
*/
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PceService implementation.
 */
public class PceServiceRPCImpl implements PceService {

    private static final Logger LOG = LoggerFactory.getLogger(PceServiceRPCImpl.class);

    private final PathComputationService pathComputationService;

    public PceServiceRPCImpl(PathComputationService pathComputationService) {
        this.pathComputationService = pathComputationService;
    }

    @Override
    public Future<RpcResult<CancelResourceReserveOutput>> cancelResourceReserve(CancelResourceReserveInput input) {
        LOG.info("RPC cancelResourceReserve request received");
        return RpcResultBuilder.success(pathComputationService.cancelResourceReserve(input)).buildFuture();
    }


    @Override
    public Future<RpcResult<PathComputationRequestOutput>> pathComputationRequest(PathComputationRequestInput input) {
        LOG.info("RPC path computation request received");
        return RpcResultBuilder.success(pathComputationService.pathComputationRequest(input)).buildFuture();
    }

}
