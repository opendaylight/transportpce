/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

//import com.google.common.util.concurrent.FluentFuture;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
//import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
//import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.networkmodel.util.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OrdLink {

    private static final Logger LOG = LoggerFactory.getLogger(OrdLink.class);

    /**Method to create OMS links if not discovered by LLDP. This is helpful
     to create test topologies using simulators**/
    public static boolean createRdm2RdmLinksTopo(InitRoadmNodesInput input, DataBroker dataBroker,
                                                 NetworkTransactionService networkTransactionService) {
        String srcNode = new StringBuilder(input.getRdmANode()).append("-DEG").append(input.getDegANum()).toString();
        String srcTp = input.getTerminationPointA();
        String destNode = new StringBuilder(input.getRdmZNode()).append("-DEG").append(input.getDegZNum()).toString();
        String destTp = input.getTerminationPointZ();
        LOG.info("Going to create r2r link between {}-{}", srcNode, destNode);
        LOG.info("Termination points connected {}-{}", srcTp, destTp);
        LinkId oppositeLinkId = LinkIdUtil.getRdm2RdmOppositeLinkId(input);
        LOG.info("Opposite link created: {}", oppositeLinkId.toString());
        //IETF link builder
        LinkBuilder linkBuilder = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp, null);
        LOG.info("Link builder created: {}", linkBuilder.toString());


        //For setting up attributes for openRoadm augment
        OMSAttributesBuilder omsAttributesBuilder = new OMSAttributesBuilder().setOppositeLink(oppositeLinkId);
        Link1Builder link1Builder = new Link1Builder().setOMSAttributes(
                omsAttributesBuilder.build());
        linkBuilder.addAugmentation(Link1.class,link1Builder.build());

        //For opposite link augment and state
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder oppsiteLinkBuilder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder();
        oppsiteLinkBuilder.setOppositeLink(oppositeLinkId);
        oppsiteLinkBuilder.setLinkType(OpenroadmLinkType.ROADMTOROADM);
        TerminationPoint rdmsrcTp = getTpofNode(srcNode, srcTp, networkTransactionService);
        LOG.info("Source tp created: {}", rdmsrcTp.toString());
        TerminationPoint rdmdestTp = getTpofNode(destNode, destTp, networkTransactionService);
        LOG.info("Destination tp created: {}", rdmdestTp.toString());
        if (rdmsrcTp.augmentation(TerminationPoint1.class).getOperationalState().equals(State.InService)
                && rdmdestTp.augmentation(TerminationPoint1.class).getOperationalState().equals(State.InService)) {
            oppsiteLinkBuilder.setOperationalState(State.InService);
            oppsiteLinkBuilder.setAdministrativeState(AdminStates.InService);
        } else {
            oppsiteLinkBuilder.setOperationalState(State.OutOfService);
            oppsiteLinkBuilder.setAdministrativeState(AdminStates.OutOfService);
        }
        linkBuilder.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .Link1.class, oppsiteLinkBuilder.build());
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);

        // Building link instance identifier
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(), linkBuilder.build());
        try {
            writeTransaction.commit().get();
            LOG.info("A new link with linkId: {} added into {} layer.",
                    linkId.getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create Roadm 2 Roadm Link for topo layer ");
            return false;
        }
    }

    public static boolean createRdm2RdmLinks(InitRoadmNodesInput input, DataBroker dataBroker) {
        String srcNode = new StringBuilder(input.getRdmANode()).append("-DEG").append(input.getDegANum()).toString();
        String srcTp = input.getTerminationPointA();
        String destNode = new StringBuilder(input.getRdmZNode()).append("-DEG").append(input.getDegZNum()).toString();
        String destTp = input.getTerminationPointZ();
        LOG.info("Going to create r2r link between {}-{}", srcNode, destNode);
        LOG.info("Termination points connected {}-{}", srcTp, destTp);
        LinkId oppositeLinkId = LinkIdUtil.getRdm2RdmOppositeLinkId(input);
        LOG.info("Opposite link created: {}", oppositeLinkId.toString());
        //IETF link builder
        LinkBuilder linkBuilder = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp, null);
        LOG.info("Link builder created: {}", linkBuilder.toString());


        //For setting up attributes for openRoadm augment
        OMSAttributesBuilder omsAttributesBuilder = new OMSAttributesBuilder().setOppositeLink(oppositeLinkId);
        Link1Builder link1Builder = new Link1Builder().setOMSAttributes(
                omsAttributesBuilder.build());
        linkBuilder.addAugmentation(Link1.class,link1Builder.build());

        //For opposite link augment and state
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder oppsiteLinkBuilder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder();
        oppsiteLinkBuilder.setOppositeLink(oppositeLinkId);
        oppsiteLinkBuilder.setLinkType(OpenroadmLinkType.ROADMTOROADM);

        linkBuilder.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .Link1.class, oppsiteLinkBuilder.build());
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);

        // Building link instance identifier
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(), linkBuilder.build());
        try {
            writeTransaction.commit().get();
            LOG.info("A new link with linkId: {} added into {} layer.",
                    linkId.getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create Roadm 2 Roadm Link for topo layer ");
            return false;
        }
    }


    private static TerminationPoint getTpofNode(String srcNode, String srcTp, NetworkTransactionService
            networkTransactionService) {
        LOG.info("Inside getTpofNode function");
        LOG.info("Source id = {}", srcNode);
        LOG.info("TP id = {}", srcTp);
        try {
            InstanceIdentifier<TerminationPoint> iiTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .child(Node.class, new NodeKey(new NodeId(srcNode)))
                    .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                            .topology.rev180226.Node1.class)
                    .child(TerminationPoint.class, new TerminationPointKey(new TpId(srcTp)))
                    .build();
            Optional<TerminationPoint> terminationPoint = networkTransactionService.read(
                    LogicalDatastoreType.CONFIGURATION, iiTp).get();
            LOG.info("TP obtained from read = {}", terminationPoint.toString());
            if (terminationPoint.isPresent()) {
                LOG.info("TP to return = {}", terminationPoint.get().toString());
                return terminationPoint.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Impossible to get tp-id {} of node {} from {}", srcTp, srcNode,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
        }
        return null;
    }

    private OrdLink(){
    }
}
