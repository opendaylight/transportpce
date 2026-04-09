/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.OpenRoadmLinkResolver;
import org.opendaylight.transportpce.tapi.topology.AbstractTapiNetworkUtil;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitRoadmRoadmTapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitRoadmRoadmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitRoadmRoadmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitRoadmRoadmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InitRoadmRoadmTapiLinkImpl extends AbstractTapiNetworkUtil implements InitRoadmRoadmTapiLink {

    private static final Logger LOG = LoggerFactory.getLogger(InitRoadmRoadmTapiLinkImpl.class);
    private TapiLink tapiLink;
    private final TopologyUtils topologyUtils;

    public InitRoadmRoadmTapiLinkImpl(
            TapiLink tapiLink,
            NetworkTransactionService networkTransactionService,
            TopologyUtils topologyUtils) {
        super(networkTransactionService);
        this.tapiLink = tapiLink;
        this.topologyUtils = topologyUtils;
    }

    @Override
    public ListenableFuture<RpcResult<InitRoadmRoadmTapiLinkOutput>> invoke(InitRoadmRoadmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String sourceNode = input.getRdmANode();
        String sourceTp = input.getDegATp();
        String destNode = input.getRdmZNode();
        String destTp = input.getDegZTp();

        Network network;
        try {
            network = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);
        } catch (TapiTopologyException e) {
            LOG.error(
                    "Failed to read topology '{}' from datastore."
                            + " Cannot create TAPI link from {}:{} to {}:{}. Aborting.",
                    InstanceIdentifiers.OPENROADM_TOPOLOGY_II,
                    sourceNode,
                    sourceTp,
                    destNode,
                    destTp,
                    e
            );
            return RpcResultBuilder.<InitRoadmRoadmTapiLinkOutput>failed()
                    .withError(ErrorType.RPC, "Failed to read topology from datastore; cannot create TAPI link")
                    .buildFuture();
        }

        Link link = this.tapiLink.createTapiLink(
                sourceNode,
                sourceTp,
                destNode,
                destTp,
                network,
                tapiTopoUuid,
                new OpenRoadmLinkResolver());
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.<InitRoadmRoadmTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to create link in topology")
                .buildFuture();
        }
        InitRoadmRoadmTapiLinkOutputBuilder output = new InitRoadmRoadmTapiLinkOutputBuilder();
        if (putLinkInTopology(link)) {
            output.setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }
}
