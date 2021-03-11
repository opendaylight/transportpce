/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyUtils.class);

    private TopologyUtils() {
    }

    // This method returns the linkBuilder object for given source and destination
    public static LinkBuilder createLink(String srcNode, String dstNode, String srcTp, String destTp,
        String otnPrefix) {

        // Create Destination for link
        DestinationBuilder dstNodeBldr = new DestinationBuilder()
            .setDestTp(destTp)
            .setDestNode(new NodeId(dstNode));

        // Create Source for the link
        SourceBuilder srcNodeBldr = new SourceBuilder()
            .setSourceNode(new NodeId(srcNode))
            .setSourceTp(srcTp);

        LinkId linkId;
        LinkId oppositeLinkId;
        if (otnPrefix == null) {
            linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
            oppositeLinkId = LinkIdUtil.buildLinkId(dstNode, destTp, srcNode, srcTp);
        } else {
            linkId = LinkIdUtil.buildOtnLinkId(srcNode, srcTp, dstNode, destTp, otnPrefix);
            oppositeLinkId = LinkIdUtil.buildOtnLinkId(dstNode, destTp, srcNode, srcTp, otnPrefix);
        }
        //set opposite link
        Link1 lnk1 = new Link1Builder().setOppositeLink(oppositeLinkId).build();

        // set link builder attribute
        LinkBuilder lnkBldr = new LinkBuilder()
            .setDestination(dstNodeBldr.build())
            .setSource(srcNodeBldr.build())
            .setLinkId(linkId)
            .withKey(new LinkKey(linkId))
            .addAugmentation(lnk1);
        return lnkBldr;
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLink(String srcNode, String dstNode, String srcTp, String destTp,
                                     NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for {}-{}", srcNode, dstNode);
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
        if (deleteLinkLinkId(linkId, networkTransactionService)) {
            LOG.debug("Link Id {} updated to have admin state down", linkId);
            return true;
        } else {
            LOG.debug("Link Id not found for Source {} and Dest {}", srcNode, dstNode);
            return false;
        }
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLinkLinkId(LinkId linkId , NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for LinkId: {}", linkId.getValue());
        try {
            InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));
            java.util.Optional<Link> link =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.get());
                Link1Builder link1Builder = new Link1Builder(linkBuilder.augmentation(Link1.class));
                linkBuilder.removeAugmentation(Link1.class);
                linkBuilder.addAugmentation(link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                    linkBuilder.build());
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
                return true;
            } else {
                LOG.error("No link found for given LinkId: {}",
                    linkId);
                return false;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error deleting link {}", linkId.getValue(), e);
            return false;
        }
    }

    public static AdminStates setNetworkAdminState(String adminState) {
        if (adminState == null) {
            return null;
        }
        switch (adminState) {
            case "InService":
                return AdminStates.InService;
            case "OutOfService":
                return AdminStates.OutOfService;
            case "Maintenance":
                return AdminStates.Maintenance;
            default:
                return null;
        }
    }

    public static State setNetworkOperState(String operState) {
        if (operState == null) {
            return null;
        }
        switch (operState) {
            case "InService":
                return State.InService;
            case "OutOfService":
                return State.OutOfService;
            case "Degraded":
                return State.Degraded;
            default:
                return null;
        }
    }
}
