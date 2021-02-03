/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
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
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyDetailsOutput>> getTopologyDetails(GetTopologyDetailsInput input) {
        // TODO: get Topology from TAPI context
        LOG.info("Input={}", input.toString());
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName().getBytes(Charset.forName("UTF-8")))
                .toString());
        Context context = this.tapiContext.getTapiContext();
        Map<TopologyKey, Topology> topologyMap = context.augmentation(Context1.class).getTopologyContext()
                .getTopology();
        if (!(topologyMap != null && topologyMap.containsKey(new TopologyKey(topoUuid)))) {
            LOG.error("Invalid TAPI topology name");
            return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder().build()).buildFuture();
        }
        Topology topology = topologyMap.get(new TopologyKey(topoUuid));
        // TODO: abstract roadm infrastructure

        return RpcResultBuilder.success(new GetTopologyDetailsOutputBuilder()
                .setTopology(this.topologyUtils.transformTopology(topology))
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
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetLinkDetailsOutput>> getLinkDetails(GetLinkDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyListOutput>> getTopologyList(GetTopologyListInput input) {
        // TODO Auto-generated method stub
        return null;
    }
}
