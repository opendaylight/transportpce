/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPEGigE;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointKey;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
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


public class ConvertORTopoToTapiTopo {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToTapiTopo.class);
    private String ietfNodeId;
    private OpenroadmNodeType ietfNodeType;
    private AdminStates ietfNodeAdminState = null;
    private State ietfNodeOperState = null;
    private List<TerminationPoint> oorClientPortList = null;
    private List<TerminationPoint> oorNetworkPortList = null;
    private OduSwitchingPools oorOduSwitchingPool = null;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<String, Uuid> uuidMap;
    private final TapiLink tapiLink;


    public ConvertORTopoToTapiTopo(Uuid tapiTopoUuid, TapiLink tapiLink) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.uuidMap = new HashMap<>();
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

        // node creation [DSR/ODU]
        LOG.info("creation of a DSR/ODU node for {}", this.ietfNodeId);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId,
            TapiStringConstants.XPDR)).getBytes(Charset.forName("UTF-8"))).toString());
        this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR), nodeUuid);
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name")
            .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR)).build();
        Name namePhot = new NameBuilder().setValueName("otsi node name")
            .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(this.ietfNodeType.getName()).build();
        Set<LayerProtocolName> dsrLayerProtocols = Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
            LayerProtocolName.DIGITALOTN,LayerProtocolName.PHOTONICMEDIA);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology
            .Node dsrNode = createTapiNode(Map.of(nameDsr.key(), nameDsr, namePhot.key(), namePhot,nameNodeType.key(),
                nameNodeType),
            dsrLayerProtocols);
        tapiNodes.put(dsrNode.key(), dsrNode);
    }

    public void convertLinks(Map<
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
                .LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
                .Link> otnLinkMap) {
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link> otnLinkList = new ArrayList<>(otnLinkMap.values());
        Collections.sort(otnLinkList, (l1, l2) -> l1.getLinkId().getValue()
            .compareTo(l2.getLinkId().getValue()));
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} otn links", otnLinkMap.size() / 2);
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
                .Link otnlink : otnLinkList) {
            if (!linksToNotConvert.contains(otnlink.getLinkId().getValue())) {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                    .network.Link oppositeLink = otnLinkMap.get(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
                        .yang.ietf.network.topology.rev180226.networks.network.LinkKey(otnlink.augmentation(Link1.class)
                    .getOppositeLink()));

                AdminStates oppLnkAdmState = null;
                State oppLnkOpState = null;
                String oppositeLinkId = null;
                if (oppositeLink != null) {
                    oppLnkAdmState = oppositeLink.augmentation(Link1.class).getAdministrativeState();
                    oppLnkOpState = oppositeLink.augmentation(Link1.class).getOperationalState();
                    oppositeLinkId = oppositeLink.getLinkId().getValue();
                }
                String adminState =
                    otnlink.augmentation(Link1.class).getAdministrativeState() == null
                        || oppLnkAdmState == null
                    ? null
                    : this.tapiLink.setTapiAdminState(
                        otnlink.augmentation(Link1.class).getAdministrativeState(), oppLnkAdmState).getName();
                String operState = otnlink.augmentation(Link1.class).getOperationalState() == null
                        || oppLnkOpState == null
                    ? null
                    : this.tapiLink.setTapiOperationalState(
                        otnlink.augmentation(Link1.class).getOperationalState(), oppLnkOpState).getName();
                // TODO: Handle not only OTU4 but also other cases
                String prefix = otnlink.getLinkId().getValue().split("-")[0];
                String nodesQual = TapiStringConstants.XPDR;
                String tpsQual = prefix.equals("OTU4") ? TapiStringConstants.I_OTSI : TapiStringConstants.E_ODU;
                LayerProtocolName layerProtocolName = prefix.equals("OTU4") ? LayerProtocolName.PHOTONICMEDIA
                    : LayerProtocolName.ODU;

                Link tapLink = this.tapiLink.createTapiLink(otnlink.getSource().getSourceNode().getValue(),
                    otnlink.getSource().getSourceTp().getValue(), otnlink.getDestination().getDestNode().getValue(),
                    otnlink.getDestination().getDestTp().getValue(), TapiStringConstants.OTN_XPDR_XPDR_LINK, nodesQual,
                    nodesQual, tpsQual, tpsQual, adminState, operState, Set.of(layerProtocolName),
                    Set.of(layerProtocolName.getName()), this.tapiTopoUuid);
                linksToNotConvert.add(oppositeLinkId);
                tapiLinks.put(tapLink.key(), tapLink);
                LOG.debug("Links converted are as follow  {}", tapiLinks.toString());
            }
        }
    }

    public void convertRoadmInfrastructure() {
        LOG.info("abstraction of the ROADM infrastructure towards a photonic node");
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.RDM_INFRA
            .getBytes(Charset.forName("UTF-8"))).toString());
        Name nodeName =  new NameBuilder().setValueName("otsi node name").setValue(TapiStringConstants.RDM_INFRA)
            .build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(OpenroadmNodeType.ROADM.getName()).build();
        Set<LayerProtocolName> nodeLayerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiPhotonicNodes
            = pruneTapiPhotonicNodes();
        //At that stage, there is no Roadm in the tapiPhotonicNodes Map / only the transponders
        Map<String, String> photonicNepUuisMap = convertListNodeWithListNepToMapForUuidAndName(tapiPhotonicNodes);
        // nep creation for rdm infra abstraction node
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = createNepForRdmNode(photonicNepUuisMap.size());
        // node rule group creation
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList
            = createNodeRuleGroupForRdmNode(nodeUuid, onepMap.values());
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
        // build RDM infra node abstraction
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node rdmNode = new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(Map.of(nodeName.key(), nodeName, nameNodeType.key(), nameNodeType))
            .setLayerProtocolName(nodeLayerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepMap)
            .setNodeRuleGroup(nodeRuleGroupList)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(riskParamPac)
            .build();
        tapiNodes.put(rdmNode.key(), rdmNode);
    // OTS link creation between photonic nodes and RDM infra abstraction node :
      //onepMap is a list of nep which Uuid is formed from THE ROADM node name, "nep" and an integer (order of the nep)
      // It has absolutely no relationship with the real ROADM infrastructure (SRG ports)
      //rdmInfraNepUuisMap is a Map <ROADMnodeUuuid--NepUuid; ROADMnodeName--nepName> built from onepMap
      //photonicNepUuisMap is a Map <TSPnodeUuuid--eNepUuid; TSPnodeName--nepName> built from TapiPhotonicNode
        Map<String, String> rdmInfraNepUuisMap = convertListNodeWithListNepToMapForUuidAndName(List.of(rdmNode));
        if (photonicNepUuisMap.size() != rdmInfraNepUuisMap.size()) {
            LOG.warn("Unable to build OTS links between photonics nodes and RDM infrasctructure abstraction");
        } else {
            //Tapi OtsLinks are created between Neps corresponding to the eNEPs of transponders (existing network ports)
            //and Generic NEPS with abstracted names created in the ROADM infrastructure corresponding to tps mirroring
            //transponders NETWORK PORTs. There is a simplification here considering that any network port of
            //transponders will have a mirroring SRG client port in the ROADM infrastructure.
            // TODO: Do not understand that we build OTS link without checking that existing transponder ports
            //are effectively connected. Need some consolidation
            createTapiOtsLinks(photonicNepUuisMap, rdmInfraNepUuisMap);
        }
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

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            pruneTapiPhotonicNodes() {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            prunedTapiPhotonicNodes = new ArrayList<>();
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiPhotonicNodes
            = this.tapiNodes.values().stream()
                .filter(n -> n.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
                .collect(Collectors.toList());
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node
            : tapiPhotonicNodes) {
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepM = new HashMap<>();
            for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> entry : node.getOwnedNodeEdgePoint().entrySet()) {
                if (entry.getValue().getName().values().stream()
                    .filter(name -> name.getValueName().startsWith("eNodeEdgePoint")).count() > 0) {
                    onepM.put(entry.getKey(), entry.getValue());
                }
            }
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node prunedNode
                = new NodeBuilder(node).setOwnedNodeEdgePoint(onepM).build();
            prunedTapiPhotonicNodes.add(prunedNode);
        }
        return prunedTapiPhotonicNodes;
    }

    private Map<String, String> convertListNodeWithListNepToMapForUuidAndName(
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodes) {
        Map<String, String> uuidNameMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node : nodes) {
            for (OwnedNodeEdgePoint nep : node.nonnullOwnedNodeEdgePoint().values()) {
                String nodeUuid = node.getUuid().getValue();
                String nepUuid = nep.getUuid().getValue();
                String nodeName = node.getName().get(new NameKey("otsi node name")).getValue();
                String nepName = nep.getName().get(new NameKey(nep.getName().keySet().stream().findFirst()
                        .orElseThrow()))
                    .getValue();
                uuidNameMap.put(String.join("--", nodeUuid, nepUuid), String.join("--", nodeName, nepName));
            }
        }
        return uuidNameMap;
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
            .build();
    }

    private Uuid getNodeUuid4Dsr(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
                                 Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList, Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR));
        // client nep creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                oorClientPortList.get(i).getTpId().getValue()), nepUuid);
            NameBuilder nameBldr = new NameBuilder().setValue(String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                oorClientPortList.get(i).getTpId().getValue()));
            Name name;
            if (OpenroadmNodeType.TPDR.equals(this.ietfNodeType)) {
                name = nameBldr.setValueName("100G-tpdr").build();
            } else {
                name = nameBldr.setValueName("NodeEdgePoint_C").build();
            }

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(name.key(), name),
                LayerProtocolName.ETH, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId,
                    TapiStringConstants.DSR));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid);
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
     // eNep creation on otsi node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
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
        // create NodeRuleGroup
        int count = 1;
        for (NonBlockingList nbl : this.oorOduSwitchingPool.nonnullNonBlockingList().values()) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
                nepList = new HashMap<>();
            for (TpId tp : nbl.getTpList()) {
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                        tp.getValue())) || this.uuidMap.containsKey(String.join(
                    "+", this.ietfNodeId, TapiStringConstants.I_ODU, tp.getValue()))) {
                    String qual = tp.getValue().contains("CLIENT") ? TapiStringConstants.DSR
                        : TapiStringConstants.I_ODU;
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                        nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group
                        .NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            TapiStringConstants.XPDR)))
                        .setNodeEdgePointUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            qual, tp.getValue())))
                        .build();
                    nepList.put(nep.key(), nep);
                }
            }
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(
                    UUID.nameUUIDFromBytes(("dsr node rule group " + count).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .build();
            nodeRuleGroupList.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeUuid;
    }

    private OwnedNodeEdgePoint createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
                                         LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol,
                                         boolean withSip, String keyword) {
        String key = String.join("+", keyword, oorTp.getTpId().getValue());
        AdministrativeState adminState = (oorTp.augmentation(TerminationPoint1.class).getAdministrativeState() != null)
            ? this.tapiLink.setTapiAdminState(oorTp.augmentation(TerminationPoint1.class).getAdministrativeState()
                .getName())
            : null;
        OperationalState operState = (oorTp.augmentation(TerminationPoint1.class).getOperationalState() != null)
            ? this.tapiLink.setTapiOperationalState(oorTp.augmentation(TerminationPoint1.class).getOperationalState()
                .getName())
            : null;
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(key))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames)
            .setSupportedCepLayerProtocolQualifierInstances(createSupportedCepLayerProtocolQualifier(oorTp,
                nodeProtocol))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(createSIP(1, oorTp, keyword));
        }
        return onepBldr.build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createNepForRdmNode(int nbNep) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (int i = 1; i <= nbNep; i++) {
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "roadm node", "nep", String.valueOf(i)))
                .getBytes(Charset.forName("UTF-8"))).toString());
            Name nepName = new NameBuilder()
                .setValueName("NodeEdgePoint name")
                .setValue(new StringBuilder("NodeEdgePoint_").append(i).toString())
                .build();
            List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build());
            OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
                .setUuid(nepUuid)
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName.key(), nepName))
                .setSupportedCepLayerProtocolQualifierInstances(sclpqiList)
                .setDirection(Direction.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(AdministrativeState.UNLOCKED).setOperationalState(OperationalState.ENABLED)
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
        Set<RuleType> ruleTypes = new HashSet<>();
        ruleTypes.add(RuleType.FORWARDING);
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(ruleTypes)
            .build();
        ruleList.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(("rdm infra node rule group").getBytes(Charset.forName("UTF-8")))
                .toString()))
            .setRule(ruleList)
            .setNodeEdgePoint(nepMap)
            .build();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        return nodeRuleGroupMap;
    }

    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createSIP(int nb, TerminationPoint tp,
                                                                                       String nodeId) {
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            //TODO : verify why i is not included to proceed Uuid
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", nodeId,
                    tp.getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString()))
                .build();
            msipl.put(msip.key(), msip);
        }
        return msipl;
    }

    private List<SupportedCepLayerProtocolQualifierInstances> createSupportedCepLayerProtocolQualifier(
            TerminationPoint tp, LayerProtocolName lpn) {
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
                //TODO: verify that in the new scope it makes sense to include ODU in the abstracted topology
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

    private void createTapiOtsLinks(Map<String, String> photonicNepUuisMap, Map<String, String> rdmInfraNepUuisMap) {
        Iterator<Entry<String, String>> it1 = photonicNepUuisMap.entrySet().iterator();
        Iterator<Entry<String, String>> it2 = rdmInfraNepUuisMap.entrySet().iterator();
        while (it1.hasNext()) {
            Map<NodeEdgePointKey, NodeEdgePoint> nepMap = new HashMap<>();
            Map.Entry<String, String> photonicEntry = it1.next();
            Map.Entry<String, String> rdmEntry = it2.next();
            Uuid sourceUuidTp = new Uuid(photonicEntry.getKey().split("--")[1]);
            Uuid sourceUuidNode = new Uuid(photonicEntry.getKey().split("--")[0]);
            Uuid destUuidTp = new Uuid(rdmEntry.getKey().split("--")[1]);
            Uuid destUuidNode = new Uuid(rdmEntry.getKey().split("--")[0]);
            NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(sourceUuidNode)
                .setNodeEdgePointUuid(sourceUuidTp)
                .build();
            nepMap.put(sourceNep.key(), sourceNep);
            NodeEdgePoint destNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(destUuidNode)
                .setNodeEdgePointUuid(destUuidTp)
                .build();
            nepMap.put(destNep.key(), destNep);
            Name linkName = new NameBuilder().setValueName("OTS link name")
                .setValue(String.join(" and ", photonicEntry.getValue(), rdmEntry.getValue()))
                .build();
            Link otsLink = new LinkBuilder()
                .setUuid(new Uuid(
                    UUID.nameUUIDFromBytes((String.join(" and ", photonicEntry.getValue(), rdmEntry.getValue()))
                            .getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setName(Map.of(linkName.key(), linkName))
                .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA))
                .setNodeEdgePoint(nepMap)
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .build();
            this.tapiLinks.put(otsLink.key(), otsLink);
        }
    }

    public Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            getTapiNodes() {
        return tapiNodes;
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }
}