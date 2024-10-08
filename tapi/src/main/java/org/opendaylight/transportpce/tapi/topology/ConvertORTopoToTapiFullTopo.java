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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIERMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
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
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertORTopoToTapiFullTopo {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToTapiFullTopo.class);
    private String ietfNodeId;
    private OpenroadmNodeType ietfNodeType;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private final TapiLink tapiLink;
    private static String topologicalMode = TapiProvider.TOPOLOGICAL_MODE;


    public ConvertORTopoToTapiFullTopo(Uuid tapiTopoUuid, TapiLink tapiLink) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.tapiSips = new HashMap<>();
        this.tapiLink = tapiLink;
    }

    public void convertRdmToRdmLinks(
            List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> rdmTordmLinkList) {
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} roadm to roadm links", rdmTordmLinkList.size() / 2);
        for (var link : rdmTordmLinkList) {
            if (linksToNotConvert.contains(link.getLinkId().getValue())) {
                continue;
            }
            var lnk1 = link.augmentation(Link1.class);
            var lnk1OppLnk = lnk1.getOppositeLink();
            var oppositeLink = rdmTordmLinkList.stream()
                .filter(l -> l.getLinkId().equals(lnk1OppLnk))
                .findAny().orElse(null);
            AdminStates oppLnkAdmState = null;
            State oppLnkOpState = null;
            if (oppositeLink != null) {
                oppLnkAdmState = oppositeLink.augmentation(Link1.class).getAdministrativeState();
                oppLnkOpState = oppositeLink.augmentation(Link1.class).getOperationalState();
            }
            var linkSrc = link.getSource();
            String linkSrcNodeValue = linkSrc.getSourceNode().getValue();
            var linkDst = link.getDestination();
            String linkDstNodeValue = linkDst.getDestNode().getValue();
            var lnkAdmState = lnk1.getAdministrativeState();
            var lnkOpState = lnk1.getOperationalState();
            Link tapLink = this.tapiLink.createTapiLink(
                String.join("-", linkSrcNodeValue.split("-")[0], linkSrcNodeValue.split("-")[1]),
                linkSrc.getSourceTp().getValue(),
                String.join("-", linkDstNodeValue.split("-")[0], linkDstNodeValue.split("-")[1]),
                linkDst.getDestTp().getValue(),
                TapiStringConstants.OMS_RDM_RDM_LINK,
                TapiStringConstants.PHTNC_MEDIA,
                TapiStringConstants.PHTNC_MEDIA,
                TapiStringConstants.PHTNC_MEDIA_OTS,
                TapiStringConstants.PHTNC_MEDIA_OTS,
                //adminState,
                lnkAdmState == null || oppLnkAdmState == null
                    ? null : this.tapiLink.setTapiAdminState(lnkAdmState, oppLnkAdmState).getName(),
                //operState,
                lnkOpState == null || oppLnkOpState == null
                    ? null : this.tapiLink.setTapiOperationalState(lnkOpState, oppLnkOpState).getName(),
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                this.tapiTopoUuid);
            linksToNotConvert.add(lnk1OppLnk.getValue());
            tapiLinks.put(tapLink.key(), tapLink);
            Map<Map<String, String>, ConnectionEndPoint> cepMap = this.tapiLink.getCepMap();
            LOG.debug("CONVERTTOFULL147, cepMap is {}", cepMap);
            addCepToOnepAndNode(cepMap);
        }
    }

    public void convertRoadmNode(Node roadm, Network openroadmTopo, String topoMode) {
        if (roadm != null && roadm.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE")) {
            return;
        }
        setTopologicalMode(topoMode);
        if (topoMode.equals("Full")) {
            convertRoadmNodeFull(roadm, openroadmTopo);
        } else {
            convertRoadmNodeAbstracted(openroadmTopo);
        }
    }

    private void addCepToOnepAndNode(Map<Map<String, String>, ConnectionEndPoint> cepMap) {

        for (Map.Entry<Map<String, String>, ConnectionEndPoint> cepEntry : cepMap.entrySet()) {
            String nepNodeId = cepEntry.getKey().entrySet().stream().findFirst().orElseThrow().getValue();
            LOG.debug("CONVERTTOFULL165, Node UUID is {}", nepNodeId);
            List<NodeKey> listKey = tapiNodes.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
            //.forEach(Function.identity().toString());
            LOG.debug("CONVERTTOFULL168, TapiNode Keys are {}", tapiNodes
                .entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));
            LOG.debug("CONVERTTOFULL172, TapiNode Keys are {}", tapiNodes
                .entrySet().stream()
                .map(nep -> nep.getValue().getName().toString())
                .collect(Collectors.toList()));
            if (!listKey.toString().contains(nepNodeId)) {
                LOG.info("ConvertToFullLINE178, ListKey {} of TapiNodes does not contain NodeUuid {}",
                    listKey, nepNodeId);
            }
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node = tapiNodes
                .entrySet().stream()
                .filter(theNode -> theNode.getKey().getUuid().toString().equals(nepNodeId))
                .map(Map.Entry::getValue).findFirst().orElseThrow();
            var onepMap = node.getOwnedNodeEdgePoint();
            OwnedNodeEdgePoint ownedNep = onepMap.entrySet().stream()
                .filter(onep -> onep.getKey().getUuid().toString()
                    .equals(cepEntry.getKey().entrySet().stream().findFirst().orElseThrow().getKey()))
                .map(Map.Entry::getValue).findFirst().orElseThrow();
            CepList cepList = new CepListBuilder()
                .setConnectionEndPoint(Map.of(cepEntry.getValue().key(), cepEntry.getValue()))
                .build();
            OwnedNodeEdgePoint1 onep1Bldr = new OwnedNodeEdgePoint1Builder().setCepList(cepList).build();
            OwnedNodeEdgePoint newOnep = new OwnedNodeEdgePointBuilder(ownedNep)
                    .addAugmentation(onep1Bldr)
                    .build();
            onepMap.put(newOnep.key(), newOnep);
            var newNode = new  org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology
                    .NodeBuilder(node)
                .setOwnedNodeEdgePoint(onepMap)
                .build();
            this.tapiNodes.put(newNode.key(), newNode);
            LOG.debug("CONVERTTOFULL201, successfully create node {} with CepList {} ", newNode.getName(), cepList);
        }
    }

    private void convertRoadmNodeFull(Node roadm, Network openroadmTopo) {
        this.ietfNodeId = roadm.getNodeId().getValue();
        this.ietfNodeType = roadm.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
            .getNodeType();
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
            String nodeId = node.getNodeId().getValue();
            if (node.getSupportingNode().values().stream()
                    .noneMatch(sp -> sp.getNodeRef().getValue().equals(this.ietfNodeId))) {
                LOG.debug("Abstracted node {} is not part of {}", nodeId, this.ietfNodeId);
                continue;
            }
            var node1 = node.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class);
            if (node.augmentation(Node1.class) == null && node1 == null) {
                LOG.warn("Abstracted node {} doesnt have type of node or is not disaggregated", nodeId);
                continue;
            }
            OpenroadmNodeType nodeType = node.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                .getNodeType();
            var node1TpValues = node1.getTerminationPoint().values();
            LOG.info("TPs of node: {}", node1TpValues);
            switch (nodeType.getIntValue()) {
                case 11:
                    LOG.info("Degree node");
                    // Get only external TPs of the degree
                    List<TerminationPoint> degPortList = node1TpValues.stream()
                        .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                                == OpenroadmTpType.DEGREETXRXTTP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                                == OpenroadmTpType.DEGREERXTTP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                                == OpenroadmTpType.DEGREETXTTP.getIntValue())
                        .collect(Collectors.toList());
                    // Convert TP List in NEPs and put it in onepl
                    LOG.info("Degree port List: {}", degPortList.toString());
                    // TODO: deg port could be sip. e.g. MDONS
                    oneplist.putAll(populateNepsForRdmNode(
                        nodeId, degPortList, false, TapiStringConstants.PHTNC_MEDIA_OTS));
                    oneplist.putAll(populateNepsForRdmNode(
                        nodeId, degPortList, false, TapiStringConstants.PHTNC_MEDIA_OMS));
                    numNeps += degPortList.size() * 2;
                    break;
                case 12:
                    LOG.info("SRG node");
                    // Get only external TPs of the srg
                    List<TerminationPoint> srgPortList = node1TpValues.stream()
                        .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                                == OpenroadmTpType.SRGTXRXPP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                                == OpenroadmTpType.SRGRXPP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                                == OpenroadmTpType.SRGTXPP.getIntValue())
                        .collect(Collectors.toList());
                    // Convert TP List in NEPs and put it in onepl
                    LOG.info("Srg port List: {}", srgPortList);
                    oneplist.putAll(populateNepsForRdmNode(
                        nodeId, srgPortList, true, TapiStringConstants.PHTNC_MEDIA_OTS));

                    numNeps += srgPortList.size();
                    numSips += srgPortList.size();
                    break;
                default:
                    LOG.error("Node {} type not supported", nodeType.getName());
            }
        }
        // create tapi Node
        // UUID
        String nodeIdPhMed = String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeIdPhMed.getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info("Creation of PHOTONIC node for {}, of Uuid {}", this.ietfNodeId, nodeUuid);
        // Names
        Name nodeNames =  new NameBuilder().setValueName("roadm node name").setValue(nodeIdPhMed).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type").setValue(this.ietfNodeType.getName()).build();
        // Protocol Layer
        Set<LayerProtocolName> layerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        // Build tapi node
        LOG.debug("CONVERTTOFULL SRG OTSNode of retrieved OnepMap {} ",
            oneplist.entrySet().stream().filter(e -> e.getValue()
                .getSupportedCepLayerProtocolQualifierInstances()
                    .contains(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                        .setNumberOfCepInstances(Uint64.valueOf(1))
                        .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                    .build()))
            .collect(Collectors.toList()).toString());
        //org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
        var roadmNode = createRoadmTapiNode(nodeUuid,
            Map.of(nodeNames.key(), nodeNames, nameNodeType.key(), nameNodeType), layerProtocols, oneplist, "Full");
        // TODO add states corresponding to device config
        LOG.info("ROADM node {} should have {} NEPs and {} SIPs", TapiStringConstants.RDM_INFRA, numNeps, numSips);
        LOG.info("ROADM node {} has {} NEPs and {} SIPs",
            TapiStringConstants.RDM_INFRA,
            roadmNode.nonnullOwnedNodeEdgePoint().values().size(),
            roadmNode.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null)
                .count());
        tapiNodes.put(roadmNode.key(), roadmNode);
    }

    private void convertRoadmNodeAbstracted(Network openroadmTopo) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneMap = new HashMap<>();
        // 1. Get degree and srg nodes to map TPs into NEPs
        if (openroadmTopo.getNode() == null) {
            LOG.warn("Openroadm-topology is null.");
            return;
        }
        int numNeps = 0;
        int numSips = 0;
        List<Node> nodeList = new ArrayList<Node>(openroadmTopo.getNode().values());
        for (Node node:nodeList) {
            if (node.augmentation(Node1.class) == null
                    && node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.network.topology.rev180226.Node1.class) == null) {
                LOG.warn("Abstracted node {} doesnt have type of node or is not disaggregated",
                    node.getNodeId().getValue());
                continue;
            }
            OpenroadmNodeType nodeType = node.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.common.network.rev230526.Node1.class).getNodeType();
            if (nodeType.getIntValue() != 11) {
                // Only consider ROADMS SRG Nodes
                continue;
            }
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
                node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.Node1.class);
            LOG.debug("Handling SRG node in Topology abstraction {}", node.getNodeId().toString());
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
            LOG.debug("Srg port List: {}", srgPortList);
            oneMap.putAll(populateNepsForRdmNode(node.getNodeId().getValue(), srgPortList, true,
                TapiStringConstants.PHTNC_MEDIA_OTS));
            numNeps += srgPortList.size();
            numSips += srgPortList.size();
        }
        // create a unique ROADM tapi Node
        LOG.info("abstraction of the ROADM infrastructure towards a photonic node");
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(
            TapiStringConstants.RDM_INFRA.getBytes(Charset.forName("UTF-8"))).toString());
        Name nodeName =  new NameBuilder().setValueName("roadm node name").setValue(TapiStringConstants.RDM_INFRA)
            .build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(OpenroadmNodeType.ROADM.getName()).build();

        // Protocol Layer
        Set<LayerProtocolName> layerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        // Build tapi node
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node roadmNode =
            createRoadmTapiNode(nodeUuid, Map.of(nodeName.key(), nodeName, nameNodeType.key(), nameNodeType),
            layerProtocols, oneMap, "Abstracted");
        // TODO add states corresponding to device config
        LOG.info("ROADM node {} should have {} NEPs and {} SIPs", TapiStringConstants.RDM_INFRA, numNeps, numSips);
        LOG.info("ROADM node {} has {} NEPs and {} SIPs", TapiStringConstants.RDM_INFRA,
            roadmNode.nonnullOwnedNodeEdgePoint().values().size(),
            roadmNode.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());

        tapiNodes.put(roadmNode.key(), roadmNode);
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
             createRoadmTapiNode(Uuid nodeUuid, Map<NameKey, Name> nameMap, Set<LayerProtocolName> layerProtocols,
             Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap, String topoMode) {
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

        var tapiFactory = new ConvertORToTapiTopology(this.tapiTopoUuid);
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap
            = tapiFactory.createAllNodeRuleGroupForRdmNode(
                topoMode.equals("Full")
                    ? "Full"
                    : "Abstracted",
                nodeUuid, this.ietfNodeId, onepMap.values());
        Map<InterRuleGroupKey, InterRuleGroup> interRuleGroupMap
            = tapiFactory.createInterRuleGroupForRdmNode(
                topoMode.equals("Full")
                    ? "Full"
                    : "Abstracted",
                nodeUuid, this.ietfNodeId,
                nodeRuleGroupMap.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList()));
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nameMap)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepMap)
            .setNodeRuleGroup(nodeRuleGroupMap)
            .setInterRuleGroup(interRuleGroupMap)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(
                new RiskParameterPacBuilder()
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .build())
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    public Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            String nodeId, List<TerminationPoint> tpList, boolean withSip, String nepPhotonicSublayer) {
        // create neps for MC and and Photonic Media OTS/OMS
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (TerminationPoint tp:tpList) {
            String tpId = tp.getTpId().getValue();
            // Admin and oper state common for all tps
            OpenroadmTpType tpType = tp.augmentation(TerminationPoint1.class).getTpType();
            // PHOTONIC MEDIA nep
            LOG.debug("PHOTO NEP = {}", String.join("+", this.ietfNodeId, nepPhotonicSublayer, tpId));
            SupportedCepLayerProtocolQualifierInstancesBuilder sclpqiBd =
                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                    .setNumberOfCepInstances(Uint64.valueOf(1));
            switch (nepPhotonicSublayer) {
                case TapiStringConstants.PHTNC_MEDIA_OMS:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROMS.VALUE);
                    break;
                case TapiStringConstants.PHTNC_MEDIA_OTS:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE);
                    break;
                case TapiStringConstants.MC:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIERMC.VALUE);
                    break;
                case TapiStringConstants.OTSI_MC:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE);
                    break;
                default:
                    break;
            }
            //List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>(List.of(sclpqiBd.build()));

