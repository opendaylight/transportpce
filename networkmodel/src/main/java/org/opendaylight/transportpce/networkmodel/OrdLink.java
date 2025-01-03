/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.networkmodel.util.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitInterDomainLinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev240923.LinkClassEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OrdLink {

    private static final Logger LOG = LoggerFactory.getLogger(OrdLink.class);

    /**Method to create OMS links if not discovered by LLDP. This is helpful
     to create test topologies using simulators**/
    public static boolean createRdm2RdmLinks(InitRoadmNodesInput input, DataBroker dataBroker) {

        LinkId oppositeLinkId = LinkIdUtil.getRdm2RdmOppositeLinkId(input);

        //For opposite link augment
        Link1Builder oppsiteLinkBuilder =
            new Link1Builder()
                .setOppositeLink(oppositeLinkId)
                .setLinkType(OpenroadmLinkType.ROADMTOROADM);
        String srcNode = new StringBuilder(input.getRdmANode()).append("-DEG").append(input.getDegANum()).toString();
        String srcTp = input.getTerminationPointA();
        String destNode = new StringBuilder(input.getRdmZNode()).append("-DEG").append(input.getDegZNum()).toString();
        String destTp = input.getTerminationPointZ();

        // Check status of TPs to provide R2R link state
        TerminationPoint rdmSrcTp = getTpofNode(srcNode, srcTp, dataBroker);
        TerminationPoint rdmDstTp = getTpofNode(destNode, destTp, dataBroker);
        if (State.InService.equals(rdmSrcTp.augmentation(TerminationPoint1.class).getOperationalState())
                && State.InService.equals(rdmDstTp.augmentation(TerminationPoint1.class).getOperationalState())) {
            oppsiteLinkBuilder.setAdministrativeState(AdminStates.InService).setOperationalState(State.InService);
        } else {
            oppsiteLinkBuilder.setAdministrativeState(AdminStates.OutOfService).setOperationalState(State.OutOfService);
        }

        //IETF link builder
        LinkBuilder linkBuilder = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp, null);

        linkBuilder.addAugmentation(new Link1Builder().setOppositeLink(oppositeLinkId).build());
        linkBuilder.addAugmentation(oppsiteLinkBuilder.build());
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);

        // Building link instance identifier
        DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .augmentation(Network1.class)
            .child(Link.class, new LinkKey(linkId))
            .build();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, linkIID, linkBuilder.build());
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

    /**Method to create InterDomain links that can't be discovered by LLDP. This is used
    to create topologies that span across several domains**/
    public static boolean createInterDomainLinks(InitInterDomainLinksInput input, DataBroker dataBroker) {
        // Determination of the node that belongs to the TAPI domain
        String tapiDomainNode = "A";
        String aendUuid = input.getAEnd().getRdmTopologyUuid();
        String zendUuid = input.getZEnd().getRdmTopologyUuid();
        if (aendUuid == null && zendUuid == null) {
            LOG.error("Creation of Interdomain Link Error : Topology Uuid must be populated for at least 1 node");
            return false;
        }
        if (aendUuid == null && zendUuid != null) {
            tapiDomainNode = "Z";
        }
        LinkId linkId;
        LinkId oppLinkId;
        String srcNode;
        String srcTp;
        String destNode;
        String destTp;
        TerminationPoint rdmSrcTp;
        TerminationPoint rdmDestTp;
        State orTpState;
        if (tapiDomainNode.equals("Z")) {
            srcNode = new StringBuilder(input.getAEnd().getRdmNode()).append("-DEG")
                .append(input.getAEnd().getDegNum()).toString();
            srcTp = input.getAEnd().getTerminationPoint();
            rdmSrcTp = getTpofNode(srcNode, srcTp, dataBroker);
            destNode = "TAPI-SBI-ABS-NODE";
            destTp = input.getZEnd().getRdmNode() + "-" + input.getZEnd().getTerminationPoint();
            linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);
            oppLinkId = LinkIdUtil.buildLinkId(destNode, destTp, srcNode, srcTp);
            orTpState = rdmSrcTp.augmentation(TerminationPoint1.class).getOperationalState();
            addTpsToTapiExtNode(destTp, input.getZEnd().getRdmNepUuid(), input.getZEnd().getRdmNode(),
                input.getZEnd().getRdmNodeUuid(), input.getZEnd().getRdmTopologyUuid(), linkId.getValue(), dataBroker);
        } else {
            destNode = new StringBuilder(input.getZEnd().getRdmNode()).append("-DEG")
                .append(input.getZEnd().getDegNum()).toString();
            destTp = input.getZEnd().getTerminationPoint();
            rdmDestTp = getTpofNode(destNode, destTp, dataBroker);
            srcNode = "TAPI-SBI-ABS-NODE";
            srcTp = input.getAEnd().getRdmNode() + "-" + input.getAEnd().getTerminationPoint();
            oppLinkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);
            linkId = LinkIdUtil.buildLinkId(destNode, destTp, srcNode, srcTp);
            orTpState = rdmDestTp.augmentation(TerminationPoint1.class).getOperationalState();
            addTpsToTapiExtNode(srcTp, input.getAEnd().getRdmNepUuid(), input.getAEnd().getRdmNode(),
                input.getAEnd().getRdmNodeUuid(), input.getAEnd().getRdmTopologyUuid(), linkId.getValue(), dataBroker);
        }
        // IETF link builder
        LinkBuilder linkBuilderFW = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp, null);
        linkBuilderFW.addAugmentation(
                new Link1Builder()
                    .setOppositeLink(oppLinkId)
                    .setAdministrativeState(
                        State.InService.equals(orTpState) ? AdminStates.InService : AdminStates.OutOfService)
                    .setOperationalState(
                        State.InService.equals(orTpState) ? orTpState : State.OutOfService)
                    .setLinkType(OpenroadmLinkType.ROADMTOROADM)
                    .build());
        org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.or.network.augmentation.rev240923.Link1Builder tpceAugmLink11Bd =
            new org.opendaylight.yang.gen.v1.http.org.opendaylight
                    .transportpce.or.network.augmentation.rev240923.Link1Builder()
                .setLinkClass(LinkClassEnum.InterDomain);
        linkBuilderFW.addAugmentation(tpceAugmLink11Bd.build());

        LinkBuilder linkBuilderBW = TopologyUtils.createLink(destNode, srcNode, destTp, srcTp, null);
        linkBuilderBW.addAugmentation(
                new Link1Builder()
                    .setOppositeLink(linkId)
                    .setAdministrativeState(
                        State.InService.equals(orTpState) ? AdminStates.InService : AdminStates.OutOfService)
                    .setOperationalState(
                        State.InService.equals(orTpState) ? orTpState : State.OutOfService)
                    .setLinkType(OpenroadmLinkType.ROADMTOROADM)
                    .build());
        linkBuilderBW.addAugmentation(tpceAugmLink11Bd.build());

        // Building link instance identifier
        DataObjectIdentifier<Link> linkIIDFW = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .augmentation(Network1.class).child(Link.class, new LinkKey(linkId))
            .build();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, linkIIDFW, linkBuilderFW.build());
        try {
            writeTransaction.commit().get();
            LOG.info("A new link with linkId: {} added into {} layer.",
                linkId.getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create Direct Inter-domain-Link between Node {} tp {} and Node {} tp {} ",
                srcNode, srcTp, destNode, destTp);
            return false;
        }

        DataObjectIdentifier<Link> linkIIDBW = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .augmentation(Network1.class).child(Link.class, new LinkKey(oppLinkId))
            .build();
        writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, linkIIDBW, linkBuilderBW.build());
        try {
            writeTransaction.commit().get();
            LOG.info("A new link with linkId: {} added into {} layer.",
                oppLinkId.getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create reverse Inter-domain-Link between Node {} tp {} and Node {} tp {} ",
                destNode, destTp, srcNode, srcTp);
            return false;
        }
    }

    /**Generates terminating Tps on TAPI-SBI-ABS-NODE while creating inter-domain or alien-transpondersToRoadm links.
       TAPI-SBI-ABS-NODE abstracts T-API Topology retrieved through SouthBound API. **/

    public static TerminationPoint addTpsToTapiExtNode(String tpName, String tpUuid, String nodeName, String nodeUuid,
            String topoUuid, String linkId, DataBroker dataBroker) {
        //Ietf tpBuilder
        org.opendaylight.yang.gen.v1.http.org.opendaylight
                .transportpce.or.network.augmentation.rev240923.TerminationPoint1Builder tpceAugmTp111Bd =
            new org.opendaylight.yang.gen.v1.http.org.opendaylight
                    .transportpce.or.network.augmentation.rev240923.TerminationPoint1Builder()
                .setSupportingNodeTopologyUuid(topoUuid)
                .setSupportingNodeUuid(nodeUuid)
                .setTpUuid(tpUuid);
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                .TerminationPoint1Builder orAugmTp11Bd =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder();

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder()
                .addAugmentation(orAugmTp11Bd.build()).addAugmentation(tpceAugmTp111Bd.build())
            .setTpId(new TpId(tpName));
        //Set by default Operational state of created tp to InService
        tpBuilder.addAugmentation(new TerminationPoint1Builder()
            .setAdministrativeState(AdminStates.InService)
            .setOperationalState(State.InService).build());

        DataObjectIdentifier<TerminationPoint> tpIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
            .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpName)))
            .build();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, tpIID, tpBuilder.build());
        try {
            writeTransaction.commit().get();
            LOG.info("A new tp {} terminating Link {} has been added  to TAPI-SBI-ABS-NODE into {} layer.",
                tpName, linkId, NetworkUtils.OVERLAY_NETWORK_ID);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create new tp {} terminating link {} on TAPI-SBI-ABS-NODE ", tpName, linkId);
            return null;
        }
        return tpBuilder.build();
    }

    private static TerminationPoint getTpofNode(String srcNode, String srcTp, DataBroker dataBroker) {
        DataObjectIdentifier<TerminationPoint> iiTp = DataObjectIdentifier.builder(Networks.class)
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

    private OrdLink() {
    }
}
