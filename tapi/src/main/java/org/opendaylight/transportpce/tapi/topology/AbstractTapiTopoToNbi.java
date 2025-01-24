/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class manages the conversion of a TAPI topology in Data store to a T-API abstracted Topology.
 * The topology shall be be provided as the output of GetTopologyDetails RPC.
 * (Instantiated by GetTopologyDetailsImpl.createbAbsTopologyFromTapiTopo).
 */
public class AbstractTapiTopoToNbi {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTapiTopoToNbi.class);
    private Map<NodeKey, Node> tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private Uuid refTopoUuid;

    /**
     * Instantiate an AbstractTapiTopoToNbi Object.
     * As soon as it is instantiated, Nodes, links and SIPs shall be provided using setters.
     * All methods will used Nodes/Link/SIPs that have been populated externally through setters.
     * Thus the constituting elements of the topology do not depend on refTopoUuid which is used as
     * the target topoUuid.
     * @param reftopoUuid Reference Topology Uuid provided in the input of GetTopologyDetails used in Builders.
     */
    public AbstractTapiTopoToNbi(Uuid reftopoUuid) {
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.tapiSips = new HashMap<>();
        this.refTopoUuid = reftopoUuid;
    }

    /**
     * Abstracts the ROADM infrastructure of the topology into a single Node.
     * The node will have TapiStringConstants.RDM_INFRA Name.
     */
    public void convertRoadmInfrastructure() {
        LOG.info("abstraction of the ROADM infrastructure towards a photonic node");
        Uuid nodeUuid = new Uuid(
            UUID.nameUUIDFromBytes(TapiStringConstants.RDM_INFRA.getBytes(Charset.forName("UTF-8"))).toString());
        Name nodeName =
            new NameBuilder().setValueName("otsi node name").setValue(TapiStringConstants.RDM_INFRA).build();
        Name nameNodeType =
            new NameBuilder().setValueName("Node Type").setValue(OpenroadmNodeType.ROADM.getName()).build();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = pruneTapiRoadmNeps();
        var tapiFactory = new ORToTapiTopoConversionFactory(this.refTopoUuid);
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap =
            tapiFactory.createAllNodeRuleGroupForRdmNode("Abstracted", nodeUuid, null, onepMap.values());
        Map<NodeRuleGroupKey, String> nrgMap = new HashMap<>();
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrgMapEntry : nodeRuleGroupMap.entrySet()) {
            nrgMap.put(nrgMapEntry.getKey(), nrgMapEntry.getValue().getName().get(new NameKey("nrg name")).getValue());
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
        // build RDM infra node abstraction
        Node rdmNode = new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(Map.of(nodeName.key(), nodeName, nameNodeType.key(), nameNodeType))
            .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA))
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepMap)
            .setNodeRuleGroup(nodeRuleGroupMap)
            .setInterRuleGroup(
                tapiFactory.createInterRuleGroupForRdmNode(
                    "Abstracted", nodeUuid, null, nrgMap))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(new RiskParameterPacBuilder()
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .build())
            .build();
        purgeTapiNodes();
        tapiNodes.put(rdmNode.key(), rdmNode);
        purgeTapiLinks();
    }

    /**
     * Provides a map(onepKey, onep) that includes only NEPs which correspond to pure photonic Nodes.
     */
    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> pruneTapiRoadmNeps() {
        List<Node> tapiPhotonicNodes = this.tapiNodes.values().stream()
                .filter(n -> n.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                    && !n.getLayerProtocolName().contains(LayerProtocolName.DIGITALOTN)
                    && !n.getLayerProtocolName().contains(LayerProtocolName.DSR)
                    && !n.getLayerProtocolName().contains(LayerProtocolName.ODU))
                .collect(Collectors.toList());
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (Node node : tapiPhotonicNodes) {
            for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> entry : node.getOwnedNodeEdgePoint().entrySet()) {
                var valStream = entry.getValue().getName().values().stream();
                if ((valStream.filter(name -> name.getValueName().equals("PHOTONIC_MEDIA_OTSNodeEdgePoint")).count() > 0
                            && valStream.filter(name -> name.getValue().contains("DEG")).count() == 0)
                        || (valStream.filter(name -> name.getValueName().equals("OTSI_MCNodeEdgePoint")).count() > 0
                            && valStream.filter(name -> name.getValue().contains("DEG")).count() == 0)) {
                    onepMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return onepMap;
    }

    /**
     * Removes from TapiLinks List all links that do not correspond to XPONDER-to-ROADM links.
     * This is done filtering links on NameKeys
     */
    private void purgeTapiLinks() {
        this.tapiLinks = this.tapiLinks.values().stream()
            .filter(l -> l.getName().containsKey(new NameKey(TapiStringConstants.VALUE_NAME_OTS_XPDR_RDM_LINK))
                || l.getName().containsKey(new NameKey(TapiStringConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK)))
            .collect(Collectors.toMap(Link::key, link -> link));
    }

    /**
     * Removes from TapiNodes List all nodes that corresponds ROADM Nodes, filtering nodes on NameKeys.
     */
    private void purgeTapiNodes() {
        this.tapiNodes = this.tapiNodes.values().stream()
            .filter(n -> !n.getName().containsKey(new NameKey(TapiStringConstants.VALUE_NAME_ROADM_NODE)))
            .collect(Collectors.toMap(Node::key, node -> node));
    }

    /**
     * TapiNodes List must be populated externally using the following setter.
     */
    public void setTapiNodes(Map<NodeKey, Node> nodeMap) {
        this.tapiNodes.putAll(nodeMap);
    }

    public Map<NodeKey, Node> getTapiNodes() {
        return tapiNodes;
    }

    /**
     * TapiLinks List must be populated externally using the following setter.
     */
    public void setTapiLinks(Map<LinkKey, Link> linkMap) {
        this.tapiLinks.putAll(linkMap);
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }

    /**
     * TapiSips List must be populated externally using the following setter.
     */
    public void setTapiSips(Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSip) {
        this.tapiSips.putAll(tapiSip);
    }
}

