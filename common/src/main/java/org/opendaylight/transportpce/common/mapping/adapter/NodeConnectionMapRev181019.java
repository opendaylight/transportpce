/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.adapter;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.node.connection.map.Source;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.DestinationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.connection.map.SourceBuilder;

public class NodeConnectionMapRev181019 implements Rev181019 {

    @Override
    public Map<ConnectionMapKey, ConnectionMap> connectionMapRev181019(
        Map<NodeConnectionMapKey, NodeConnectionMap> connectionMapMap
    ) {

        Map<ConnectionMapKey, ConnectionMap> rev181019 = new HashMap<>();

        if (connectionMapMap == null) {
            return rev181019;
        }

        for (Map.Entry<NodeConnectionMapKey, NodeConnectionMap> connectionMap : connectionMapMap.entrySet()) {

            Source source = connectionMap.getValue().getSource();
            SourceBuilder sourceBuilder = new SourceBuilder();
            sourceBuilder
                .setPortName(source.getPortName())
                .setCircuitPackName(source.getCircuitPackName());

            Map<DestinationKey, Destination> newDestination = new HashMap<>();

            Map<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping
                .rev231026.node.connection.map.group.node.connection.map
                .DestinationKey,
                org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping
                    .rev231026.node.connection.map.group.node.connection.map
                    .Destination> destination = connectionMap.getValue().getDestination();

            assert destination != null;

            for (Map.Entry<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping
                .rev231026.node.connection.map.group.node.connection.map.DestinationKey,
                org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping
                    .rev231026.node.connection.map.group.node.connection.map
                    .Destination> dest : destination.entrySet()) {

                DestinationBuilder destinationBuilder = new DestinationBuilder();
                org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping
                    .rev231026.node.connection.map.group.node.connection.map
                    .Destination value = dest.getValue();
                destinationBuilder
                    .setCircuitPackName(value.getCircuitPackName())
                    .setPortName(value.getPortName());
                org.opendaylight.yang.gen.v1.http.org.openroadm.device
                    .rev181019.org.openroadm.device.container.org.openroadm.device.connection.map
                    .Destination build = destinationBuilder.build();
                newDestination.put(build.key(), build);
            }

            ConnectionMapBuilder connectionMapBuilder = new ConnectionMapBuilder();
            connectionMapBuilder
                .setConnectionMapNumber(connectionMap.getValue().getConnectionMapNumber())
                .setSource(sourceBuilder.build())
                .setDestination(newDestination);

            ConnectionMap build = connectionMapBuilder.build();

            rev181019.put(build.key(), build);
        }

        return rev181019;
    }

}
