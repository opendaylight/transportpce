/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrdLink {

    private static final Logger LOG = LoggerFactory.getLogger(OrdLink.class);

    /**Method to create OMS links if not discovered by LLDP. This is helpful
    to create test topologies using simulators**/
    public static boolean createRdm2RdmLinks(InitRoadmNodesInput input,
        OpenRoadmTopology openRoadmTopology, DataBroker dataBroker) {

        LinkId oppositeLinkId = LinkIdUtil.getRdm2RdmOppositeLinkId(input);

        //For setting up attributes for openRoadm augment
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setLinkType(OpenroadmLinkType.ROADMTOROADM);
        OMSAttributesBuilder omsAttributesBuilder = new OMSAttributesBuilder();
        omsAttributesBuilder.setOppositeLink(oppositeLinkId);
        link1Builder.setOMSAttributes(omsAttributesBuilder.build());

        //For opposite link augment
        org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder oppsiteLinkBuilder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder();
        oppsiteLinkBuilder.setOppositeLink(oppositeLinkId);

        String srcNode = new StringBuilder(input.getRdmANode()).append("-DEG").append(input.getDegANum()).toString();
        String srcTp = input.getTerminationPointA();
        String destNode = new StringBuilder(input.getRdmZNode()).append("-DEG").append(input.getDegZNum()).toString();
        String destTp = input.getTerminationPointZ();

        //IETF link builder
        LinkBuilder linkBuilder = openRoadmTopology.createLink(srcNode, destNode, srcTp, destTp);

        linkBuilder.addAugmentation(Link1.class,link1Builder.build());
        linkBuilder.addAugmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.opposite.links.rev170929.Link1.class,oppsiteLinkBuilder.build());
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);

        // Building link instance identifier
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID =
            InstanceIdentifier.builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(), linkBuilder.build());
        try {
            writeTransaction.submit().get();
            LOG.info("A new link with linkId: {} added into {} layer.",
                    linkId.getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create Roadm 2 Roadm Link for topo layer ");
            return false;
        }
    }
}
