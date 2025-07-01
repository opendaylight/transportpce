/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.tapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.ORTerminationPoint;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.NameAndValue;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record TapiTopologyNodes(Map<NodeKey, Node> nodes) implements TopologyNodes {

    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyNodes.class);

    @Override
    public Map<NodeKey, Node> nodes() {
        return nodes;
    }

    @Override
    public boolean isEmpty() {
        return nodes().isEmpty();
    }

    @Override
    public int size() {
        return nodes().size();
    }

    @Override
    public Set<NodeKey> nodeKeys() {
        return nodes.keySet();
    }

    @Override
    public NodeEdgePoints updatedNodeEdgePoints(
            ORTerminationPoint orTerminationPoint,
            TapiContext tapiContext) {

        return createUpdatedTapiNodes(
                createOwnNodeEdgePoints(
                        nodes,
                        orTerminationPoint,
                        tapiContext
                ),
                nodes
        );
    }

    /**
     * This method finds the nodes in the existing TAPI topology for the given newOwnNodeEdgePoints.
     */
    private NodeEdgePoints createUpdatedTapiNodes(
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> newOwnNodeEdgePoints,
            Map<NodeKey, Node> tapiTopologyNodes) {

        Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

        newOwnNodeEdgePoints.forEach((key, newNodeEdgePoint) -> {
            LOG.debug("OwnedNodeEdgePoint Key: {}, Value: {}", key.getUuid().getValue(), newNodeEdgePoint);

            Name newNodeEdgePointReferenceName = Objects.requireNonNull(newNodeEdgePoint.getName())
                    .values().stream().findFirst().orElseThrow();

            updatedTapiNodes.putAll(
                    addNodeOwnEdgePoints(
                            newNodeEdgePointReferenceName,
                            newNodeEdgePoint,
                            tapiTopologyNodes.entrySet()
            ));
        });
        LOG.debug("Node OwnedNodeEdgePoints: {}", updatedTapiNodes);
        return new TapiNodeEdgePoint(updatedTapiNodes);
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createOwnNodeEdgePoints(
            Map<NodeKey, Node> tapiTopologyNodes,
            ORTerminationPoint orTerminationPoint,
            TapiContext tapiContext) {

        if (tapiTopologyNodes.isEmpty()) {
            LOG.error("No TAPI topology nodes found, cannot update Own Node Edge Points.");
            return new HashMap<>();
        }
        return orTerminationPoint.ownedNodeEdgePoints(tapiContext);
    }

    private Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> addNodeOwnEdgePoints(
            Name newNodeEdgePointReferenceName,
            OwnedNodeEdgePoint newNodeEdgePoint,
            Set<Map.Entry<NodeKey, Node>> currentTopologyNodes) {

        Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

        for (Map.Entry<NodeKey, Node> currentTapiTopologyNode : currentTopologyNodes) {

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

        return updatedTapiNodes;
    }

    /**
     * Finds the node key in the TAPI topology containing the newNodeEdgePointReferenceName.
     */
    private Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> findEdgePointParentNodeKey(
            Name newNodeEdgePointReferenceName,
            OwnedNodeEdgePoint newNodeEdgePoint,
            Map.Entry<NodeKey, Node> currentTapiTopologyNode) {

        Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

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
                    NodeKey nodeKey = currentTapiTopologyNode.getKey();
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
            Map.Entry<NodeKey, Node> currentTapiTopologyNode,
            NodeKey nodeKey) {

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
