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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InitXpdrRdmTapiLinkImpl extends AbstractTapiNetworkUtil implements InitXpdrRdmTapiLink {

    private static final Logger LOG = LoggerFactory.getLogger(InitXpdrRdmTapiLinkImpl.class);
    private TapiLink tapiLink;
    private final TopologyUtils topologyUtils;

    public InitXpdrRdmTapiLinkImpl(
            TapiLink tapiLink,
            NetworkTransactionService networkTransactionService,
            TopologyUtils topologyUtils) {
        super(networkTransactionService);
        this.tapiLink = tapiLink;
        this.topologyUtils = topologyUtils;
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmTapiLinkOutput>> invoke(InitXpdrRdmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String destNode = input.getRdmNode();
        String destTp = input.getAddDropTp();
        String sourceNode = input.getXpdrNode();
        String sourceTp = input.getNetworkTp();

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
            return RpcResultBuilder.<InitXpdrRdmTapiLinkOutput>failed()
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
            return RpcResultBuilder.<InitXpdrRdmTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to create link in topology")
                .buildFuture();
        }
        InitXpdrRdmTapiLinkOutputBuilder output = new InitXpdrRdmTapiLinkOutputBuilder();
        if (putLinkInTopology(link)) {
            output.setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

}
