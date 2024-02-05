/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetTopologyListImpl implements GetTopologyList {
    private static final Logger LOG = LoggerFactory.getLogger(GetTopologyListImpl.class);
    private final TapiContext tapiContext;

    public GetTopologyListImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetTopologyListOutput>> invoke(GetTopologyListInput input) {
        // TODO Auto-generated method stub
        // TODO -> maybe we get errors when having CEPs?
        Map<TopologyKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology>
                topologyMap = this.tapiContext.getTopologyContext();
        if (topologyMap.isEmpty()) {
            LOG.error("No topologies exist in tapi context");
            return RpcResultBuilder.<GetTopologyListOutput>failed()
                .withError(ErrorType.APPLICATION, "No topologies exist in tapi context")
                .buildFuture();
        }
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.topology.list.output.TopologyKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.topology.list.output.Topology>
                newTopoMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology
                topo:topologyMap.values()) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.topology.list.output.Topology
                newTopo = new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.topology.rev221121.get.topology.list.output.TopologyBuilder(topo).build();
            newTopoMap.put(newTopo.key(), newTopo);
        }
        return RpcResultBuilder
                .success(new GetTopologyListOutputBuilder().setTopology(newTopoMap).build())
                .buildFuture();
    }

}
