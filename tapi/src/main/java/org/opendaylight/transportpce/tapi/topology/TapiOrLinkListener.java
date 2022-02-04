/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiOrLinkListener implements DataTreeChangeListener<Link> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiOrLinkListener.class);
    private final TapiLink tapiLink;
    private final NetworkTransactionService networkTransactionService;
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
        .getBytes(Charset.forName("UTF-8"))).toString());

    public TapiOrLinkListener(final TapiLink tapiLink, final NetworkTransactionService networkTransactionService) {
        this.tapiLink = tapiLink;
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Link>> changes) {
        LOG.info("onDataTreeChanged - {}", this.getClass().getSimpleName());
        for (DataTreeModification<Link> change : changes) {
            if (change.getRootNode().getDataBefore() == null && change.getRootNode().getDataAfter() != null) {
                LOG.info("New link in openroadm topology");
                Link link = change.getRootNode().getDataAfter();
                // Todo: XPDR links are unidirectional, therefore we need to check for the current one and
                //  the opposite one. But first check the type
                Link1 link1 = link.augmentation(Link1.class);
                if (link1 == null) {
                    LOG.error("No type in link. We cannot trigger the TAPI link creation");
                    return;
                }
                if (!(link1.getLinkType().equals(OpenroadmLinkType.XPONDERINPUT)
                        || link1.getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT))) {
                    LOG.warn("Not triggering creation of link for type = {}", link1.getLinkType().getName());
                    return;
                }
                if (!oppositeLinkExists(link1.getOppositeLink())) {
                    LOG.warn("Opposite link doest exist. Not creating TAPI link");
                    return;
                }
                LOG.info("Opposite link already in datastore. Creatin TAPI bidirectional link");
                String srcNode = getRoadmOrXpdr(link.getSource().getSourceNode().getValue());
                String srcTp = link.getSource().getSourceTp().getValue();
                String destNode = getRoadmOrXpdr(link.getDestination().getDestNode().getValue());
                String destTp = link.getDestination().getDestTp().getValue();
                putTapiLinkInTopology(this.tapiLink.createTapiLink(srcNode, srcTp, destNode, destTp,
                    TapiStringConstants.OMS_XPDR_RDM_LINK, getQual(srcNode), getQual(destNode),
                    TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.PHTNC_MEDIA,
                    link1.getAdministrativeState().getName(), link1.getOperationalState().getName(),
                    List.of(LayerProtocolName.PHOTONICMEDIA), List.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                    tapiTopoUuid));
            }
        }
    }

    private void putTapiLinkInTopology(
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link tapiXpdrLink) {
        LOG.info("Creating tapi link in TAPI topology context");
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
            .build();

        Topology topology = new TopologyBuilder().setUuid(this.tapiTopoUuid)
            .setLink(Map.of(tapiXpdrLink.key(), tapiXpdrLink)).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topoIID,
            topology);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
        }
        LOG.info("TAPI Link added succesfully.");
    }

    private String getQual(String node) {
        if (node.contains("ROADM")) {
            return TapiStringConstants.PHTNC_MEDIA;
        }
        return TapiStringConstants.OTSI;
    }

    private boolean oppositeLinkExists(LinkId oppositeLink) {
        try {
            InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(oppositeLink)).build();

            Optional<Link> optLink =
                this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, linkIID).get();
            if (!optLink.isPresent()) {
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
        if (node.contains("ROADM")) {
            return String.join("-", node.split("-")[0], node.split("-")[1]);
        }
        return node;
    }
}
