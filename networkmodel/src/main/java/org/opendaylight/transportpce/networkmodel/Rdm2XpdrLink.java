/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.networkmodel.util.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.links.input.grouping.LinksInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


final class Rdm2XpdrLink {

    private static final Logger LOG = LoggerFactory.getLogger(Rdm2XpdrLink.class);

    public static boolean createXpdrRdmLinks(LinksInput linksInput, DataBroker dataBroker) {
        String srcNode =
            new StringBuilder(linksInput.getXpdrNode()).append("-XPDR").append(linksInput.getXpdrNum()).toString();
        String srcTp = new StringBuilder("XPDR").append(linksInput.getXpdrNum()).append("-NETWORK")
            .append(linksInput.getNetworkNum()).toString();
        String destNode =
            new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
        String destTp = linksInput.getTerminationPointNum();
        // update tail-equipment-id for tp of link
        TerminationPoint xpdrTp = getTpofNode(srcNode, srcTp, dataBroker);

        Network topoNetowkLayer = createNetworkBuilder(srcNode, srcTp, destNode, destTp, false, xpdrTp).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
        WriteTransaction wrtx = dataBroker.newWriteOnlyTransaction();
        wrtx.merge(LogicalDatastoreType.CONFIGURATION, nwIID.build(), topoNetowkLayer);

        FluentFuture<? extends @NonNull CommitInfo> commit = wrtx.commit();

        try {
            commit.get();
            LOG.info("Post successful");
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create Xponder to Roadm link in the Topo layer ");
            return false;
        }
    }

    public static boolean createRdmXpdrLinks(LinksInput linksInput, DataBroker dataBroker) {
        String srcNode =
            new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
        String srcTp = linksInput.getTerminationPointNum();
        String destNode =
            new StringBuilder(linksInput.getXpdrNode()).append("-XPDR").append(linksInput.getXpdrNum()).toString();
        String destTp = new StringBuilder("XPDR").append(linksInput.getXpdrNum()).append("-NETWORK")
            .append(linksInput.getNetworkNum()).toString();
        TerminationPoint xpdrTp = getTpofNode(destNode, destTp, dataBroker);

        Network topoNetowkLayer = createNetworkBuilder(srcNode, srcTp, destNode, destTp, true, xpdrTp).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Network> nwIID =
            InstanceIdentifier.builder(Networks.class).child(Network.class,
            new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
        WriteTransaction wrtx = dataBroker.newWriteOnlyTransaction();
        wrtx.merge(LogicalDatastoreType.CONFIGURATION, nwIID.build(), topoNetowkLayer);
        FluentFuture<? extends @NonNull CommitInfo> commit = wrtx.commit();
        try {
            commit.get();
            LOG.info("Post successful");
            return true;

        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create Xponder to Roadm link in the Topo layer ");
            return false;
        }
    }

    private static NetworkBuilder createNetworkBuilder(String srcNode, String srcTp, String destNode, String destTp,
        boolean isXponderInput, TerminationPoint xpdrTp) {
        //update tp of nodes
        TerminationPointBuilder xpdrTpBldr = new TerminationPointBuilder(xpdrTp);
        if (xpdrTpBldr.augmentation(TerminationPoint1.class) != null) {
            LOG.warn("Rewritting tail-equipment-id {} on tp {} of node {}", xpdrTpBldr
                .augmentation(TerminationPoint1.class).getXpdrNetworkAttributes().getTailEquipmentId(), srcTp, srcNode);
        }
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();
        if (isXponderInput) {
            tp1Bldr.setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                .setTailEquipmentId(srcNode + "--" + srcTp).build());
        } else {
            tp1Bldr.setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                .setTailEquipmentId(destNode + "--" + destTp).build());
        }
        xpdrTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
            new Node1Builder().setTerminationPoint(ImmutableList.of(xpdrTpBldr.build())).build();
        NodeBuilder nodeBldr = new NodeBuilder()
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1.class, node1);
        if (isXponderInput) {
            nodeBldr.setNodeId(new NodeId(destNode));
        } else {
            nodeBldr.setNodeId(new NodeId(srcNode));
        }

        Link1Builder lnk1bldr = new Link1Builder();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder lnk2bldr
            = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder()
                .setLinkType(isXponderInput ? OpenroadmLinkType.XPONDERINPUT : OpenroadmLinkType.XPONDEROUTPUT)
                .setOppositeLink(LinkIdUtil.getOppositeLinkId(srcNode, srcTp, destNode, destTp));
        LinkBuilder linkBuilder = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp, null)
            .addAugmentation(Link1.class, lnk1bldr.build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class,
                lnk2bldr.build());

        LOG.info("Link id in the linkbldr {}", linkBuilder.getLinkId());
        LOG.info("Link with oppo link {}", linkBuilder.augmentation(Link1.class));
        Network1Builder nwBldr1 = new Network1Builder().setLink(ImmutableList.of(linkBuilder.build()));

        NetworkId nwId = new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID);
        NetworkBuilder nwBuilder = new NetworkBuilder()
            .setNetworkId(nwId)
            .withKey(new NetworkKey(nwId))
            .addAugmentation(Network1.class, nwBldr1.build())
            .setNode(ImmutableList.of(nodeBldr.build()));
        return nwBuilder;
    }

    private static TerminationPoint getTpofNode(String srcNode, String srcTp, DataBroker dataBroker) {
        InstanceIdentifier<TerminationPoint> iiTp = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId(srcNode)))
            .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(srcTp)))
            .build();
        @NonNull
        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        @NonNull
        FluentFuture<Optional<TerminationPoint>> tpFf = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iiTp);
        if (tpFf.isDone()) {
            try {
                Optional<TerminationPoint> tpOpt;
                tpOpt = tpFf.get();
                if (tpOpt.isPresent()) {
                    return tpOpt.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Impossible to get tp-id {} of node {} from {}", srcTp, srcNode,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            }
        }
        return null;
    }

    private Rdm2XpdrLink() {
    }

}
