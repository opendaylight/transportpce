/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.topology.storage.OpenROADM;
import org.opendaylight.transportpce.tapi.openroadm.topology.storage.TAPI;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.ORTerminationPoint;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.NameAndValue;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopology implements Topology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    private final TapiContext tapiContext;

    private final TAPI tapiStorage;

    private final OpenROADM openRoadmStorage;

    private final ORTerminationPoint orTerminationPoint;

    public OpenRoadmTopology(
            TapiContext tapiContext,
            TAPI tapiStorage,
            OpenROADM openRoadmStorage,
            ORTerminationPoint orTerminationPoint) {
        this.tapiContext = tapiContext;
        this.tapiStorage = tapiStorage;
        this.openRoadmStorage = openRoadmStorage;
        this.orTerminationPoint = orTerminationPoint;
    }

    @Override
    public boolean copyTopologyToTAPI(TopologyUpdateResult topologyUpdateResult) {
        LOG.info("TAPI - Received topology update");
        LOG.debug("Topology update result: {}", topologyUpdateResult);

        Map<String, Set<String>> nodeIdTerminationPointIdMap = nodeIdTerminationPointIdMap(topologyUpdateResult);

        if (nodeIdTerminationPointIdMap.isEmpty()) {
            LOG.warn("Unable to update TAPI topology, no Termination Points found in OpenROADM topology.");
            return false;
        }

        TopologyKey topologyKey = new TopologyKey(TapiConstants.T0_FULL_MULTILAYER_UUID);
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                tapiTopologyNodes = tapiTopologyNodes(tapiContext, topologyKey);
        if (tapiTopologyNodes.isEmpty()) {
            return false;
        }

        LOG.debug("TAPI topology nodes: {}", tapiTopologyNodes.keySet());

        Set<Map.Entry<NodeKey, Node>> openRoadmTopology = openRoadmStorage.openRoadmTopology(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedOwnNodeEdgePoints =
                createUpdatedOwnNodeEdgePoints(tapiTopologyNodes, openRoadmTopology, nodeIdTerminationPointIdMap);

        LOG.info("Updating {} node(s) in TAPI context {}...", updatedOwnNodeEdgePoints.size(),
                topologyKey.getUuid().getValue());

        boolean result = tapiStorage.updateTopology(topologyKey, updatedOwnNodeEdgePoints);

        LOG.info("All nodes updated in TAPI context {}.", topologyKey.getUuid().getValue());

        return result;
    }

    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> createUpdatedOwnNodeEdgePoints(
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                    tapiTopologyNodes,
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap
    ) {
        return createNodeOwnEdgePoints(
                createOwnNodeEdgePoints(
                        tapiTopologyNodes,
                        openRoadmTopology,
                        nodeIdTerminationPointIdMap
                ),
                tapiTopologyNodes
        );
    }

    private Map<String, Set<String>> nodeIdTerminationPointIdMap(TopologyUpdateResult topologyUpdateResult) {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges =
                Objects.requireNonNull(topologyUpdateResult.getTopologyChanges());

        Map<String, Set<String>> nodeIdTerminationPointIdMap = topologyChanges.values().stream()
                .collect(Collectors.groupingBy(TopologyChanges::getNodeId,
                        Collectors.mapping(TopologyChanges::getTpId, Collectors.toSet())));

        LOG.info("Node ID to Termination Point ID map: {}", nodeIdTerminationPointIdMap);

        return nodeIdTerminationPointIdMap;
    }

    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiTopologyNodes(
            TapiContext objTapiContext, TopologyKey topologyKey) {

        Map<TopologyKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.context.Topology> tapiTopologyContext = objTapiContext.getTopologyContext();

        if (tapiTopologyContext != null && !tapiTopologyContext.isEmpty()) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.context.Topology tapiTopology =
                    tapiTopologyContext.get(topologyKey);

            return tapiTopology.getNode();
        }

        LOG.error("No TAPI topology context found for topology key: {}", topologyKey);
        return new HashMap<>();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createOwnNodeEdgePoints(
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                    tapiTopologyNodes,
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap
    ) {
        if (tapiTopologyNodes.isEmpty()) {
            LOG.error("No TAPI topology nodes found, cannot update Own Node Edge Points.");
            return new HashMap<>();
        }
        return this.orTerminationPoint.ownedNodeEdgePoints(
                openRoadmTopology, nodeIdTerminationPointIdMap, this.tapiContext
        );
    }

    /**
     * This method finds the nodes in the existing TAPI topology for the given newOwnNodeEdgePoints.
     */
    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> createNodeOwnEdgePoints(
                    Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> newOwnNodeEdgePoints,
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                    tapiTopologyNodes) {

        Set<Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                        .rev221121.topology.Node>> currentTopologyNodes = tapiTopologyNodes.entrySet();

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

        newOwnNodeEdgePoints.forEach((key, newNodeEdgePoint) -> {
            LOG.debug("OwnedNodeEdgePoint Key: {}, Value: {}", key.getUuid().getValue(), newNodeEdgePoint);

            Name newNodeEdgePointReferenceName = Objects.requireNonNull(newNodeEdgePoint.getName())
                    .values().stream().findFirst().orElseThrow();

            for (Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.Node> currentTapiTopologyNode : currentTopologyNodes) {

                findEdgePointParentNodeKey(
                        newNodeEdgePointReferenceName,
                        newNodeEdgePoint,
                        currentTapiTopologyNode
                ).forEach((nodeKey, updatedNodeEdgePoints) -> {
                    if (!updatedTapiNodes.containsKey(nodeKey)) {
                        updatedTapiNodes.put(nodeKey, new HashMap<>());
                    }
                    updatedTapiNodes.get(nodeKey).putAll(updatedNodeEdgePoints);
                });

            }
        });
        LOG.debug("Node OwnedNodeEdgePoints: {}", updatedTapiNodes);
        return updatedTapiNodes;
    }

    /**
     * Finds the node key in the TAPI topology containing the newNodeEdgePointReferenceName.
     */
    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> findEdgePointParentNodeKey(
                    Name newNodeEdgePointReferenceName,
                    OwnedNodeEdgePoint newNodeEdgePoint,
                    Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.NodeKey,
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.Node> currentTapiTopologyNode) {

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> currentTapiNodeEdgePoints = currentTapiTopologyNode.getValue()
                .getOwnedNodeEdgePoint();

        if (currentTapiNodeEdgePoints == null) {
            return updatedTapiNodes;
        }

        currentTapiNodeEdgePoints
                .entrySet()
                .stream()
                .filter(
                        edgePoint -> Objects.requireNonNull(
                                edgePoint.getValue().getName()
                        ).containsValue(newNodeEdgePointReferenceName))
                .forEach(edgePoint -> {
                    LOG.debug("Current OwnedNodeEdgePoint: {} found when searching for {}", edgePoint,
                            newNodeEdgePointReferenceName.getValue());
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.NodeKey nodeKey = currentTapiTopologyNode.getKey();
                    logFoundNodeEdgePoint(
                            newNodeEdgePointReferenceName, newNodeEdgePoint, currentTapiTopologyNode, nodeKey);

                    if (!updatedTapiNodes.containsKey(nodeKey)) {
                        updatedTapiNodes.put(nodeKey, new HashMap<>());
                    }
                    updatedTapiNodes.get(nodeKey).put(newNodeEdgePoint.key(), newNodeEdgePoint);
                });

        return updatedTapiNodes;
    }

    private void logFoundNodeEdgePoint(
            Name newNodeEdgePointReferenceName,
            OwnedNodeEdgePoint newNodeEdgePoint,
            Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.Node> currentTapiTopologyNode,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.NodeKey nodeKey
    ) {
        LOG.info("Found OwnedNodeEdgePoint {} (uuid: {}) in TAPI topology "
                        + "on node {} (uuid: {})",
                newNodeEdgePointReferenceName.getValue(),
                newNodeEdgePoint.key().getUuid().getValue(),
                currentTapiTopologyNode.getValue().nonnullName()
                        .values().stream()
                        .map(NameAndValue::getValue)
                        .collect(Collectors.joining()),
                nodeKey.getUuid().getValue());
    }
}
