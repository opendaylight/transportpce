/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.mapping.adapter.Rev181019;
import org.opendaylight.transportpce.common.mapping.adapter.Rev231026;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.OpenroadmConnectionMap;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.OpenroadmConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.openroadm.connection.map.NetworkNodesBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.portmapping.rev231026.openroadm.connection.map.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

public class ConnectionMapStorage implements Storage {

    private final DataBroker dataBroker;

    private final Rev181019 rev181019;

    private final Rev231026 rev231026;

    private final Logger log;

    public ConnectionMapStorage(DataBroker dataBroker, Rev181019 rev181019, Rev231026 rev231026, Logger log) {
        this.dataBroker = dataBroker;
        this.rev181019 = rev181019;
        this.rev231026 = rev231026;
        this.log = log;
    }

    @Override
    public boolean save(String nodeId, Map<ConnectionMapKey, ConnectionMap> connectionMap) {

        if (connectionMap != null) {
            NetworkNodesBuilder networkNodesBuilder = new NetworkNodesBuilder();
            networkNodesBuilder.setNodeId(nodeId);
            Map<NodeConnectionMapKey, NodeConnectionMap> values = rev231026.connectionMapRev231026(connectionMap);
            networkNodesBuilder.setNodeConnectionMap(values);
            NetworkNodes build = networkNodesBuilder.build();

            Map<NetworkNodesKey, NetworkNodes> networkNodes = new HashMap<>();
            networkNodes.put(build.key(), build);

            OpenroadmConnectionMapBuilder openroadmConnectionMapBuilder = new OpenroadmConnectionMapBuilder();
            openroadmConnectionMapBuilder.setNetworkNodes(networkNodes);
            OpenroadmConnectionMap openRoadmNetwork = openroadmConnectionMapBuilder.build();

            InstanceIdentifier<OpenroadmConnectionMap> nodesIID =
                InstanceIdentifier.builder(OpenroadmConnectionMap.class).build();
            WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
            writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, nodesIID, openRoadmNetwork);
            FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();

            try {
                commit.get();
                log.debug("Saved connection map for node {} to storage.", nodeId);
                return true;
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed saving connection map for node {} to storage: {}", nodeId, e.getMessage());
                return false;
            }

        }

        log.debug("Not saving connection map for node {} since it's empty.", nodeId);
        return true;
    }

    @Override
    public Map<ConnectionMapKey, ConnectionMap> read(String nodeId) {
        InstanceIdentifier<OpenroadmConnectionMap> nodesIID =
            InstanceIdentifier.builder(OpenroadmConnectionMap.class).build();

        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            Optional<OpenroadmConnectionMap> connectionMap =
                readTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesIID).get();

            OpenroadmConnectionMap openroadmConnectionMap = connectionMap.orElseThrow();
            Map<NetworkNodesKey, NetworkNodes> connectionMapNodes = openroadmConnectionMap.getNetworkNodes();

            if (connectionMapNodes != null) {
                Collection<NetworkNodes> values = connectionMapNodes.values();

                log.debug("Read {} connection maps from storage for node {}.", values.size(), nodeId);
                for (NetworkNodes networkNodes : values) {
                    Map<NodeConnectionMapKey, NodeConnectionMap> nodeMap = networkNodes.getNodeConnectionMap();
                    return rev181019.connectionMapRev181019(nodeMap);
                }
            } else {
                log.debug("No connection map found for node {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
            log.warn("Exception thrown while reading Logical Connection Point value", ex);
        }

        return rev181019.connectionMapRev181019(null);
    }
}
