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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.DeleteLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.DeleteLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.DeleteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.InitRoadmRoadmLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.InitRoadmRoadmLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.InitRoadmRoadmLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.InitXpdrRdmLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.InitXpdrRdmLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.InitXpdrRdmLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210204.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetworkUtilsImpl implements TransportpceTapinetworkutilsService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkUtilsImpl.class);
    private static final String PHTNC_MEDIA = "PHOTONIC_MEDIA";
    private static final String OTSI = "OTSi";
    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;

    public TapiNetworkUtilsImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.networkTransactionService = new NetworkTransactionImpl(new RequestProcessor(this.dataBroker));
    }

    @Override
    public ListenableFuture<RpcResult<InitRoadmRoadmLinkOutput>> initRoadmRoadmLink(InitRoadmRoadmLinkInput input) {
        String sourceNode = input.getRdmANode().split("-")[0];
        String sourceTp = input.getDegATp();
        String destNode = input.getRdmZNode().split("-")[0];
        String destTp = input.getDegZTp();
        String linkId = String.format(LINK_ID_FORMAT, input.getRdmANode(), sourceTp, input.getRdmZNode(), destTp);
        Link link = createTapiLink(sourceNode, sourceTp, destNode, destTp, linkId, "OMS link name", PHTNC_MEDIA);
        InitRoadmRoadmLinkOutputBuilder output = new InitRoadmRoadmLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.success(output.build()).buildFuture();
        }
        if (putLinkInTopology(link)) {
            output = new InitRoadmRoadmLinkOutputBuilder()
                    .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmLinkOutput>> initXpdrRdmLink(InitXpdrRdmLinkInput input) {
        String destNode = input.getRdmNode().split("-")[0];
        String destTp = input.getAddDropTp();
        String sourceNode = input.getXpdrNode();
        String sourceTp = input.getNetworkTp();
        String linkId = String.format(LINK_ID_FORMAT, input.getRdmNode(), sourceTp, destNode, destTp);
        Link link = createTapiLink(sourceNode, sourceTp, destNode, destTp, linkId, "XPDR-RDM link name", OTSI);
        InitXpdrRdmLinkOutputBuilder output = new InitXpdrRdmLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.success(output.build()).buildFuture();
        }
        if (putLinkInTopology(link)) {
            output = new InitXpdrRdmLinkOutputBuilder()
                    .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteLinkOutput>> deleteLink(DeleteLinkInput input) {
        // TODO: check if this IID is correct
        InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class).child(Topology.class,
                        new TopologyKey(tapiTopoUuid)).child(Link.class, new LinkKey(input.getUuid())).build();
        this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, linkIID);

        return RpcResultBuilder.success(new DeleteLinkOutputBuilder()
                .setResult("Link successfully deleted from tapi topology")).buildFuture();
    }

    private Link createTapiLink(String sourceNode, String sourceTp, String destNode, String destTp, String linkId,
                                String valueName, String sourceNodeQual) {
        Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
        Uuid sourceUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode,
                sourceNodeQual)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid sourceUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode, PHTNC_MEDIA,
                sourceTp)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode,
                PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode, PHTNC_MEDIA,
                destTp)).getBytes(Charset.forName("UTF-8"))).toString());
        NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(sourceUuidNode)
                .setNodeEdgePointUuid(sourceUuidTp)
                .build();
        nepList.put(sourceNep.key(), sourceNep);
        NodeEdgePoint destNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(destUuidNode)
                .setNodeEdgePointUuid(destUuidTp)
                .build();
        nepList.put(destNep.key(), destNep);
        Name linkName = new NameBuilder().setValueName(valueName)
                .setValue(linkId)
                .build();
        return new LinkBuilder()
                .setUuid(new Uuid(
                        UUID.nameUUIDFromBytes(linkId.getBytes(Charset.forName("UTF-8")))
                                .toString()))
                .setName(Map.of(linkName.key(), linkName))
                .setLayerProtocolName(List.of(LayerProtocolName.PHOTONICMEDIA))
                .setNodeEdgePoint(nepList)
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .build();
    }

    private boolean putLinkInTopology(Link tapiLink) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiTopoUuid))
                .build();

        Topology topology = new TopologyBuilder().setUuid(tapiTopoUuid)
                .setLink(Map.of(tapiLink.key(), tapiLink)).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, topoIID,
                topology);
        try {
            this.networkTransactionService.commit().get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
            return false;
        }
        LOG.info("Roadm Link added succesfully.");
        return true;
    }
}
