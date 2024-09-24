/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import com.google.common.collect.ImmutableMap;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.links.input.grouping.LinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev240923.LinkClassEnum;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev240923.ModelEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
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
        TerminationPoint rdmTp;
        String destNode;
        String destTp;
        boolean isRdmTapiNode = false;
        if (linksInput.getRdmTopologyUuid() != null) {
            rdmTp = OrdLink.addTpsToTapiExtNode(("tp" + linksInput.getRdmNepUuid()),
                linksInput.getRdmNepUuid(), linksInput.getRdmNode(), linksInput.getRdmNodeUuid(),
                linksInput.getRdmTopologyUuid(),
                ("link from " + linksInput.getXpdrNode() + "to" + linksInput.getRdmNode()), dataBroker);
            destNode = linksInput.getRdmNode();
            destTp = ("tp" + linksInput.getRdmNepUuid());
            isRdmTapiNode = true;
        } else {
            destNode =
                new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
            destTp = linksInput.getTerminationPointNum();
            rdmTp = getTpofNode(destNode, destTp, dataBroker);
        }
        String srcNode =
            new StringBuilder(linksInput.getXpdrNode()).append("-XPDR").append(linksInput.getXpdrNum()).toString();
        String srcTp = new StringBuilder("XPDR").append(linksInput.getXpdrNum()).append("-NETWORK")
            .append(linksInput.getNetworkNum()).toString();
//        String destNode =
//            new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
//        String destTp = linksInput.getTerminationPointNum();
        // update tail-equipment-id for tp of link
        TerminationPoint xpdrTp = getTpofNode(srcNode, srcTp, dataBroker);
//        TerminationPoint rdmTp = getTpofNode(destNode, destTp, dataBroker);

        NetworkBuilder networkBldr = createNetworkBuilder(srcNode, srcTp, destNode, destTp, false, xpdrTp, rdmTp,
            isRdmTapiNode, dataBroker);
        if (networkBldr == null) {
            return false;
        }
        Network network = networkBldr.build();
        InstanceIdentifier.Builder<Network> nwIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
        WriteTransaction wrtx = dataBroker.newWriteOnlyTransaction();
        wrtx.merge(LogicalDatastoreType.CONFIGURATION, nwIID.build(), network);

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
        TerminationPoint rdmTp;
        String srcNode;
        String srcTp;
        boolean isRdmTapiNode = false;
        if (linksInput.getRdmTopologyUuid() != null) {
            rdmTp = OrdLink.addTpsToTapiExtNode(("tp" + linksInput.getRdmNepUuid()),
                linksInput.getRdmNepUuid(), linksInput.getRdmNode(), linksInput.getRdmNodeUuid(),
                linksInput.getRdmTopologyUuid(),
                ("link from " + linksInput.getRdmNode() + "to" + linksInput.getXpdrNode()), dataBroker);
            srcNode = linksInput.getRdmNode();
            srcTp = ("tp" + linksInput.getRdmNepUuid());
            isRdmTapiNode = true;
        } else {
            srcNode =
                new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
            srcTp = linksInput.getTerminationPointNum();
            rdmTp = getTpofNode(srcNode, srcTp, dataBroker);
        }
//        String srcNode =
//            new StringBuilder(linksInput.getRdmNode()).append("-SRG").append(linksInput.getSrgNum()).toString();
//        String srcTp = linksInput.getTerminationPointNum();
        String destNode =
            new StringBuilder(linksInput.getXpdrNode()).append("-XPDR").append(linksInput.getXpdrNum()).toString();
        String destTp = new StringBuilder("XPDR").append(linksInput.getXpdrNum()).append("-NETWORK")
            .append(linksInput.getNetworkNum()).toString();
        TerminationPoint xpdrTp = getTpofNode(destNode, destTp, dataBroker);
