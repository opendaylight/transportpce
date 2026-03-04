/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.topology.nep.RoadmNepFactory;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev250110.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
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

/**
 * This Class manages  the conversion of the OpenROADM topology in the Data Store to a T-API Topology also stored
 * in the Data Store. From the level of abstraction will depend DS used resources.
 * The level of abstraction depends on the topologicalMode
 */
public class ConvertTopoORtoTapiAtInit {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertTopoORtoTapiAtInit.class);
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private final TapiLink tapiLink;
    private static String topologicalMode = TapiProvider.TOPOLOGICAL_MODE;
    private final RoadmNepFactory roadmNepFactory;

    /**
     * Instantiate an ConvertORToDSTapiTopo Object.
     * @param tapiTopoUuid Uuid of the generated topology which corresponds to either an Abstracted or a Full view
     *                     of the OpenROAM converted topology.
     * @param tapiLink Instance of TapiLink leveraging its methods.
     */
    public ConvertTopoORtoTapiAtInit(Uuid tapiTopoUuid, TapiLink tapiLink, RoadmNepFactory roadmNepFactory) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.tapiSips = new HashMap<>();
        this.tapiLink = tapiLink;
        this.roadmNepFactory = roadmNepFactory;
    }

    /**
     * Populate tapiLinks from a list of ietf/OpenROADM links provided as the input of the method.
     * @param rdmTordmLinkList List of ietf/openroadm links provided as an input.
     */
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
                TapiConstants.OMS_RDM_RDM_LINK,
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA_OTS,
                TapiConstants.PHTNC_MEDIA_OTS,
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

    /**
     * Selects the right method to convert OpenROADM topology according to the topological mode (Abstracted/Full).
     * @param roadm A list of ietf/openroadm Nodes provided as an input.
     * @param openroadmTopo A list of ietf/openroadm network provided as an input.
     * @param topoMode Topological mode which corresponds to the desired level of abstraction.
     */
    public void convertRoadmNode(Node roadm, Network openroadmTopo, String topoMode) {
        if (roadm != null && roadm.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE")) {
            return;
        }
        if (topoMode != null) {
            setTopologicalMode(topoMode);
        }
        if (roadm == null) {
            LOG.warn("Null roadm");
            return;
        }
        if (ConvertTopoORtoTapiAtInit.topologicalMode.equals("Full")) {
            convertRoadmNodeFull(roadm, openroadmTopo);
        } else {
            convertRoadmNodeAbstracted(openroadmTopo, roadm.getNodeId().getValue());
        }
    }

    /**
     * Enriches Nep description of tapiNodes populating their associated cep-list with Cep.
     * @param cepMap Map of Connection-End-Points with their Keys.
     */
    private void addCepToOnepAndNode(Map<Map<String, String>, ConnectionEndPoint> cepMap) {

        for (Map.Entry<Map<String, String>, ConnectionEndPoint> cepEntry : cepMap.entrySet()) {
            String nepNodeId = cepEntry.getKey().entrySet().stream().findFirst().orElseThrow().getValue();
            LOG.debug("CONVERTTOFULL165, Node UUID is {}", nepNodeId);
            List<NodeKey> listKey = tapiNodes.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
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

    /**
     * Converts OpenROADM infrastructure of the OpenROADM topology to its equivalent Tapi Topology stored in Data Store.
     * Associated topology name T0_FULL_MULTILAYER.
     * @param roadm A list of OpenROADM nodes,
     * @param openroadmTopo A list of networks topologies.
     */
    private void convertRoadmNodeFull(Node roadm, Network openroadmTopo) {
        LOG.info("Converting ROADMs by doing a full conversion");
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneplist = new HashMap<>();
        // 1. Get degree and srg nodes to map TPs into NEPs
        if (openroadmTopo.getNode() == null) {
            LOG.warn("Openroadm-topology is null.");
            return;
        }
        int numNeps = 0;
        int numSips = 0;
        List<Node> nodeList = new ArrayList<Node>(openroadmTopo.getNode().values());
        LOG.info("Converting {} nodes on {}", nodeList.size(), roadm.getNodeId().getValue());
        String ietfNodeId = roadm.getNodeId().getValue();
        for (Node node:nodeList) {
            String nodeId = node.getNodeId().getValue();
            if (node.getSupportingNode().values().stream()
                    .noneMatch(sp -> sp.getNodeRef().getValue().equals(ietfNodeId))) {
                LOG.debug("Abstracted node {} is not part of {}", nodeId, ietfNodeId);
                continue;
            }
            var node1 = node.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class);
            if (node.augmentation(Node1.class) == null && node1 == null) {
                LOG.warn("Abstracted node {} doesnt have type of node or is not disaggregated", nodeId);
                continue;
            }
            OpenroadmNodeType nodeType = node.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Node1.class)
                .getNodeType();
            var node1TpValues = node1.getTerminationPoint().values();
            logTerminationsPointIds(node1TpValues);
            String sietfNodeId = Optional.ofNullable(node.getSupportingNode())
                    .orElse(Map.of())
                    .values()
                    .stream()
                    .filter(n -> n.key().getNetworkRef().getValue().equals("openroadm-network"))
                    .map(r -> r.getNodeRef().getValue())
                    .findFirst()
                    .orElseThrow();
            switch (nodeType.getIntValue()) {
                case 11:
                    LOG.debug("Supported node {} is a Degree", nodeId);
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
                    LOG.debug("Degree port List: {}", degPortList.toString());
                    // TODO: deg port could be sip. e.g. MDONS
                    oneplist.putAll(
                         populateNepsForRdmNode(false, sietfNodeId, degPortList, true, TapiConstants.PHTNC_MEDIA_OTS));
                    oneplist.putAll(
                        populateNepsForRdmNode(false, sietfNodeId, degPortList, false, TapiConstants.PHTNC_MEDIA_OMS));
                    numNeps += degPortList.size() * 2;
                    break;
                case 12:
                    LOG.debug("Supported node {} is a SRG", nodeId);
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
                    LOG.info("Srg port List: {}", srgPortList.stream().map(srg ->
                            srg.getTpId().getValue()).collect(Collectors.toSet()));
                    oneplist.putAll(
                        populateNepsForRdmNode(true, sietfNodeId, srgPortList, true, TapiConstants.PHTNC_MEDIA_OTS));

                    numNeps += srgPortList.size();
                    numSips += srgPortList.size();
                    break;
                default:
                    LOG.error("Node {} type not supported", nodeType.getName());
            }
        }
        // create tapi Node
        // UUID
        String nodeIdPhMed = String.join("+", ietfNodeId, TapiConstants.PHTNC_MEDIA);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeIdPhMed.getBytes(StandardCharsets.UTF_8)).toString());
        LOG.info("Creation of PHOTONIC node for {}, of Uuid {}", ietfNodeId, nodeUuid.getValue());
        // Names
        OpenroadmNodeType ietfNodeType = roadm.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Node1.class)
                .getNodeType();
        Name nodeNames =  new NameBuilder().setValueName("roadm node name").setValue(nodeIdPhMed).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type").setValue(ietfNodeType.getName()).build();
        // Protocol Layer
        Set<LayerProtocolName> layerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        // Build tapi node
        LOG.debug("CONVERTTOFULL SRG OTSNode of retrieved OnepMap {} ",
            oneplist.entrySet().stream().filter(e -> e.getValue()
                .getSupportedCepLayerProtocolQualifierInstances()
                    .contains(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                        .setNumberOfCepInstances(Uint64.ONE)
                        .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                    .build()))
            .collect(Collectors.toList()).toString());
        //org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
        var roadmNode = createRoadmTapiNode(
                nodeUuid,
                Map.of(nodeNames.key(), nodeNames, nameNodeType.key(), nameNodeType),
                layerProtocols,
                oneplist,
                "Full",
                ietfNodeId);
        // TODO add states corresponding to device config
        LOG.info("ROADM node {} should have {} NEPs and {} SIPs (CRNF)", TapiConstants.RDM_INFRA, numNeps, numSips);
        LOG.info("ROADM node {} has {} NEPs and {} SIPs (CRNF)",
            TapiConstants.RDM_INFRA,
            roadmNode.nonnullOwnedNodeEdgePoint().values().size(),
            roadmNode.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null)
                .count());
        tapiNodes.put(roadmNode.key(), roadmNode);
        LOG.info("{}: Full ROADM conversion complete.", ietfNodeId);
    }

    /**
     * Logs a brief summary of Termination Points on a node by extracting their TP IDs,
     * then logging the count and a comma-separated list of the IDs.
     *
     * @param terminationPoints collection of termination points to summarize
     */
    private void logTerminationsPointIds(Collection<TerminationPoint> terminationPoints) {
        Set<String> tpIds = tpIds(terminationPoints);
        LOG.info("TPs ({}) on node: {}", tpIds.size(), String.join(", ", tpIds));
    }

    /**
     * Extracts the string values of Termination Point IDs (TP IDs) from a collection of
     * {@link TerminationPoint}s.
     *
     * @param terminationPoints collection of termination points
     * @return set of TP ID string values
     */
    private Set<String> tpIds(Collection<TerminationPoint> terminationPoints) {
        return terminationPoints
                .stream()
                .map(tp -> tp
                        .getTpId()
                        .getValue())
                .collect(Collectors.toSet());
    }

    /**
     * Abstracts OpenROADM infrastructure to a single Photonic Tapi Node "ROADM_INFRA" in Tapi Topology stored in DS.
     * Associated topology name T0_MULTILAYER.
     * @param openroadmTopo A list of networks topologies.
     */
    private void convertRoadmNodeAbstracted(Network openroadmTopo, String ietfNodeId) {
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
                .org.openroadm.common.network.rev250110.Node1.class).getNodeType();
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
            String sietfNodeId = Optional.ofNullable(node.getSupportingNode())
                    .orElse(Map.of())
                    .values()
                    .stream()
                    .filter(n -> n.key().getNetworkRef().getValue().equals("openroadm-network"))
                    .map(r -> r.getNodeRef().getValue())
                    .findFirst()
                    .orElseThrow();
            LOG.debug("Node {} SRG port List: {}", sietfNodeId, srgPortList);
            oneMap.putAll(populateNepsForRdmNode(true, sietfNodeId, srgPortList, true, TapiConstants.PHTNC_MEDIA_OTS));
            numNeps += srgPortList.size();
            numSips += srgPortList.size();
        }
        // create a unique ROADM tapi Node
        LOG.info("abstraction of the ROADM infrastructure towards a photonic node");
        Uuid nodeUuid = new Uuid(
            UUID.nameUUIDFromBytes(TapiConstants.RDM_INFRA.getBytes(StandardCharsets.UTF_8)).toString());
        Name nodeName =  new NameBuilder().setValueName("roadm node name").setValue(TapiConstants.RDM_INFRA).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(OpenroadmNodeType.ROADM.getName()).build();

        // Protocol Layer
        Set<LayerProtocolName> layerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        // Build tapi node
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node roadmNode =
            createRoadmTapiNode(nodeUuid, Map.of(nodeName.key(), nodeName, nameNodeType.key(), nameNodeType),
            layerProtocols, oneMap, "Abstracted", ietfNodeId);
        // TODO add states corresponding to device config
        LOG.info("ROADM node {} should have {} NEPs and {} SIPs (CRNA)", TapiConstants.RDM_INFRA, numNeps, numSips);
        LOG.info("ROADM node {} has {} NEPs and {} SIPs (CRNA)", TapiConstants.RDM_INFRA,
            roadmNode.nonnullOwnedNodeEdgePoint().values().size(),
            roadmNode.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());

        tapiNodes.put(roadmNode.key(), roadmNode);
    }

    /**
     * Converts OpenROADM infrastructure of the OpenROADM topology to its equivalent Tapi Topology stored in Data Store.
     * Associated topology name T0_FULL_MULTILAYER.
     * @param nodeUuid Uuid of the node to be created,
     * @param nameMap Name Map of the node to be created,
     * @param layerProtocols Set of layer protocols supported by the node,
     * @param onepMap Map of Owned-Node-Edge-Point of the node,
     * @param topoMode Mode of creation for the topo,
     * @param tapiNodeBuilder Node Builder returned by the method.
     */
    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
             createRoadmTapiNode(Uuid nodeUuid, Map<NameKey, Name> nameMap, Set<LayerProtocolName> layerProtocols,
             Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap, String topoMode, String ietfNodeId) {
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();

        var tapiFactory = new ORtoTapiTopoConversionTools(this.tapiTopoUuid);
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap
            = tapiFactory.createAllNodeRuleGroupForRdmNode(
                topoMode.equals("Full")
                    ? "Full"
                    : "Abstracted",
                nodeUuid, ietfNodeId, onepMap.values());
        Map<NodeRuleGroupKey, String> nrgMap = new HashMap<>();
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrgMapEntry : nodeRuleGroupMap.entrySet()) {
            nrgMap.put(nrgMapEntry.getKey(), nrgMapEntry.getValue().getName().get(new NameKey("nrg name")).getValue());
        }
        Map<InterRuleGroupKey, InterRuleGroup> interRuleGroupMap
            = tapiFactory.createInterRuleGroupForRdmNode(
                topoMode.equals("Full")
                    ? "Full"
                    : "Abstracted",
                nodeUuid, ietfNodeId, nrgMap);
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

    /**
     * Provides a Map of Owned Node Edge Point supported by a ROADM node of the Tapi Topology.
     *
     * @see RoadmNepFactory#populateNepsForRdmNode(boolean, String, List, boolean, String, TapiLink)
     */
    public Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            boolean srg,
            String nodeId,
            List<TerminationPoint> tpList,
            boolean withSip,
            String nepPhotonicSublayer) {

        return roadmNepFactory.populateNepsForRdmNode(
                srg,
                nodeId,
                tpList,
                withSip,
                nepPhotonicSublayer,
                tapiLink
        );
    }

    /**
     * Adds to tapiLinkMap the links connecting Xponders to ROADM.
     * @param xpdrRdmLinkList A list of OpenROADM link connecting ROADMs to Xponders.
     */
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
                TapiConstants.OMS_XPDR_RDM_LINK,
                sourceNode.contains("ROADM") ? TapiConstants.PHTNC_MEDIA : TapiConstants.XPDR,
                destNode.contains("ROADM") ? TapiConstants.PHTNC_MEDIA : TapiConstants.XPDR,
                TapiConstants.PHTNC_MEDIA_OTS, TapiConstants.PHTNC_MEDIA_OTS,
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

    public String getIdBasedOnModelVersion(String linknodeid) {
        return linknodeid.substring(0, linknodeid.lastIndexOf("-"));
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
        ConvertTopoORtoTapiAtInit.topologicalMode = topoMode;
    }

    public String getTopologicalMode() {
        return topologicalMode;
    }

}
