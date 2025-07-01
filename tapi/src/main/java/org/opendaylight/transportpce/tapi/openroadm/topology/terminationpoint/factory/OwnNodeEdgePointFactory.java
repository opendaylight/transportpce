/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.factory;

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

public class OwnNodeEdgePointFactory implements OwnNodeEdgePoint {

    private static final Logger LOG = LoggerFactory.getLogger(OwnNodeEdgePointFactory.class);

    private final ORTerminationPoint orTerminationPoint;

    private final TapiContext tapiContext;

    public OwnNodeEdgePointFactory(ORTerminationPoint orTerminationPoint, TapiContext tapiContext) {
        this.orTerminationPoint = orTerminationPoint;
        this.tapiContext = tapiContext;
    }

    @Override
    public Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> createUpdatedOwnNodeEdgePoints(
            Map<NodeKey, Node> tapiTopologyNodes, Set<Map.Entry<org.opendaylight.yang.gen.v1.urn.ietf.params.xml
                    .ns.yang.ietf.network.rev180226.networks.network.NodeKey, org.opendaylight.yang.gen.v1.urn.ietf
                    .params.xml.ns.yang.ietf.network.rev180226.networks.network.Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap) {

        return createUpdatedTapiNodes(
                createOwnNodeEdgePoints(
                        tapiTopologyNodes,
                        openRoadmTopology,
                        nodeIdTerminationPointIdMap
                ),
                tapiTopologyNodes
        );
    }

    /**
     * This method finds the nodes in the existing TAPI topology for the given newOwnNodeEdgePoints.
     */
    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> createUpdatedTapiNodes(
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

            updatedTapiNodes.putAll(addNodeOwnEdgePoints(
                    newNodeEdgePointReferenceName,
                    newNodeEdgePoint,
                    currentTopologyNodes
            ));
        });
        LOG.debug("Node OwnedNodeEdgePoints: {}", updatedTapiNodes);
        return updatedTapiNodes;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createOwnNodeEdgePoints(
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                    tapiTopologyNodes,
            Set<Map.Entry<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                    .rev180226.networks.network.NodeKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.rev180226.networks.network.Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap) {

        if (tapiTopologyNodes.isEmpty()) {
            LOG.error("No TAPI topology nodes found, cannot update Own Node Edge Points.");
            return new HashMap<>();
        }
        return this.orTerminationPoint.ownedNodeEdgePoints(
                openRoadmTopology, nodeIdTerminationPointIdMap, this.tapiContext
        );
    }

    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> addNodeOwnEdgePoints(
            Name newNodeEdgePointReferenceName,
            OwnedNodeEdgePoint newNodeEdgePoint,
            Set<Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.Node>> currentTopologyNodes) {

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

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
                    .rev221121.topology.NodeKey nodeKey) {

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
