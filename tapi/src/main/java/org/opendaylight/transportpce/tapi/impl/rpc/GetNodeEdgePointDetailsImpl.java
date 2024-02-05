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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.node.edge.point.details.output.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNodeEdgePointDetailsImpl implements GetNodeEdgePointDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetNodeEdgePointDetailsImpl.class);
    private final TapiContext tapiContext;

    public GetNodeEdgePointDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeEdgePointDetailsOutput>> invoke(GetNodeEdgePointDetailsInput input) {
        // TODO Auto-generated method stub
        // TODO -> maybe we get errors when having CEPs?
        Uuid topoUuid = input.getTopologyId();
        // Node id: if roadm -> ROADMid+PHOTONIC_MEDIA. if xpdr -> XPDRid-XPDRnbr+DSR/OTSi
        Uuid nodeUuid = input.getNodeId();
        // NEP id: if roadm -> ROADMid+PHOTONIC_MEDIA/MC/OTSiMC+TPid.
        // if xpdr -> XPDRid-XPDRnbr+DSR/eODU/iODU/iOTSi/eOTSi/PHOTONIC_MEDIA+TPid
        Uuid nepUuid = input.getNodeEdgePointId();
        OwnedNodeEdgePoint nep = this.tapiContext.getTapiNEP(topoUuid, nodeUuid, nepUuid);
        if (nep == null) {
            LOG.error("Invalid TAPI nep name");
            return RpcResultBuilder.<GetNodeEdgePointDetailsOutput>failed()
                .withError(ErrorType.RPC, "Invalid NEP name")
                .buildFuture();
        }
        return RpcResultBuilder
                .success(new GetNodeEdgePointDetailsOutputBuilder()
                        .setNodeEdgePoint(new NodeEdgePointBuilder(nep).build()).build())
                .buildFuture();
    }

}
