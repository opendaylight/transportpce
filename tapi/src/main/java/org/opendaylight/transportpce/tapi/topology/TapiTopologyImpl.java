/*
 * Copyright © 2019 Orange & 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.link.details.output.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.node.details.output.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.node.edge.point.details.output.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopologyImpl implements TapiTopologyService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImpl.class);
    private final TapiContext tapiContext;
    private final TopologyUtils topologyUtils;

    public TapiTopologyImpl(TapiContext tapiContext, TopologyUtils topologyUtils) {
        this.tapiContext = tapiContext;
        this.topologyUtils = topologyUtils;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeDetailsOutput>> getNodeDetails(GetNodeDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // Node id: if roadm -> ROADM+PHOTONIC_MEDIA. if xpdr -> XPDR-XPDR+DSR/OTSi
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(input.getNodeIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        Node node = this.tapiContext.getTapiNode(topoUuid, nodeUuid);
        if (node == null) {
            LOG.error("Invalid TAPI node name");
            return RpcResultBuilder.success(new GetNodeDetailsOutputBuilder().build()).buildFuture();
        }
        return RpcResultBuilder.success(new GetNodeDetailsOutputBuilder()
                .setNode(new NodeBuilder(node).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyDetailsOutput>> getTopologyDetails(GetTopologyDetailsInput input) {
        // TODO: get Topology from TAPI context
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        Context context = this.tapiContext.getTapiContext();
        Map<TopologyKey, Topology> topologyMap = context.augmentation(Context1.class).getTopologyContext()
                .getTopology();
        if (!topologyMap.containsKey(new TopologyKey(topoUuid))) {
            LOG.error("Invalid TAPI topology name");
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().build()).buildFuture();
        }
        Topology topology = topologyMap.get(new TopologyKey(topoUuid));
        // TODO: abstract roadm infrastructure

        return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                .setTopology(new TopologyBuilder(topology).build())
                .build())
                .buildFuture();
        /*
        if (!TopologyUtils.T0_MULTILAYER.equals(input.getTopologyIdOrName())
             && !TopologyUtils.TPDR_100G.equals(input.getTopologyIdOrName())) {
            LOG.error("Invalid TAPI topology name");
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().build()).buildFuture();
        }
        try {
            LOG.info("Building TAPI Topology abstraction for {}", input.getTopologyIdOrName());
            Topology topology = createAbstractedOtnTopology();
            if (TopologyUtils.TPDR_100G.equals(input.getTopologyIdOrName())) {
                topology = createAbstracted100GTpdrTopology(topology);
            }
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().setTopology(topology).build())
                .buildFuture();
        } catch (TapiTopologyException e) {
            LOG.error("error building TAPI topology");
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().build()).buildFuture();
        }
        */
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeEdgePointDetailsOutput>> getNodeEdgePointDetails(
        GetNodeEdgePointDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // Node id: if roadm -> ROADMid+PHOTONIC_MEDIA. if xpdr -> XPDRid-XPDRnbr+DSR/OTSi
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(input.getNodeIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // NEP id: if roadm -> ROADMid+PHOTONIC_MEDIA/MC/OTSiMC+TPid.
        // if xpdr -> XPDRid-XPDRnbr+DSR/eODU/iODU/iOTSi/eOTSi/PHOTONIC_MEDIA+TPid
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(input.getEpIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint nep = this.tapiContext.getTapiNEP(topoUuid, nodeUuid, nepUuid);
        if (nep == null) {
            LOG.error("Invalid TAPI nep name");
            return RpcResultBuilder.success(new GetNodeEdgePointDetailsOutputBuilder().build()).buildFuture();
        }
        return RpcResultBuilder.success(new GetNodeEdgePointDetailsOutputBuilder()
                .setNodeEdgePoint(new NodeEdgePointBuilder(nep).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetLinkDetailsOutput>> getLinkDetails(GetLinkDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        // Link id: same as OR link id
        Uuid linkUuid = new Uuid(UUID.nameUUIDFromBytes(input.getLinkIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        Link link = this.tapiContext.getTapiLink(topoUuid, linkUuid);
        if (link == null) {
            LOG.error("Invalid TAPI link name");
            return RpcResultBuilder.success(new GetLinkDetailsOutputBuilder().build()).buildFuture();
        }
        return RpcResultBuilder.success(new GetLinkDetailsOutputBuilder().setLink(new LinkBuilder(link).build())
                .build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyListOutput>> getTopologyList(GetTopologyListInput input) {
        // TODO Auto-generated method stub
        Map<TopologyKey, Topology> topologyMap = this.tapiContext.getTopologyContext();
        if (topologyMap.isEmpty()) {
            LOG.error("No topologies exist in tapi context");
            return RpcResultBuilder.success(new GetTopologyListOutputBuilder().build()).buildFuture();
        }
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.TopologyKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.Topology>
                newTopoMap = new HashMap<>();
        for (Topology topo:topologyMap.values()) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.Topology
                newTopo = new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.topology.rev181210.get.topology.list.output.TopologyBuilder(topo).build();
            newTopoMap.put(newTopo.key(), newTopo);
        }
        return RpcResultBuilder.success(new GetTopologyListOutputBuilder().setTopology(newTopoMap).build())
                .buildFuture();
    }
}
