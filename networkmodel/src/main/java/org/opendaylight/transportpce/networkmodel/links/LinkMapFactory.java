/*
 * Copyright © 2023  AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.transportpce.common.mapping.connectionmap.Storage;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.DestinationKey;
import org.slf4j.Logger;


public class LinkMapFactory implements Factory {

    private final Logger logger;

    public LinkMapFactory(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Set<Link> connections(
            Map<String, Set<String>> circuitConnections,
            Map<String, String> translation
    ) {

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

        Map<ConnectionMapKey, ConnectionMap> connectionMap = storage.read(nodeId);
        if (connectionMap != null) {
            logger.info("Connection map size {}", connectionMap.size());

            for (Map.Entry<ConnectionMapKey, ConnectionMap> entry : connectionMap.entrySet()) {
                Set<String> destinations = new HashSet<>();

                for (Map.Entry<DestinationKey, Destination> asd : Objects
                    .requireNonNull(entry.getValue().getDestination()).entrySet()) {
                    destinations.add(asd.getValue().getCircuitPackName());
                }

                if (!circuitConnectionMap.containsKey(entry.getValue().getSource().getCircuitPackName())) {
                    circuitConnectionMap.put(entry.getValue().getSource().getCircuitPackName(), destinations);
                } else {
                    circuitConnectionMap
                        .get(entry.getValue().getSource().getCircuitPackName())
                        .addAll(destinations);
                }

            }
        }

        return circuitConnectionMap;
    }
}
