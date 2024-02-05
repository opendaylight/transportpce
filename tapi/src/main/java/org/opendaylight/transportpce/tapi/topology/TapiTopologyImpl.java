/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.impl.rpc.GetLinkDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeEdgePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointListImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyListImpl;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyList;
import org.opendaylight.yangtools.yang.binding.Rpc;

public class TapiTopologyImpl {

    private final NetworkTransactionService networkTransactionService;
    private final TapiContext tapiContext;
    private final TopologyUtils topologyUtils;
    private final TapiLink tapiLink;

    public TapiTopologyImpl(NetworkTransactionService networkTransactionService, TapiContext tapiContext,
            TopologyUtils topologyUtils, TapiLink tapiLink) {
        this.networkTransactionService = networkTransactionService;
        this.tapiContext = tapiContext;
        this.topologyUtils = topologyUtils;
        this.tapiLink = tapiLink;

    }

    public ImmutableClassToInstanceMap<Rpc<?, ?>> registerRPCs() {
        return ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(GetNodeDetails.class, new GetNodeDetailsImpl(tapiContext))
            .put(GetTopologyDetails.class, new GetTopologyDetailsImpl(tapiContext, topologyUtils, tapiLink,
                    networkTransactionService))
            .put(GetNodeEdgePointDetails.class, new GetNodeEdgePointDetailsImpl(tapiContext))
            .put(GetLinkDetails.class, new GetLinkDetailsImpl(tapiContext))
            .put(GetTopologyList.class, new GetTopologyListImpl(tapiContext))
            .put(GetServiceInterfacePointDetails.class, new GetServiceInterfacePointDetailsImpl(tapiContext))
            .put(GetServiceInterfacePointList.class, new GetServiceInterfacePointListImpl(tapiContext))
            .build();
    }

}
