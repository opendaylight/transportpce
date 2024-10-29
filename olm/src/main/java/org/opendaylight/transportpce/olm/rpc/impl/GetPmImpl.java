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

    public GetPmImpl(final OlmPowerService olmPowerService) {
        this.olmPowerService = requireNonNull(olmPowerService);
    }

    @Override
    public ListenableFuture<RpcResult<GetPmOutput>> invoke(final GetPmInput input) {
        if (input.getNodeId() == null) {
            LOG.error("getPm: NodeId can not be null");
            return RpcResultBuilder.<GetPmOutput>failed()
                    .withError(ErrorType.RPC, "Error with input parameters")
                    .buildFuture();
        }
        return RpcResultBuilder.success(this.olmPowerService.getPm(input)).buildFuture();
    }
}
