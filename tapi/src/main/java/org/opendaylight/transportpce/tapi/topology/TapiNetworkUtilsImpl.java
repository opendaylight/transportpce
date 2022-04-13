/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.DeleteTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.DeleteTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.DeleteTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitRoadmRoadmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitRoadmRoadmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitRoadmRoadmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitXpdrRdmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitXpdrRdmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitXpdrRdmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetworkUtilsImpl implements TransportpceTapinetworkutilsService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkUtilsImpl.class);
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
    private final NetworkTransactionService networkTransactionService;
    private final TapiLink tapiLink;

    public TapiNetworkUtilsImpl(NetworkTransactionService networkTransactionService, TapiLink tapiLink) {
        this.networkTransactionService = networkTransactionService;
        this.tapiLink = tapiLink;
    }

    @Override
    public ListenableFuture<RpcResult<InitRoadmRoadmTapiLinkOutput>> initRoadmRoadmTapiLink(
            InitRoadmRoadmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String sourceNode = input.getRdmANode();
        String sourceTp = input.getDegATp();
        String destNode = input.getRdmZNode();
        String destTp = input.getDegZTp();
        Link link = this.tapiLink.createTapiLink(sourceNode, sourceTp, destNode, destTp,
            TapiStringConstants.OMS_RDM_RDM_LINK, TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.PHTNC_MEDIA,
            TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.PHTNC_MEDIA,
            this.tapiLink.getAdminState(sourceNode, sourceTp, destNode, destTp),
            this.tapiLink.getOperState(sourceNode, sourceTp, destNode, destTp),
            Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()), tapiTopoUuid);
        InitRoadmRoadmTapiLinkOutputBuilder output = new InitRoadmRoadmTapiLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.<InitRoadmRoadmTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to create link in topology")
                .buildFuture();
        }
        if (putLinkInTopology(link)) {
            output = new InitRoadmRoadmTapiLinkOutputBuilder()
                .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmTapiLinkOutput>> initXpdrRdmTapiLink(InitXpdrRdmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String destNode = input.getRdmNode();
        String destTp = input.getAddDropTp();
        String sourceNode = input.getXpdrNode();
        String sourceTp = input.getNetworkTp();
        Link link = this.tapiLink.createTapiLink(sourceNode, sourceTp, destNode, destTp,
            TapiStringConstants.OMS_XPDR_RDM_LINK, TapiStringConstants.OTSI, TapiStringConstants.PHTNC_MEDIA,
            TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.PHTNC_MEDIA,
            this.tapiLink.getAdminState(sourceNode, sourceTp, destNode, destTp),
            this.tapiLink.getOperState(sourceNode, sourceTp, destNode, destTp),
            Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()), tapiTopoUuid);
        InitXpdrRdmTapiLinkOutputBuilder output = new InitXpdrRdmTapiLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.<InitXpdrRdmTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to create link in topology")
                .buildFuture();
        }
        if (putLinkInTopology(link)) {
            output = new InitXpdrRdmTapiLinkOutputBuilder()
                .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteTapiLinkOutput>> deleteTapiLink(DeleteTapiLinkInput input) {
        // TODO: check if this IID is correct
        // TODO --> need to check if the link exists in the topology
        try {
            InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
                    .augmentation(Context1.class).child(TopologyContext.class).child(Topology.class,
                            new TopologyKey(tapiTopoUuid)).child(Link.class, new LinkKey(input.getUuid())).build();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, linkIID);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI link deleted successfully.");
            return RpcResultBuilder.success(new DeleteTapiLinkOutputBuilder()
                .setResult("Link successfully deleted from tapi topology").build()).buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI link", e);
            return RpcResultBuilder.<DeleteTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to delete link from topology")
                .buildFuture();
        }
    }

    private boolean putLinkInTopology(Link tapLink) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid))
            .build();

        Topology topology = new TopologyBuilder().setUuid(tapiTopoUuid)
            .setLink(Map.of(tapLink.key(), tapLink)).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topoIID,
            topology);
        try {
            this.networkTransactionService.commit().get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
            return false;
        }
        LOG.info("TAPI Link added succesfully.");
        return true;
    }
}
