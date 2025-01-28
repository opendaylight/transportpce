/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
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
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_FULL_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());

    public TapiOrLinkListener(final TapiLink tapiLink, final NetworkTransactionService networkTransactionService) {
        this.tapiLink = tapiLink;
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<Link>> changes) {
        LOG.info("onDataTreeChanged - {}", this.getClass().getSimpleName());
        for (DataTreeModification<Link> change : changes) {

            Link link = change.getRootNode().dataAfter();
            if (link == null) {
                continue;
            }
            LOG.info("New link in openroadm topology");
            // Todo: XPDR links are unidirectional, therefore we need to check for the current one and
            //  the opposite one. But first check the type
            Link1 link1 = link.augmentation(Link1.class);
            if (link1 == null) {
                LOG.error("No type in link. We cannot trigger the TAPI link creation");
                return;
            }

            if (!(link1.getLinkType().equals(OpenroadmLinkType.XPONDERINPUT)
                        || link1.getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT))
                        || link1.getLinkType().equals(OpenroadmLinkType.ROADMTOROADM)) {
                // No creation of link for ADD/DROP/EXPRESS links
                LOG.warn("Not triggering creation of link for type = {}", link1.getLinkType().getName());
                continue;
            }

            if (!oppositeLinkExists(link1.getOppositeLink())) {
                LOG.warn("Opposite link doest exist. Not creating TAPI link");
                return;
            }
            LOG.info("Opposite link already in datastore. Creating TAPI bidirectional link");

            String srcNode = getRoadmOrXpdr(link.getSource().getSourceNode().getValue());
            String srcTp = link.getSource().getSourceTp().getValue();
            String destNode = getRoadmOrXpdr(link.getDestination().getDestNode().getValue());
            String destTp = link.getDestination().getDestTp().getValue();
            //Configuring link type to default OMS_XPDR-RDM
            String linkType = TapiStringConstants.OMS_XPDR_RDM_LINK;

            if (link1.getLinkType().equals(OpenroadmLinkType.ROADMTOROADM)) {
                // For ROADM to ROADM link, only capture change on existing links to track change in OMS
                // Avoid creating 2 unidirectional links since these links are bidirectional in TAPI :
                // Links are created at initialization through a process that guarantees the creation of a unique link
                // Thus check that the link already exist in Datastore to upgrade it rather than creating an additional
                // unidirectional link
                if (!(linkExistInTopology(srcNode, srcTp, destNode, destTp, getQual(srcNode), getQual(destNode),
                        TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS))) {
                    continue;
                }
                LOG.warn("Now triggering creation of link for type = {} to account for OMS change",
                    link1.getLinkType().getName());
                linkType = TapiStringConstants.OMS_RDM_RDM_LINK;
            }

            // for Xpdr to roadm link, create Link only if it was not before in datastore since links are created
            // through rpcs and do not contain characteristics subject to potential updates
            if (change.getRootNode().dataBefore() != null) {
                continue;
            }
            putTapiLinkInTopology(this.tapiLink.createTapiLink(srcNode, srcTp, destNode, destTp,
                linkType, getQual(srcNode), getQual(destNode),
                TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS,
                link1.getAdministrativeState().getName(), link1.getOperationalState().getName(),
                Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                tapiTopoUuid));
        }
    }

    private boolean linkExistInTopology(String srcNodeId, String srcTpId, String dstNodeId, String dstTpId,
            String srcNodeQual, String dstNodeQual, String srcTpQual, String dstTpQual) {
        String sourceNepKey = String.join("+", srcNodeId, srcTpQual, srcTpId);
        String destNepKey = String.join("+", dstNodeId, dstTpQual, dstTpId);
        String linkKey = String.join("to", sourceNepKey, destNepKey);
        Uuid linkUuid = new Uuid(
            UUID.nameUUIDFromBytes(linkKey.getBytes(Charset.forName("UTF-8"))).toString());
        DataObjectIdentifier<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link>
                linkIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link.class,
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey(linkUuid))
            .build();
        try {
            Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> optLink =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, linkIID).get();
            if (optLink.isEmpty()) {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not read TAPI link in DataStore checking that rdm2rdm link is present");
        }
        return true;
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

    private String getQual(String node) {
        return node.contains("ROADM") ? TapiStringConstants.PHTNC_MEDIA : TapiStringConstants.XPDR;
    }

    private boolean oppositeLinkExists(LinkId oppositeLink) {
        try {
            if (this.networkTransactionService
                    .read(
                        LogicalDatastoreType.CONFIGURATION,
                        DataObjectIdentifier.builder(Networks.class)
                            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                            .augmentation(Network1.class)
                            .child(Link.class, new LinkKey(oppositeLink))
                            .build())
                    .get()
                    .isEmpty()) {
                LOG.error("Opposite link not found in datastore {}", oppositeLink.getValue());
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read opposite link", e);
            return false;
        }
    }

    private String getRoadmOrXpdr(String node) {
        return node.contains("ROADM")
            ? String.join("-", node.split("-")[0], node.split("-")[1])
            : node;
    }
}
