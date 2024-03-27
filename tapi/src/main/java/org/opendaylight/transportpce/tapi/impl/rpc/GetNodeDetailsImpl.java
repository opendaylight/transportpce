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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.node.details.output.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNodeDetailsImpl implements GetNodeDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetNodeDetailsImpl.class);
    private final TapiContext tapiContext;

    public GetNodeDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeDetailsOutput>> invoke(GetNodeDetailsInput input) {
        // TODO Auto-generated method stub
        // TODO -> maybe we get errors when having CEPs?
        // Node id: if roadm -> ROADM+PHOTONIC_MEDIA. if xpdr -> XPDR-XPDR+DSR/OTSi
        Node node = this.tapiContext.getTapiNode(input.getTopologyId(), input.getNodeId());
        if (node == null) {
            LOG.error("Invalid TAPI node name");
            return RpcResultBuilder.<GetNodeDetailsOutput>failed()
                .withError(ErrorType.RPC, "Invalid Tapi Node name")
                .buildFuture();
        }
        return RpcResultBuilder
            .success(
                new GetNodeDetailsOutputBuilder()
                    .setNode(new NodeBuilder(node).build())
                    .build())
            .buildFuture();
    }

}
