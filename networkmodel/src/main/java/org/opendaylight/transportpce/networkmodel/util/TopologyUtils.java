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

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
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

public  final class TopologyUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyUtils.class);

    private TopologyUtils() {
    }

    // This method returns the linkBuilder object for given source and destination
    public static LinkBuilder createLink(String srcNode, String dstNode, String srcTp, String destTp) {

        // Create Destination for link
        DestinationBuilder dstNodeBldr = new DestinationBuilder();
        dstNodeBldr.setDestTp(destTp);
        dstNodeBldr.setDestNode(new NodeId(dstNode));

        // Create Source for the link
        SourceBuilder srcNodeBldr = new SourceBuilder();
        srcNodeBldr.setSourceNode(new NodeId(srcNode));
        srcNodeBldr.setSourceTp(srcTp);

        // set link builder attribute
        LinkBuilder lnkBldr = new LinkBuilder();

        lnkBldr.setDestination(dstNodeBldr.build());
        lnkBldr.setSource(srcNodeBldr.build());
        lnkBldr.setLinkId(LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp));
        lnkBldr.withKey(new LinkKey(lnkBldr.getLinkId()));

        //set opposite link
        Link1Builder lnk1Bldr = new Link1Builder();
        LinkId oppositeLinkId = LinkIdUtil.getOppositeLinkId(srcNode, srcTp, dstNode, destTp);
        lnk1Bldr.setOppositeLink(oppositeLinkId);
        lnkBldr.addAugmentation(Link1.class,lnk1Bldr.build());
        return lnkBldr;
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLink(String srcNode, String dstNode, String srcTp, String destTp,
                                     NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for {}-{}", srcNode, dstNode);
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
        if (deleteLinkLinkId(linkId, networkTransactionService)) {
            LOG.debug("Link Id {} updated to have admin state down");
            return true;
        } else {
            LOG.debug("Link Id not found for Source {} and Dest {}", srcNode, dstNode);
            return false;
        }
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLinkLinkId(LinkId linkId , NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for LinkId: {}", linkId);
        try {
            InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));
            com.google.common.base.Optional<Link> link =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.get());
                Link1Builder link1Builder = new Link1Builder(linkBuilder.augmentation(Link1.class));
//                link1Builder.setAdministrativeState(State.OutOfService);
                linkBuilder.removeAugmentation(Link1.class);
                linkBuilder.addAugmentation(Link1.class,link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                    linkBuilder.build());
                networkTransactionService.submit().get(1, TimeUnit.SECONDS);
                return true;
            } else {
                LOG.error("No link found for given LinkId: {}",
                    linkId);
                return false;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }
}
