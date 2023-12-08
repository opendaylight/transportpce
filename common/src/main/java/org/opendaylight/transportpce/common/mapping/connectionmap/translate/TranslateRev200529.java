/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.translate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.node.connection.map.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.node.connection.map.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.DestinationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.Source;

/**
 * {@link Rev200529} implementation translating a rev200529 connection map into the
 * TransportPCE connection-map model.
 */
public class TranslateRev200529 implements Rev200529 {

    @Override
    public Map<NodeConnectionMapKey, NodeConnectionMap> toTransportPCE(
        Map<ConnectionMapKey, ConnectionMap> openRoadmConnectionMap) {

        Map<NodeConnectionMapKey, NodeConnectionMap> result = new HashMap<>();

        for (Map.Entry<ConnectionMapKey, ConnectionMap> connectionMap : openRoadmConnectionMap.entrySet()) {

            Source source = connectionMap.getValue().getSource();
            SourceBuilder sourceBuilder = new SourceBuilder();
            sourceBuilder
                    .setPortName(source.getPortName())
                    .setCircuitPackName(source.getCircuitPackName());

            Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026
                    .node.connection.map.group.node.connection.map.DestinationKey,
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026
                    .node.connection.map.group.node.connection.map.Destination> newDestination = new HashMap<>();

            Map<DestinationKey, Destination> destination =
                    Optional.ofNullable(connectionMap.getValue().getDestination()).orElse(new HashMap<>());

            for (Map.Entry<DestinationKey, Destination> dest : destination.entrySet()) {
                DestinationBuilder destinationBuilder = new DestinationBuilder();
                Destination value = dest.getValue();

                destinationBuilder
                    .setCircuitPackName(value.getCircuitPackName())
                    .setPortName(value.getPortName());

                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026
                        .node.connection.map.group.node.connection.map.Destination build = destinationBuilder.build();

                newDestination.put(build.key(), build);
            }

            NodeConnectionMapBuilder connectionMapBuilder = new NodeConnectionMapBuilder();
            connectionMapBuilder
                .setConnectionMapNumber(connectionMap.getValue().getConnectionMapNumber())
                .setSource(sourceBuilder.build())
                .setDestination(newDestination);

            NodeConnectionMap build = connectionMapBuilder.build();

            result.put(build.key(), build);
        }

        return result;
    }
}
