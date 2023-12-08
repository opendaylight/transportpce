/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import com.google.common.util.concurrent.FluentFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.Translate;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.OpenroadmConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.OpenroadmConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageRev200529 implements Rev200529 {

    private static final Logger LOG = LoggerFactory.getLogger(StorageRev200529.class);

    @Override
    public boolean save(DataBroker dataBroker,
                        Translate openRoadm,
                        String nodeId,
                        Map<ConnectionMapKey, ConnectionMap> connectionMap) {

        if (connectionMap == null) {
            LOG.debug("Not saving connection map for node {} since it's empty.", nodeId);
            return true;
        }

        NetworkNodesBuilder networkNodesBuilder = new NetworkNodesBuilder();
        networkNodesBuilder.setNodeId(nodeId);
        Map<NodeConnectionMapKey, NodeConnectionMap> values = openRoadm.toTransportPCErev200529(connectionMap);
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
            LOG.debug("Saved connection map for node {} to storage.", nodeId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed saving connection map for node {} to storage: {}", nodeId, e.getMessage());
            return false;
        }

    }
}