//          OwnedNodeEdgePointBuilder onepBd = new OwnedNodeEdgePointBuilder();


            AdminStates admin = tp.augmentation(TerminationPoint1.class).getAdministrativeState();
            State oper = tp.augmentation(TerminationPoint1.class).getOperationalState();
            Name nepName = new NameBuilder()
                .setValueName(nepPhotonicSublayer + "NodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, nepPhotonicSublayer, tpId))
                .build();
            OwnedNodeEdgePointBuilder onepBdd = new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId, nepPhotonicSublayer, tpId))
                    .getBytes(Charset.forName("UTF-8"))).toString()))
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName.key(), nepName))
                .setSupportedCepLayerProtocolQualifierInstances(
                    new ArrayList<>(List.of(
                        new SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(
                                TapiStringConstants.PHTNC_MEDIA_OMS.equals(nepPhotonicSublayer)
                                    ? PHOTONICLAYERQUALIFIEROMS.VALUE
                                    : PHOTONICLAYERQUALIFIEROTS.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(1))
                            .build())))
                .setDirection(Direction.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(this.tapiLink.setTapiAdminState(admin.getName()))
                .setOperationalState(this.tapiLink.setTapiOperationalState(oper.getName()))
                .setLifecycleState(LifecycleState.INSTALLED);

            if (!nepPhotonicSublayer.equals(TapiStringConstants.MC)
                    && !nepPhotonicSublayer.equals(TapiStringConstants.OTSI_MC)) {
                ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(this.tapiTopoUuid);
                Map<Double,Double> usedFreqMap = new HashMap<>();
                Map<Double,Double> availableFreqMap = new HashMap<>();
                switch (tpType) {
                    // Whatever is the TP and its type we consider that it is handled in a bidirectional way :
                    // same wavelength(s) used in both direction.
                    case SRGRXPP:
                    case SRGTXPP:
                    case SRGTXRXPP:
                        usedFreqMap = tapiFactory.getPPUsedWavelength(tp);
                        if (usedFreqMap == null || usedFreqMap.isEmpty()) {
                            availableFreqMap.put(GridConstant.START_EDGE_FREQUENCY * 1E12,
                                GridConstant.START_EDGE_FREQUENCY * 1E12
                                + GridConstant.GRANULARITY * GridConstant.EFFECTIVE_BITS * 1E09);
                        } else {
                            LOG.debug("EnteringLOOPcreateOTSiMC & MC with usedFreqMap non empty {} NEP {} for Node {}",
                                usedFreqMap, String.join("+", this.ietfNodeId, nepPhotonicSublayer, tpId), nodeId);
                            onepMap.putAll(populateNepsForRdmNode(
                                nodeId, new ArrayList<>(List.of(tp)), true, TapiStringConstants.MC));
                            onepMap.putAll(populateNepsForRdmNode(
                                nodeId, new ArrayList<>(List.of(tp)), true, TapiStringConstants.OTSI_MC));
                        }
                        break;
                    case DEGREERXTTP:
                    case DEGREETXTTP:
                    case DEGREETXRXTTP:
                        usedFreqMap = tapiFactory.getTTPUsedFreqMap(tp);
                        availableFreqMap = tapiFactory.getTTPAvailableFreqMap(tp);
                        break;
                    default:
                        break;
                }
                LOG.debug("calling add Photonic NEP spec for Roadm");
                onepBdd = tapiFactory.addPhotSpecToRoadmOnep(
                    nodeId, usedFreqMap, availableFreqMap, onepBdd, String.join("+", nodeId, nepPhotonicSublayer));
            }

            OwnedNodeEdgePoint onep = onepBdd.build();
            LOG.debug("ConvertORToTapiTopology.populateNepsForRdmNode onep is {}", onep);
            onepMap.put(onep.key(), onep);
        }
        return onepMap;
    }

    public void convertXpdrToRdmLinks(
            List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> xpdrRdmLinkList) {
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} xpdr to roadm links", xpdrRdmLinkList.size() / 2);
        LOG.debug("Link list = {}", xpdrRdmLinkList);
        for (var link:xpdrRdmLinkList) {
            if (linksToNotConvert.contains(link.getLinkId().getValue())) {
                continue;
            }
            var oppositeLink = xpdrRdmLinkList.stream()
                .filter(l -> l.getLinkId().equals(link.augmentation(Link1.class).getOppositeLink()))
                .findAny().orElse(null);
            AdminStates oppLnkAdmState = null;
            State oppLnkOpState = null;
            if (oppositeLink != null) {
                oppLnkAdmState = oppositeLink.augmentation(Link1.class).getAdministrativeState();
                oppLnkOpState = oppositeLink.augmentation(Link1.class).getOperationalState();
            }
            String sourceNode =
                link.getSource().getSourceNode().getValue().contains("ROADM")
                    ? getIdBasedOnModelVersion(link.getSource().getSourceNode().getValue())
                    : link.getSource().getSourceNode().getValue();
            String destNode =
                link.getDestination().getDestNode().getValue().contains("ROADM")
                    ? getIdBasedOnModelVersion(link.getDestination().getDestNode().getValue())
                    : link.getDestination().getDestNode().getValue();
            Link tapLink = this.tapiLink.createTapiLink(
                sourceNode, link.getSource().getSourceTp().getValue(),
                destNode, link.getDestination().getDestTp().getValue(),
                TapiStringConstants.OMS_XPDR_RDM_LINK,
                sourceNode.contains("ROADM") ? TapiStringConstants.PHTNC_MEDIA : TapiStringConstants.XPDR,
                destNode.contains("ROADM") ? TapiStringConstants.PHTNC_MEDIA : TapiStringConstants.XPDR,
                TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS,
                //adminState,
                link.augmentation(Link1.class).getAdministrativeState() == null || oppLnkAdmState == null
                    ? null
                    : this.tapiLink.setTapiAdminState(
                        link.augmentation(Link1.class).getAdministrativeState(), oppLnkAdmState).getName(),
                //operState,
                link.augmentation(Link1.class).getOperationalState() == null || oppLnkOpState == null
                    ? null
                    : this.tapiLink.setTapiOperationalState(
                        link.augmentation(Link1.class).getOperationalState(), oppLnkOpState).getName(),
                Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                this.tapiTopoUuid);
            linksToNotConvert.add(link.augmentation(Link1.class).getOppositeLink().getValue());
            this.tapiLinks.put(tapLink.key(), tapLink);
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

    public void setTapiSips(Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSip) {
        this.tapiSips.putAll(tapiSip);
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }

    public static void setTopologicalMode(String topoMode) {
        ConvertORTopoToTapiFullTopo.topologicalMode = topoMode;
    }

    public String getTopologicalMode() {
        return topologicalMode;
    }
}
