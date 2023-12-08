/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.OpenroadmConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Rev181019} implementation that translates a rev181019 connection map into the
 * TransportPCE connection-map model and writes it to the operational datastore.
 */
public class StorageRev181019 implements Rev181019 {

    private static final Logger LOG = LoggerFactory.getLogger(StorageRev181019.class);

    @Override
    public boolean save(DataBroker dataBroker,
                        Translate openRoadm,
                        String nodeId,
                        Map<ConnectionMapKey, ConnectionMap> connectionMap) {

        NetworkNodesBuilder networkNodesBuilder = new NetworkNodesBuilder();
        networkNodesBuilder.setNodeId(nodeId);

        Map<NodeConnectionMapKey, NodeConnectionMap> values = new HashMap<>();

        if (connectionMap != null && !connectionMap.isEmpty()) {
            values = openRoadm.toTransportPCErev181019(connectionMap);
        }
        networkNodesBuilder.setNodeConnectionMap(values);
        NetworkNodes networkNodes = networkNodesBuilder.build();

        DataObjectIdentifier<NetworkNodes> nodesIID = DataObjectIdentifier
                .builder(OpenroadmConnectionMap.class)
                .child(NetworkNodes.class, new NetworkNodesKey(nodeId))
                .build();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, nodesIID, networkNodes);
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
