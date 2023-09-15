/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertORTopoToTapiFullTopo {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToTapiFullTopo.class);
    private String ietfNodeId;
    private OpenroadmNodeType ietfNodeType;
    private AdminStates ietfNodeAdminState;
    private State ietfNodeOperState;
    private List<TerminationPoint> oorClientPortList;
    private List<TerminationPoint> oorNetworkPortList;
    private OduSwitchingPools oorOduSwitchingPool;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private Map<String, Uuid> uuidMap;
    private final TapiLink tapiLink;


    public ConvertORTopoToTapiFullTopo(Uuid tapiTopoUuid, TapiLink tapiLink) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.uuidMap = new HashMap<>();
        this.tapiSips = new HashMap<>();
        this.tapiLink = tapiLink;
    }

    public void convertNode(Node ietfNode, List<String> networkPorts) {
        this.ietfNodeId = ietfNode.getNodeId().getValue();
        if (ietfNode.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                == null) {
            return;
        }
        this.ietfNodeType = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class).getNodeType();
        this.ietfNodeAdminState = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                .getAdministrativeState();
        this.ietfNodeOperState = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                .getOperationalState();
        this.oorNetworkPortList = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
                .getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERNETWORK.getIntValue()
                    && networkPorts.contains(tp.getTpId().getValue()))
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
        if (!OpenroadmNodeType.TPDR.equals(this.ietfNodeType)) {
            this.oorOduSwitchingPool = ietfNode.augmentation(Node1.class).getSwitchingPools().getOduSwitchingPools()
                .values().stream().findFirst().orElseThrow();
            this.oorClientPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
                    .getTerminationPoint().values().stream()
                    .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                        == OpenroadmTpType.XPONDERCLIENT.getIntValue())
                    .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                    .collect(Collectors.toList());
        } else {
            this.oorOduSwitchingPool = createOduSwitchingPoolForTp100G();
            List<TpId> tpList = this.oorOduSwitchingPool.getNonBlockingList().values().stream()
                .flatMap(nbl -> nbl.getTpList().stream())
                .collect(Collectors.toList());
            this.oorClientPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
                    .getTerminationPoint().values().stream()
                    .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                        == OpenroadmTpType.XPONDERCLIENT.getIntValue() && tpList.contains(tp.getTpId()))
                    .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                    .collect(Collectors.toList());
            this.oorClientPortList.forEach(tp -> LOG.info("tp = {}", tp.getTpId()));
        }

        // node creation XPDR ([DSR/ODU] and OTSI merged in R 2.4.X
        LOG.info("creation of a DSR/ODU node for {}", this.ietfNodeId);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId,
            TapiStringConstants.XPDR)).getBytes(Charset.forName("UTF-8"))).toString());
        this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR), nodeUuid);
        Name nameDsrNode = new NameBuilder().setValueName("dsr/odu node name").setValue(
            String.join("+", this.ietfNodeId, TapiStringConstants.XPDR)).build();
        Name nameOtsiNode =  new NameBuilder().setValueName("otsi node name").setValue(
            String.join("+", this.ietfNodeId, TapiStringConstants.XPDR)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(this.ietfNodeType.getName()).build();
        Set<LayerProtocolName> dsrLayerProtocols = Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
            LayerProtocolName.DIGITALOTN, LayerProtocolName.PHOTONICMEDIA);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology
            .Node dsrNode = createTapiNode(Map.of(nameDsrNode.key(), nameDsrNode, nameOtsiNode.key(), nameOtsiNode,
            nameNodeType.key(), nameNodeType), dsrLayerProtocols);
        LOG.info("XPDR Node {} should have {} NEPs and {} SIPs", this.ietfNodeId,
            this.oorClientPortList.size() + this.oorNetworkPortList.size(),
            this.oorClientPortList.size() + this.oorNetworkPortList.size());
        LOG.info("XPDR Node {} has {} NEPs and {} SIPs", this.ietfNodeId,
            dsrNode.getOwnedNodeEdgePoint().values().size(), dsrNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());
        tapiNodes.put(dsrNode.key(), dsrNode);
    }

    public void convertRdmToRdmLinks(List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link> rdmTordmLinkList) {
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} roadm to roadm links", rdmTordmLinkList.size() / 2);
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link link : rdmTordmLinkList) {
            if (!linksToNotConvert.contains(link.getLinkId().getValue())) {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.network.topology.rev180226.networks.network.Link oppositeLink = rdmTordmLinkList.stream()
                    .filter(l -> l.getLinkId().equals(link.augmentation(Link1.class).getOppositeLink()))
                    .findAny().orElse(null);

                AdminStates oppLnkAdmState = null;
                State oppLnkOpState = null;
                if (oppositeLink != null) {
                    oppLnkAdmState = oppositeLink.augmentation(Link1.class).getAdministrativeState();
                    oppLnkOpState = oppositeLink.augmentation(Link1.class).getOperationalState();
                }
                String adminState =
                    link.augmentation(Link1.class).getAdministrativeState() == null
                        || oppLnkAdmState == null
                    ? null
                    : this.tapiLink.setTapiAdminState(
                        link.augmentation(Link1.class).getAdministrativeState(), oppLnkAdmState).getName();
                String operState =
                    link.augmentation(Link1.class).getOperationalState() == null
                        || oppLnkOpState == null
                    ? null
                    : this.tapiLink.setTapiOperationalState(
                        link.augmentation(Link1.class).getOperationalState(), oppLnkOpState).getName();

                Link tapLink = this.tapiLink.createTapiLink(String.join("-",
                        link.getSource().getSourceNode().getValue().split("-")[0],
                        link.getSource().getSourceNode().getValue().split("-")[1]),
                    link.getSource().getSourceTp().getValue(), String.join("-",
                        link.getDestination().getDestNode().getValue().split("-")[0],
                        link.getDestination().getDestNode().getValue().split("-")[1]),
                    link.getDestination().getDestTp().getValue(), TapiStringConstants.OMS_RDM_RDM_LINK,
                    TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.PHTNC_MEDIA,
                    TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS, adminState, operState,
                    Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                    this.tapiTopoUuid);
                linksToNotConvert.add(link
                    .augmentation(Link1.class)
                    .getOppositeLink().getValue());
                tapiLinks.put(tapLink.key(), tapLink);
            }
        }
    }

    public void convertRoadmNode(Node roadm, Network openroadmTopo) {
        this.ietfNodeId = roadm.getNodeId().getValue();
        this.ietfNodeType = roadm.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class).getNodeType();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneplist = new HashMap<>();
        // 1. Get degree and srg nodes to map TPs into NEPs
        if (openroadmTopo.getNode() == null) {
            LOG.warn("Openroadm-topology is null.");
            return;
        }
        int numNeps = 0;
        int numSips = 0;
        List<Node> nodeList = new ArrayList<Node>(openroadmTopo.getNode().values());
        for (Node node:nodeList) {
            if (node.getSupportingNode().values().stream().noneMatch(sp -> sp.getNodeRef().getValue()
                .equals(this.ietfNodeId))) {
                LOG.debug("Abstracted node {} is not part of {}",
                    node.getNodeId().getValue(), this.ietfNodeId);
                continue;
            }
            if (node.augmentation(Node1.class) == null
                && node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.topology.rev180226.Node1.class) == null) {
                LOG.warn("Abstracted node {} doesnt have type of node or is not disaggregated",
                    node.getNodeId().getValue());
                continue;
            }
            OpenroadmNodeType nodeType = node.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.common.network.rev230526.Node1.class).getNodeType();
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
                node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.Node1.class);
            LOG.info("TPs of node: {}", node1.getTerminationPoint().values());
            switch (nodeType.getIntValue()) {
                case 11:
                    LOG.info("Degree node");
                    // Get only external TPs of the degree
                    List<TerminationPoint> degPortList = node1.getTerminationPoint().values().stream()
                        .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.DEGREETXRXTTP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.DEGREERXTTP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.DEGREETXTTP.getIntValue())
                        .collect(Collectors.toList());
                    // Convert TP List in NEPs and put it in onepl
                    LOG.info("Degree port List: {}", degPortList);
                    // TODO: deg port could be sip. e.g. MDONS
                    oneplist.putAll(populateNepsForRdmNode(degPortList, false, TapiStringConstants.PHTNC_MEDIA_OTS));
                    oneplist.putAll(populateNepsForRdmNode(degPortList, false, TapiStringConstants.PHTNC_MEDIA_OMS));
                    numNeps += degPortList.size() * 2;
                    break;
                case 12:
                    LOG.info("SRG node");
                    // Get only external TPs of the srg
                    List<TerminationPoint> srgPortList = node1.getTerminationPoint().values().stream()
                        .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.SRGTXRXPP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.SRGRXPP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.SRGTXPP.getIntValue())
                        .collect(Collectors.toList());
                    // Convert TP List in NEPs and put it in onepl
                    LOG.info("Srg port List: {}", srgPortList);
                    oneplist.putAll(populateNepsForRdmNode(srgPortList, true, TapiStringConstants.PHTNC_MEDIA_OTS));
                    numNeps += srgPortList.size();
                    numSips += srgPortList.size();
                    break;
                default:
                    LOG.error("Node {} type not supported", nodeType.getName());
            }
        }
        // create tapi Node
        // UUID
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", roadm.getNodeId().getValue(),
            TapiStringConstants.PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info("Creation of PHOTONIC node for {}, of Uuid {}", roadm.getNodeId().getValue(), nodeUuid.toString());
        // Names
        Name nodeNames =  new NameBuilder().setValueName("roadm node name")
            .setValue(String.join("+", roadm.getNodeId().getValue(), TapiStringConstants.PHTNC_MEDIA)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(this.ietfNodeType.getName()).build();
        // Protocol Layer
        Set<LayerProtocolName> layerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        // Build tapi node
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology
            .Node roadmNode = createRoadmTapiNode(nodeUuid,
            Map.of(nodeNames.key(), nodeNames, nameNodeType.key(), nameNodeType), layerProtocols, oneplist);
        // TODO add states corresponding to device config
        LOG.info("ROADM node {} should have {} NEPs and {} SIPs", roadm.getNodeId().getValue(), numNeps, numSips);
        LOG.info("ROADM node {} has {} NEPs and {} SIPs", roadm.getNodeId().getValue(),
            roadmNode.nonnullOwnedNodeEdgePoint().values().size(),
            roadmNode.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());

        tapiNodes.put(roadmNode.key(), roadmNode);
    }

    private OduSwitchingPools createOduSwitchingPoolForTp100G() {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        int count = 1;
        for (TerminationPoint tp : this.oorNetworkPortList) {
            TpId tpid1 = tp.getTpId();
            TpId tpid2 = tp.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1.class)
                    .getAssociatedConnectionMapTp().iterator().next();
            Set<TpId> tpList = new HashSet<>();
            tpList.add(tpid1);
            tpList.add(tpid2);
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(count))
                .setTpList(tpList)
                .build();
            nblMap.put(nbl.key(), nbl);
            count++;
        }
        return new OduSwitchingPoolsBuilder()
            .setNonBlockingList(nblMap)
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
            createTapiNode(Map<NameKey, Name> nodeNames, Set<LayerProtocolName> layerProtocols) {
        Uuid nodeUuid = null;
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Set<RuleType> ruleTypes = new HashSet<>();
        ruleTypes.add(RuleType.FORWARDING);
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(ruleTypes)
            .build();
        ruleList.put(rule.key(), rule);
        if (layerProtocols.contains(LayerProtocolName.DSR)
                || layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
            nodeUuid = getNodeUuid4Dsr(onepl, nodeRuleGroupList, ruleList);
        } else {
            LOG.error("Undefined LayerProtocolName for {} node {}", nodeNames.get(nodeNames.keySet().iterator().next())
                .getValueName(), nodeNames.get(nodeNames.keySet().iterator().next()).getValue());
        }
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiStringConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        RiskParameterPac riskParamPac = new RiskParameterPacBuilder()
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nodeNames)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(this.tapiLink.setTapiAdminState(this.ietfNodeAdminState.getName()))
            .setOperationalState(this.tapiLink.setTapiOperationalState(this.ietfNodeOperState.getName()))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepl)
            .setNodeRuleGroup(nodeRuleGroupList)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(riskParamPac)
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
             createRoadmTapiNode(Uuid nodeUuid, Map<NameKey, Name> nameMap, Set<LayerProtocolName> layerProtocols,
             Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneplist) {
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiStringConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        RiskParameterPac riskParamPac = new RiskParameterPacBuilder()
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nameMap)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(oneplist)
            .setNodeRuleGroup(createNodeRuleGroupForRdmNode(nodeUuid, oneplist.values()))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(riskParamPac)
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private Uuid getNodeUuid4Dsr(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
                                 Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList, Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR));
        // client nep creation on DSR node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                oorClientPortList.get(i).getTpId().getValue()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue()), nepUuid);
            NameBuilder nameBldr = new NameBuilder().setValue(
                String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue()));
            Name name;
            if (OpenroadmNodeType.TPDR.equals(this.ietfNodeType)) {
                name = nameBldr.setValueName("100G-tpdr").build();
            } else {
                name = nameBldr.setValueName("NodeEdgePoint_C").build();
            }

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(name.key(), name),
                LayerProtocolName.DSR, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId,
                    TapiStringConstants.DSR));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on I_ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_N")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId,
                    TapiStringConstants.I_ODU));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on E_ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                oorClientPortList.get(i).getTpId().getValue()));
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                    oorClientPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                oorClientPortList.get(i).getTpId().getValue()), nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint_N")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                    oorClientPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, false, String.join("+", this.ietfNodeId,
                    TapiStringConstants.E_ODU));
            onepl.put(onep.key(), onep);
        }
        // OTS network nep creation
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid2);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS));
            onepl.put(onep.key(), onep);
        }
        // OTSI_MC network nep creation
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid3);
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC));
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        LOG.info("ODU switching pool = {}", this.oorOduSwitchingPool.nonnullNonBlockingList().values());
        for (NonBlockingList nbl : this.oorOduSwitchingPool.nonnullNonBlockingList().values()) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
                nepList = new HashMap<>();
            LOG.info("UUidMap={}", this.uuidMap.keySet());
            LOG.info("TP list = {}", nbl.getTpList());
            for (TpId tp : nbl.getTpList()) {
                LOG.info("TP={}", tp.getValue());
                LOG.info("UuidKey={}", String.join("+", this.ietfNodeId,
                    TapiStringConstants.E_ODU, tp.getValue()));
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId,
                            TapiStringConstants.E_ODU, tp.getValue()))
                        && this.uuidMap.containsKey(String.join("+", this.ietfNodeId,
                            TapiStringConstants.DSR, tp.getValue()))) {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                        nep1 = new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            TapiStringConstants.XPDR)))
                        .setNodeEdgePointUuid(this.uuidMap.get(String.join(
                            "+", this.ietfNodeId, TapiStringConstants.DSR, tp.getValue())))
                        .build();
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                        nep2 = new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            TapiStringConstants.XPDR)))
                        .setNodeEdgePointUuid(this.uuidMap.get(String.join(
                            "+", this.ietfNodeId, TapiStringConstants.E_ODU, tp.getValue())))
                        .build();
                    nepList.put(nep1.key(), nep1);
                    nepList.put(nep2.key(), nep2);
                }
            }
            //LOG.info("NEPLIST is {}", nepList.toString());
            // Empty random creation of mandatory fields for avoiding errors....
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiStringConstants.COST_HOP_VALUE)
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
                .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                .build();
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(("dsr node rule group " + count)
                    .getBytes(Charset.forName("UTF-8"))).toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupList.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeUuid;
    }

    private OwnedNodeEdgePoint createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
                                         LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip,
                                         String keyword) {
        String key = String.join("+", keyword, oorTp.getTpId().getValue());
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(key))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(createMSIP(1, nepProtocol, oorTp, keyword));
        }
        AdministrativeState adminState = null;
        OperationalState operState = null;
        if (oorTp.augmentation(TerminationPoint1.class).getAdministrativeState() != null) {
            adminState = this.tapiLink.setTapiAdminState(oorTp.augmentation(TerminationPoint1.class)
                .getAdministrativeState().getName());
        }
        if (oorTp.augmentation(TerminationPoint1.class).getOperationalState() != null) {
            operState = this.tapiLink.setTapiOperationalState(oorTp.augmentation(TerminationPoint1.class)
                .getOperationalState().getName());
        }
        onepBldr.setSupportedCepLayerProtocolQualifierInstances(createSupportedLayerProtocolQualifier(oorTp,
                nepProtocol))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED);
        return onepBldr.build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(List<TerminationPoint> tpList,
            boolean withSip, String nepPhotonicSublayer) {
        // create neps for MC and OTSiMC and Photonic Media
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (TerminationPoint tp:tpList) {
            // Admin and oper state common for all tps
            AdminStates admin = tp.augmentation(TerminationPoint1.class).getAdministrativeState();
            State oper = tp.augmentation(TerminationPoint1.class).getOperationalState();
            // PHOTONIC MEDIA nep
            LOG.info("PHOTO NEP = {}", String.join("+", this.ietfNodeId, nepPhotonicSublayer,
                tp.getTpId().getValue()));
            Name nepName = new NameBuilder()
                .setValueName(nepPhotonicSublayer + "NodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, nepPhotonicSublayer,
                    tp.getTpId().getValue()))
                .build();

            List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
            sclpqiList.add(
                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                    .setLayerProtocolQualifier(
                        TapiStringConstants.PHTNC_MEDIA_OMS.equals(nepPhotonicSublayer)
                            ? PHOTONICLAYERQUALIFIEROMS.VALUE
                            : PHOTONICLAYERQUALIFIEROTS.VALUE)
                    .setNumberOfCepInstances(Uint64.valueOf(1))
                    .build());
            OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId,
                    nepPhotonicSublayer, tp.getTpId().getValue()))
                    .getBytes(Charset.forName("UTF-8"))).toString()))
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName.key(), nepName))
                .setSupportedCepLayerProtocolQualifierInstances(sclpqiList)
                .setDirection(Direction.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(this.tapiLink.setTapiAdminState(admin.getName()))
                .setOperationalState(this.tapiLink.setTapiOperationalState(oper.getName()))
                .setLifecycleState(LifecycleState.INSTALLED)
                .build();
            onepMap.put(onep.key(), onep);
        }
        return onepMap;
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForRdmNode(Uuid nodeUuid,
                                                                               Collection<OwnedNodeEdgePoint> onepl) {
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
            nepMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : onepl) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group
                .NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onep.key().getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Set<RuleType> ruleTypes = new HashSet<>();
        ruleTypes.add(RuleType.FORWARDING);
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(ruleTypes)
            .build();
        ruleList.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((this.ietfNodeId + " node rule group")
                .getBytes(Charset.forName("UTF-8"))).toString()))
            .setRule(ruleList)
            .setNodeEdgePoint(nepMap)
            .build();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        return nodeRuleGroupMap;
    }

    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createMSIP(int nb,
                                                                                        LayerProtocolName layerProtocol,
                                                                                        TerminationPoint tp,
                                                                                        String nodeid) {
        // add them to SIP context
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            LOG.info("SIP = {}", String.join("+", "SIP", nodeid, tp.getTpId().getValue()));
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", nodeid,
                tp.getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sipUuid).build();
            ServiceInterfacePoint sip = createSIP(sipUuid, layerProtocol, tp, nodeid);
            this.tapiSips.put(sip.key(), sip);
            msipl.put(msip.key(), msip);
        }
        return msipl;
    }

    private ServiceInterfacePoint createSIP(Uuid sipUuid, LayerProtocolName layerProtocol, TerminationPoint tp,
                                            String nodeid) {
        // TODO: what value should be set in total capacity and available capacity??
        // LOG.info("SIP name = {}", String.join("+", nodeid, tp.getTpId().getValue()));
        Name sipName = new NameBuilder()
            .setValueName("SIP name")
            .setValue(String.join("+", nodeid, tp.getTpId().getValue()))
            .build();
        AdministrativeState adminState = null;
        OperationalState operState = null;
        if (tp.augmentation(TerminationPoint1.class).getAdministrativeState() != null) {
            adminState = this.tapiLink.setTapiAdminState(tp.augmentation(TerminationPoint1.class)
                .getAdministrativeState().getName());
        }
        if (tp.augmentation(TerminationPoint1.class).getOperationalState() != null) {
            operState = this.tapiLink.setTapiOperationalState(tp.augmentation(TerminationPoint1.class)
                .getOperationalState().getName());
        }
        return new ServiceInterfacePointBuilder()
            .setUuid(sipUuid)
            .setName(Map.of(sipName.key(), sipName))
            .setLayerProtocolName(layerProtocol)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setAvailableCapacity(new AvailableCapacityBuilder().build())
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().build())
            .setSupportedCepLayerProtocolQualifierInstances(createSipSupportedLayerProtocolQualifier(
                tp, layerProtocol))
            .build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
            .service._interface.point.SupportedCepLayerProtocolQualifierInstances>
            createSipSupportedLayerProtocolQualifier(TerminationPoint tp,
            LayerProtocolName lpn) {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
            .service._interface.point.SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1 tp1 =
            tp.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.otn.network.topology.rev230526.TerminationPoint1.class);
        if (tp1 == null) {
            return new ArrayList<>(sclpqiList);
        }
        if (tp1.getTpSupportedInterfaces() == null) {
            LOG.warn("Tp supported interface doesnt exist on TP {}", tp.getTpId().getValue());
            return new ArrayList<>(sclpqiList);
        }
        Collection<SupportedInterfaceCapability> sicList = tp1.getTpSupportedInterfaces()
            .getSupportedInterfaceCapability().values();
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (lpn.getName()) {
                case "DSR":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPEGigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GE":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GE":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "ODU":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                        case "If10GE":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                        case "If100GE":
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    if (ifCapType.equals("IfOCHOTU4ODU4") || ifCapType.equals("IfOCH")) {
                        sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                        sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                            .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown");
                    break;
            }
        }
        return sclpqiList.stream().distinct().toList();
    }

    private List<SupportedCepLayerProtocolQualifierInstances> createSupportedLayerProtocolQualifier(TerminationPoint tp,
            LayerProtocolName lpn) {
        List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1 tp1 =
            tp.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.otn.network.topology.rev230526.TerminationPoint1.class);
        if (tp1 == null) {
            return new ArrayList<>(sclpqiList);
        }
        if (tp1.getTpSupportedInterfaces() == null) {
            LOG.warn("Tp supported interface doesnt exist on TP {}", tp.getTpId().getValue());
            return new ArrayList<>(sclpqiList);
        }
        Collection<SupportedInterfaceCapability> sicList = tp1.getTpSupportedInterfaces()
            .getSupportedInterfaceCapability().values();
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (lpn.getName()) {
                case "DSR":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPEGigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "ODU":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                        case "If10GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                        case "If100GE":
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    if (ifCapType.equals("IfOCHOTU4ODU4") || ifCapType.equals("IfOCH")) {
                        sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                        sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown");
                    break;
            }
        }
        return sclpqiList.stream().distinct().toList();
    }

    public void convertXpdrToRdmLinks(List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link> xpdrRdmLinkList) {
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} xpdr to roadm links", xpdrRdmLinkList.size() / 2);
        // LOG.info("Link list = {}", xpdrRdmLinkList.toString());
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link link:xpdrRdmLinkList) {
            if (!linksToNotConvert.contains(link.getLinkId().getValue())) {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.networks.network.Link oppositeLink = xpdrRdmLinkList.stream()
                    .filter(l -> l.getLinkId().equals(link.augmentation(Link1.class).getOppositeLink())).findAny()
                    .orElse(null);

                AdminStates oppLnkAdmState = null;
                State oppLnkOpState = null;
                if (oppositeLink != null) {
                    oppLnkAdmState = oppositeLink.augmentation(Link1.class).getAdministrativeState();
                    oppLnkOpState = oppositeLink.augmentation(Link1.class).getOperationalState();
                }
                String adminState =
                    link.augmentation(Link1.class).getAdministrativeState() == null
                        || oppLnkAdmState == null
                    ? null
                    : this.tapiLink.setTapiAdminState(
                        link.augmentation(Link1.class).getAdministrativeState(), oppLnkAdmState).getName();
                String operState =
                    link.augmentation(Link1.class).getOperationalState() == null
                        || oppLnkOpState == null
                    ? null
                    : this.tapiLink.setTapiOperationalState(
                        link.augmentation(Link1.class).getOperationalState(), oppLnkOpState).getName();

                String sourceNode = (link.getSource().getSourceNode().getValue().contains("ROADM"))
                    ? getIdBasedOnModelVersion(link.getSource().getSourceNode().getValue())
                    : link.getSource().getSourceNode().getValue();
                String sourceTp = link.getSource().getSourceTp().getValue();
                String sourceNodeQual = sourceNode.contains("ROADM") ? TapiStringConstants.PHTNC_MEDIA
                    : TapiStringConstants.XPDR;
                String destNode = (link.getDestination().getDestNode().getValue().contains("ROADM"))
                    ? getIdBasedOnModelVersion(link.getDestination().getDestNode().getValue())
                    : link.getDestination().getDestNode().getValue();
                String destTp = link.getDestination().getDestTp().getValue();
                String destNodeQual = destNode.contains("ROADM") ? TapiStringConstants.PHTNC_MEDIA
                    : TapiStringConstants.XPDR;
                Link tapLink = this.tapiLink.createTapiLink(sourceNode, sourceTp, destNode, destTp,
                    TapiStringConstants.OMS_XPDR_RDM_LINK, sourceNodeQual, destNodeQual,
                    TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS, adminState,
                    operState, Set.of(LayerProtocolName.PHOTONICMEDIA),
                    Set.of(LayerProtocolName.PHOTONICMEDIA.getName()), this.tapiTopoUuid);
                linksToNotConvert.add(link.augmentation(Link1.class).getOppositeLink().getValue());
                this.tapiLinks.put(tapLink.key(), tapLink);
            }
        }
    }

    private String getIdBasedOnModelVersion(String linknodeid) {
        if (linknodeid.matches("[A-Z]{5}-[A-Z0-9]{2}-.*")) {
            LOG.info("OpenROADM version > 1.2.1 {}", linknodeid);
            return String.join("-", linknodeid.split("-")[0], linknodeid.split("-")[1]);
        } else {
            LOG.info("OpenROADM version <= 1.2.1 {}", linknodeid);
            return linknodeid.split("-")[0];
        }
    }

    public Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            getTapiNodes() {
        return tapiNodes;
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }
}
