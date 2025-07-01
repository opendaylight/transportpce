/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NodeTerminationPoint;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NodeTypeCollection;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.TerminationPointCollection;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record OpenRoadmTopology(Set<Map.Entry<NodeKey, Node>> topology) implements ORTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    @Override
    public NodeTypeCollection terminationPoints(NodeTerminationPoint nodeTerminationPoint) {
        NodeTypeCollection collection = new TerminationPointCollection();

        for (Map.Entry<String, Set<String>> entrySet : nodeTerminationPoint.nodeTerminationPointIdMap().entrySet()) {

            Map<NodeKey, Node> nodeMap = topology.stream().filter(
                    orTopology -> orTopology.getKey().equals(new NodeKey(new NodeId(entrySet.getKey())))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            nodeMap.values().stream().map(node -> String.format(
                    "OpenROADM node ID: %s",
                    node.getNodeId().getValue()
            )).forEach(LOG::info);

            collection.addAll(this.terminationPointCollection(nodeMap, entrySet));
        }

        LOG.debug("Termination point(s): {}", collection.terminationPoints());

        return collection;
    }

    private NodeTypeCollection terminationPointCollection(
            Map<NodeKey, Node> nodeMap,
            Map.Entry<String, Set<String>> nodeIdTerminationPointIdMapper) {

        NodeTypeCollection collection = new TerminationPointCollection();

        for (Map.Entry<NodeKey, Node> entry : nodeMap.entrySet()) {
            Node node = entry.getValue();
            String supportingNodeName = supportingNodeName(node, NetworkId.getDefaultInstance("openroadm-network"));
            Node1 node1 = node.augmentationOrElseThrow(Node1.class);

            List<TerminationPoint> node1TpValues = Objects.requireNonNull(node1.getTerminationPoint())
                    .values().stream().toList();

            List<TerminationPoint> tempTerminationPointList = new ArrayList<>();
            for (String tpId : nodeIdTerminationPointIdMapper.getValue()) {
                LOG.info("Processing Termination Point ID: {}", tpId);
                tempTerminationPointList.addAll(node1TpValues.stream()
                        .filter(tp -> tp.getTpId().equals(TpId.getDefaultInstance(tpId)))
                        .toList());
            }

            collection.add(node, supportingNodeName, new HashSet<>(tempTerminationPointList));
        }

        return collection;
    }

    /**
     * Find the supporting node, e.g. ROADM-A1 for the node ROADM-A1-DEG1
     */
    private String supportingNodeName(Node node, NetworkId networkId) {
        return Objects.requireNonNull(node.getSupportingNode())
                .values().stream()
                .filter(supportingNode -> (
                        supportingNode.getNetworkRef().equals(networkId))
                ).findFirst()
                .orElseThrow()
                .getNodeRef().getValue();
    }
}
