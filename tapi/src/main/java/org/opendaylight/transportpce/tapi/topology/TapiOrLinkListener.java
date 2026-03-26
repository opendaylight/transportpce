/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev250902.LinkClassEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiOrLinkListener implements DataTreeChangeListener<Link> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiOrLinkListener.class);
    private final TapiLink tapiLink;
    private final NetworkTransactionService networkTransactionService;
    private final Uuid tapiTopoUuid = new Uuid(
            UUID.nameUUIDFromBytes(TapiConstants.T0_FULL_MULTILAYER.getBytes(StandardCharsets.UTF_8)).toString());

    public TapiOrLinkListener(final TapiLink tapiLink, final NetworkTransactionService networkTransactionService) {
        this.tapiLink = tapiLink;
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<Link>> changes) {
        LOG.info("onDataTreeChanged - {} changes - {}", changes.size(), this.getClass().getSimpleName());
        for (DataTreeModification<Link> change : changes) {

            Link link = change.getRootNode().dataAfter();
            if (link == null) {
                continue;
            }
            LOG.info("New link in openroadm topology: {}", link.getLinkId().getValue());

            Link1 link1 = link.augmentation(Link1.class);
            if (link1 == null) {
                LOG.error("No type in link. We cannot trigger the TAPI link creation");
                return;
            }
            var tpceAugmLink1 = link.augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.or.network.augmentation.rev250902.Link1.class);
            if (tpceAugmLink1 != null && tpceAugmLink1.getLinkClass() != null
                    && tpceAugmLink1.getLinkClass().equals(LinkClassEnum.InterDomain)) {
                LOG.info("{} post InterdomainLink {} in TAPI topology Datastores",
                    addInterdomainLinkToTapiTopologies(link) ? "Successfully" : "Did not succeed to", link.getLinkId());
                continue;
            }

            if (!(link1.getLinkType().equals(OpenroadmLinkType.XPONDERINPUT)
                        || link1.getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT)
                        || link1.getLinkType().equals(OpenroadmLinkType.ROADMTOROADM))) {
                // No creation of link for ADD/DROP/EXPRESS links
                LOG.debug("TapiORLinkListener Line 82 Not triggering creation of link for type = {}, RtoR = {}",
                    link1.getLinkType().getName(), link1.getLinkType().equals(OpenroadmLinkType.ROADMTOROADM));
                continue;
            }
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.Link1 link11 = link
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.Link1.class);
            if (link1.getLinkType().equals(OpenroadmLinkType.ROADMTOROADM) && link11 != null
                && link11.getOMSAttributes() != null) {
                LOG.debug("TapiORLinkListener line 116 for link {} found an OMS attributes ", link.getLinkId());
            }

            String linkType = null;
            switch (link1.getLinkType()) {
                case ROADMTOROADM:
                    linkType = TapiConstants.OMS_RDM_RDM_LINK;
                    break;
                case XPONDERINPUT:
                    linkType = TapiConstants.OTS_RDM_XPDR_LINK;
                    break;
                case XPONDEROUTPUT:
                    linkType = TapiConstants.OTS_XPDR_RDM_LINK;
                    break;
                case OTNLINK:
                    return;
                default:
                    LOG.warn("Type not recognized for link {}", link.getLinkId());
                    return;
            }
            String srcNode = getRoadmOrXpdr(link.getSource().getSourceNode().getValue());
            String srcTp = link.getSource().getSourceTp().getValue();
            String destNode = getRoadmOrXpdr(link.getDestination().getDestNode().getValue());
            String destTp = link.getDestination().getDestTp().getValue();
            String opplinkId = link1.getOppositeLink().getValue();

            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link tapiLink1 =
                this.tapiLink.createTapiLink(srcNode, srcTp, destNode, destTp,
                        linkType, getQual(srcNode), getQual(destNode),
                        TapiConstants.PHTNC_MEDIA_OTS, TapiConstants.PHTNC_MEDIA_OTS,
                        link1.getAdministrativeState().getName(), link1.getOperationalState().getName(),
                        Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                        tapiTopoUuid, opplinkId);
            logNewTapiLink(tapiLink1);
            putTapiLinkInTopology(tapiLink1);
        }
    }

    /**
     * Logs the creation of a new TAPI topology link, including its resolved name (or a default if unnamed).
     * Emits the full link object at DEBUG level.
     *
     * @param tapiLink1 newly created TAPI link
     */
    private void logNewTapiLink(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link
            tapiLink1) {

        Set<String> names = Optional.ofNullable(tapiLink1.getName())
                .stream()
                .flatMap(m -> m.values().stream())
                .map(Name::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (names.isEmpty()) {
            names = Set.of("<unnamed>");
        }

        LOG.info("TAPI link {} created.", String.join(", ", names));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Link: {}", tapiLink1);
        }
    }

    private boolean addInterdomainLinkToTapiTopologies(Link link) {
        String tapiSBIend = "A";
        TpId srcTpId = link.getSource().getSourceTp();
        TpId dstTpId = link.getDestination().getDestTp();
        NodeId srcNode = link.getSource().getSourceNode();
        NodeId dstNode = link.getDestination().getDestNode();
        String aendName = String.join("+", srcNode.getValue(), srcTpId.getValue());
        String zendName = String.join("+", dstNode.getValue(), dstTpId.getValue());

        if (dstNode.getValue().equals("TAPI-SBI-ABS-NODE")) {
            tapiSBIend = "Z";
            zendName = dstTpId.getValue();
        } else {
            aendName = srcTpId.getValue();
        }
        DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation
                .rev250902.TerminationPoint1> tpIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
            .child(Node.class, new NodeKey(tapiSBIend.equals("A") ? srcNode : dstNode))
            .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(
                new TpId(tapiSBIend.equals("A") ? srcTpId : dstTpId)))
            .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.or.network.augmentation.rev250902.TerminationPoint1.class)
            .build();
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev250902
            .TerminationPoint1 tapiTp = null;
        try {
            Optional<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev250902
                .TerminationPoint1> optTp = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get();
            if (optTp.isPresent()) {
                tapiTp = optTp.orElseThrow();
                LOG.debug("TapiORLinListener optTp.isPresent = true {}", tapiTp);
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("TapiORLinListener: Error retrieving Tp {} from InterdomainLink {}", tpIID, link.getLinkId(), e);
            return false;
        }
        Uuid srcTpUuid;
        Uuid srcNodeUuid;
        Uuid dstTpUuid;
        Uuid dstNodeUuid;
        Uuid srcTopoUuid;
        Uuid dstTopoUuid;
        if (tapiSBIend.equals("Z")) {
            srcTpUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+",
                    srcNode.getValue().split("-DEG")[0], TapiConstants.PHTNC_MEDIA_OTS, srcTpId.getValue())
                .getBytes(StandardCharsets.UTF_8)).toString());
            srcNodeUuid = new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", srcNode.getValue().split("-DEG")[0], TapiConstants.PHTNC_MEDIA)
                .getBytes(StandardCharsets.UTF_8)).toString());
            // Source TopoUuid depends on the type of translation used for OR Topo to tapi (ML vs Full ML)
            srcTopoUuid = this.tapiTopoUuid;
            dstTopoUuid = new Uuid(StringConstants.SBI_TAPI_TOPOLOGY_UUID);
            dstTpUuid = new Uuid(tapiTp.getTpUuid());
            dstNodeUuid = new Uuid(tapiTp.getSupportingNodeUuid());
        } else {
            dstTpUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+",
                    dstNode.getValue().split("-DEG")[0], TapiConstants.PHTNC_MEDIA_OTS, dstTpId.getValue())
                .getBytes(StandardCharsets.UTF_8)).toString());
            dstNodeUuid = new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", dstNode.getValue().split("-DEG")[0], TapiConstants.PHTNC_MEDIA)
                .getBytes(StandardCharsets.UTF_8)).toString());
            // Dest TopoUuid depends on the type of translation used for OR Topo to tapi (ML vs Full ML)
            dstTopoUuid = this.tapiTopoUuid;
            srcTopoUuid = new Uuid(StringConstants.SBI_TAPI_TOPOLOGY_UUID);
            srcTpUuid = new Uuid(tapiTp.getTpUuid());
            srcNodeUuid = new Uuid(tapiTp.getSupportingNodeUuid());
        }
        return
                putTapiInterDomainLinkInTopology(StringConstants.SBI_TAPI_TOPOLOGY_UUID,
                    this.tapiLink.createInterDomainTapiLink(link.getLinkId(), String.join("to", aendName, zendName),
                        srcNodeUuid, srcTpUuid, dstNodeUuid, dstTpUuid, srcTopoUuid, dstTopoUuid))
                &&
                putTapiInterDomainLinkInTopology(StringConstants.T0_FULL_MULTILAYER_UUID,
                    this.tapiLink.createInterDomainTapiLink(link.getLinkId(), String.join("to", aendName, zendName),
                        srcNodeUuid, srcTpUuid, dstNodeUuid, dstTpUuid, srcTopoUuid, dstTopoUuid));
    }

    private void putTapiLinkInTopology(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link tapiXpdrLink) {
        LOG.info("Creating tapi link in TAPI topology context");
        // merge in datastore
        this.networkTransactionService.merge(
            LogicalDatastoreType.OPERATIONAL,
            DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                .build(),
            new TopologyBuilder()
                .setUuid(this.tapiTopoUuid)
                .setLink(Map.of(tapiXpdrLink.key(), tapiXpdrLink))
                .build());
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
        }
        LOG.info("TAPI Link added succesfully.");
    }

    private boolean putTapiInterDomainLinkInTopology(Uuid topoUuid,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link) {
        LOG.info("Creating tapi link {} in TAPI topology context", link.getName());
        // merge in datastore
        this.networkTransactionService.merge(
            LogicalDatastoreType.OPERATIONAL,
            DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topoUuid))
                .build(),
            new TopologyBuilder()
                .setUuid(topoUuid)
                .setLink(Map.of(link.key(), link))
                .build());
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology with InterdomainLink {}: ", link.getName(), e);
            return false;
        }
        LOG.info("TAPI InterdomainLink {} added succesfully in Topology {}.", link.getName(), topoUuid);
        return true;
    }

    private String getQual(String node) {
        return node.contains("ROADM") ? TapiConstants.PHTNC_MEDIA : TapiConstants.XPDR;
    }

    private String getRoadmOrXpdr(String node) {
        return node.contains("ROADM")
            ? node.substring(0, node.lastIndexOf("-"))
            : node;
    }
}