//        rdmTp = getTpofNode(srcNode, srcTp, dataBroker);

        NetworkBuilder networkBldr = createNetworkBuilder(srcNode, srcTp, destNode, destTp, true, xpdrTp, rdmTp,
            isRdmTapiNode, dataBroker);
        if (networkBldr == null) {
            return false;
        }
        Network network = networkBldr.build();
        InstanceIdentifier.Builder<Network> nwIID =
            InstanceIdentifier.builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
        WriteTransaction wrtx = dataBroker.newWriteOnlyTransaction();
        wrtx.merge(LogicalDatastoreType.CONFIGURATION, nwIID.build(), network);
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
            boolean isXponderInput, TerminationPoint xpdrTp, TerminationPoint rdmTp, boolean isRdmTapiNode,
        DataBroker dataBroker) {
        if (xpdrTp == null || rdmTp == null) {
            return null;
        }
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
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        TerminationPoint newXpdrTp = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
            new Node1Builder().setTerminationPoint(ImmutableMap.of(newXpdrTp.key(),newXpdrTp)).build();
        NodeBuilder nodeBldr = new NodeBuilder().addAugmentation(node1);
        if (isXponderInput) {
            nodeBldr.setNodeId(new NodeId(destNode));
        } else {
            nodeBldr.setNodeId(new NodeId(srcNode));
        }
        LinkId oppositeLinkId = isRdmTapiNode ? LinkIdUtil.buildLinkId(destNode, destTp, srcNode, srcTp)
            : LinkIdUtil.getOppositeLinkId(srcNode, srcTp, destNode, destTp);
        Link1Builder lnk2bldr
            = new Link1Builder()
                .setLinkType(isXponderInput ? OpenroadmLinkType.XPONDERINPUT : OpenroadmLinkType.XPONDEROUTPUT)
                .setOppositeLink(oppositeLinkId);

        // If both TPs of the Xpdr2Rdm link are inService --> link inService. Otherwise outOfService
        if (State.InService.equals(xpdrTp.augmentation(org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev230526.TerminationPoint1.class).getOperationalState())
                && State.InService.equals(rdmTp.augmentation(org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev230526.TerminationPoint1.class).getOperationalState())) {
            lnk2bldr.setOperationalState(State.InService).setAdministrativeState(AdminStates.InService);
        } else {
            lnk2bldr.setOperationalState(State.OutOfService).setAdministrativeState(AdminStates.OutOfService);
        }
        LinkBuilder linkBuilder = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp, null)
            .addAugmentation(lnk2bldr.build());

        org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.or.network.augmentation.rev240923.Link1Builder tpceAugmLink11Bd =
            new org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.or.network.augmentation.rev240923.Link1Builder();
        ModelEnum nodeModel = getNodeModel(srcNode, srcTp, dataBroker);
        if (isRdmTapiNode) {
            tpceAugmLink11Bd.setLinkClass(LinkClassEnum.AlienToTapi);
        } else if (nodeModel == null || nodeModel.equals(ModelEnum.Openroadm)) {
            tpceAugmLink11Bd.setLinkClass(LinkClassEnum.Openroadm);
        } else if (nodeModel.equals(ModelEnum.OpenconfigAlien)) {
            tpceAugmLink11Bd.setLinkClass(LinkClassEnum.AlienOcToOpenroadm);
        } else {
            LOG.error("Undetermined Node Model for XPDR {}, cannot define Link class for {}", srcNode,
                LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp));
        }
        linkBuilder.addAugmentation(tpceAugmLink11Bd.build());

        LOG.info("Creating Link with id {}", linkBuilder.getLinkId());
        Link link = linkBuilder.build();
        Network1Builder nwBldr1 = new Network1Builder().setLink(ImmutableMap.of(link.key(),link));

        NetworkId nwId = new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID);
        Node node = nodeBldr.build();
        return new NetworkBuilder()
            .setNetworkId(nwId)
            .withKey(new NetworkKey(nwId))
            .addAugmentation(nwBldr1.build())
            .setNode(ImmutableMap.of(node.key(),node));
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
                    return tpOpt.orElseThrow();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Impossible to get tp-id {} of node {} from {}", srcTp, srcNode,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            }
        }
        return null;
    }

    private static ModelEnum getNodeModel(String srcNode, String srcTp, DataBroker dataBroker) {
        InstanceIdentifier<
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev240923.Node1>
            nodeIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId(srcNode)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight
                    .transportpce.or.network.augmentation.rev240923.Node1.class)
                .build();
        @NonNull
        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        @NonNull
        FluentFuture<Optional<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation
                .rev240923.Node1>> nodeFf = readTransaction.read(LogicalDatastoreType.CONFIGURATION, nodeIID);
        if (nodeFf.isDone()) {
            try {
                Optional<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation
                    .rev240923.Node1> node;
                node = nodeFf.get();
                if (node.isPresent()) {
                    return node.orElseThrow().getNodeModel();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Impossible to get the NodeModel of node {} ", srcNode, e);
            }
        }
        return null;
    }

    private Rdm2XpdrLink() {
    }

}
