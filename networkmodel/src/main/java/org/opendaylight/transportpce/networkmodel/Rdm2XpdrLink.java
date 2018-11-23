/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.links.input.grouping.LinksInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Rdm2XpdrLink {

    private static final Logger LOG = LoggerFactory.getLogger(Rdm2XpdrLink.class);

    public static boolean createXpdrRdmLinks(LinksInput linksInput, OpenRoadmTopology openRoadmTopology,
        DataBroker dataBroker) {
        String srcNode =
                new StringBuilder(linksInput.getXpdrNode()).append("-XPDR").append(linksInput.getXpdrNum()).toString();
        String srcTp = new StringBuilder("XPDR").append(linksInput.getXpdrNum()).append("-NETWORK")
                .append(linksInput.getNetworkNum()).toString();
        String destNode =
                new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
        String destTp = linksInput.getTerminationPointNum();

        Network topoNetowkLayer = createNetworkBuilder(srcNode, srcTp, destNode, destTp, false,
                openRoadmTopology).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
        WriteTransaction wrtx = dataBroker.newWriteOnlyTransaction();
        wrtx.merge(LogicalDatastoreType.CONFIGURATION, nwIID.build(), topoNetowkLayer);

        CheckedFuture<Void, TransactionCommitFailedException> submit = wrtx.submit();

        try {
            submit.checkedGet();
            LOG.info("Post successful");
            return true;

        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to create Xponder to Roadm link in the Topo layer ");
            return false;

        }
    }

    public static boolean createRdmXpdrLinks(LinksInput linksInput,
                                             OpenRoadmTopology openRoadmTopology, DataBroker dataBroker) {
        String srcNode =
                new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
        String srcTp = linksInput.getTerminationPointNum();
        String destNode =
                new StringBuilder(linksInput.getXpdrNode()).append("-XPDR").append(linksInput.getXpdrNum()).toString();
        String destTp = new StringBuilder("XPDR").append(linksInput.getXpdrNum()).append("-NETWORK")
                .append(linksInput.getNetworkNum()).toString();

        Network topoNetowkLayer = createNetworkBuilder(srcNode, srcTp, destNode, destTp, true,
                openRoadmTopology).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
        WriteTransaction wrtx = dataBroker.newWriteOnlyTransaction();
        wrtx.merge(LogicalDatastoreType.CONFIGURATION, nwIID.build(), topoNetowkLayer);
        CheckedFuture<Void, TransactionCommitFailedException> submit = wrtx.submit();
        try {
            submit.checkedGet();
            LOG.info("Post successful");
            return true;

        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to create Xponder to Roadm link in the Topo layer ");
            return false;
        }
    }

    private static NetworkBuilder createNetworkBuilder(String srcNode, String srcTp, String destNode, String destTp,
            boolean isXponderInput, OpenRoadmTopology openRoadmTopology) {
        NetworkId nwId = new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID);
        NetworkBuilder nwBuilder = new NetworkBuilder();
        nwBuilder.setNetworkId(nwId);
        nwBuilder.withKey(new NetworkKey(nwId));
        Link1Builder lnk1bldr = new Link1Builder();
        LinkBuilder linkBuilder = openRoadmTopology.createLink(srcNode, destNode, srcTp, destTp, true);
        lnk1bldr.setLinkType(isXponderInput ? OpenroadmLinkType.XPONDERINPUT : OpenroadmLinkType.XPONDEROUTPUT);
        linkBuilder.addAugmentation(Link1.class, lnk1bldr.build());
        LOG.info("Link id in the linkbldr {}", linkBuilder.getLinkId());
        LOG.info("Link with oppo link {}", linkBuilder.augmentation(Link1.class));
        Network1Builder nwBldr1 = new Network1Builder();
        nwBldr1.setLink(ImmutableList.of(linkBuilder.build()));
        nwBuilder.addAugmentation(Network1.class, nwBldr1.build());
        return nwBuilder;
    }

    private Rdm2XpdrLink() {
    }

}

