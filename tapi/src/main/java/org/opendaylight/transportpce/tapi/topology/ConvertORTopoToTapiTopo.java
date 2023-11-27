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
import java.util.Collections;
import java.util.HashMap;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
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
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertORTopoToTapiTopo {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToTapiTopo.class);
    private String ietfNodeId;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private final TapiLink tapiLink;


    public ConvertORTopoToTapiTopo(Uuid tapiTopoUuid, TapiLink tapiLink) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.tapiSips = new HashMap<>();
        this.tapiLink = tapiLink;
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
            = new ConvertORToTapiTopology(this.tapiTopoUuid)
                .createNodeRuleGroupForRdmNode("Abstracted", nodeUuid, this.ietfNodeId, onepMap.values());
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
                    .filter(name -> name.getValueName().equals("eNodeEdgePoint")).count() > 0) {
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

    public void setTapiNodes(Map<NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeMap) {
        this.tapiNodes.putAll(nodeMap);
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

    public void setTapiSips(Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSip) {
        this.tapiSips.putAll(tapiSip);
    }
}