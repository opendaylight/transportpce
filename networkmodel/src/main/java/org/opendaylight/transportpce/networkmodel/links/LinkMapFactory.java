/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.DestinationKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesKey;
import org.slf4j.Logger;

/**
 * Reads a node's connection map from {@link Storage} and turns it into
 * {@link Link}s between node ports, translating circuit pack names to port
 * names along the way.
 */
public class LinkMapFactory implements Factory {

    private final Logger logger;

    public LinkMapFactory(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Set<Link> connections(Map<String, Set<String>> circuitConnections, Map<String, String> translation) {
        StringBuilder conns = new StringBuilder();

        circuitConnections.forEach((key, value) -> conns
                .append("(")
                .append(key)
                .append(":")
                .append(String.join(",", value))
                .append(")")
                .append(" ")

        );

        logger.info("LinkMapFactory - Circuit connections size {} ({})", circuitConnections.size(), conns);

        int size = translation.size();
        StringBuilder trans = new StringBuilder();
        translation.forEach((key, value) -> trans
                .append("(")
                .append(key)
                .append(":")
                .append(value)
                .append(")")
                .append(" ")
        );

        logger.info("LinkMapFactory - Translation size: {} ({})", size, trans);

        Set<Link> result = new HashSet<>();

        for (Map.Entry<String, Set<String>> connections : circuitConnections.entrySet()) {

            String source = translation.get(connections.getKey());

            for (String circuitPack : connections.getValue()) {
                String destination = translation.get(circuitPack);

                logger.info("LinkMapFactory - Source/Dest {}/{}", source, destination);
                result.add(new InterfaceLink(source, destination));
            }

        }

        logger.info("LinkMapFactory - Node connection link map {}", connections(result));

        return result;
    }

    @Override
    public String connections(Set<Link> links) {
        StringBuilder builder = new StringBuilder();

        links.forEach(l -> builder
                .append("(")
                .append(l.source())
                .append(":")
                .append(l.destination())
                .append(")")
                .append(System.lineSeparator())
        );

        return builder.toString();
    }

    @Override
    public Map<String, Set<String>> circuitConnections(Storage storage, String nodeId) {
        Map<String, Set<String>> circuitConnectionMap = new HashMap<>();

        Map<NetworkNodesKey, NetworkNodes> networkNodes = storage.read(nodeId);

        if (networkNodes == null) {
            return circuitConnectionMap;
        }

        for (Map.Entry<NetworkNodesKey, NetworkNodes> networkNodeEntry : networkNodes.entrySet()) {

            Map<NodeConnectionMapKey, NodeConnectionMap> map =
                    Optional.ofNullable(networkNodeEntry.getValue().getNodeConnectionMap()).orElse(new HashMap<>());

            for (Map.Entry<NodeConnectionMapKey, NodeConnectionMap> nodeConnMap : map.entrySet()) {
                Set<String> destinationResult = new HashSet<>();

                Map<DestinationKey, Destination> destinations =
                        Optional.ofNullable(nodeConnMap.getValue().getDestination()).orElse(new HashMap<>());

                for (Map.Entry<DestinationKey, Destination> destination : destinations.entrySet()) {
                    destinationResult.add(destination.getValue().getCircuitPackName());
                }

                if (!destinationResult.isEmpty()) {
                    circuitConnectionMap.computeIfAbsent(nodeConnMap.getValue().getSource().getCircuitPackName(),
                                    key -> new HashSet<>())
                            .addAll(destinationResult);
                }
            }
        }

        logger.info("LinkMapFactory - Circuit connections {}", circuitConnectionMap);

        return circuitConnectionMap;
    }
}
