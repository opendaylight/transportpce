/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.link.details.output.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetLinkDetailsImpl implements GetLinkDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetLinkDetailsImpl.class);
    private final TapiContext tapiContext;

    public GetLinkDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetLinkDetailsOutput>> invoke(GetLinkDetailsInput input) {
        // TODO Auto-generated method stub
        // Link id: same as OR link id
        Link link = this.tapiContext.getTapiLink(input.getTopologyId(), input.getLinkId());
        if (link == null) {
            LOG.error("Invalid TAPI link name");
            return RpcResultBuilder.<GetLinkDetailsOutput>failed()
                .withError(ErrorType.RPC, "Invalid Link name")
                .buildFuture();
        }
        LOG.info("debug link is : {}", link.getName());
        return RpcResultBuilder
                .success(new GetLinkDetailsOutputBuilder().setLink(new LinkBuilder(link).build()).build())
                .buildFuture();
    }

}
