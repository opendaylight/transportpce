/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupKey;

public class Cache implements Storage {

    private final Storage storage;

    private final Map<String, Map<SharedRiskGroupKey, SharedRiskGroup>> nodeSharedRiskGroup = new HashMap<>();

    public Cache(Storage storage) {
        this.storage = storage;
    }

    @Override
    public boolean save(String nodeId, Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap) {

        nodeSharedRiskGroup.remove(nodeId);

        boolean result = storage.save(nodeId, sharedRiskGroupMap);

        if (result) {
            nodeSharedRiskGroup.put(nodeId, sharedRiskGroupMap);
        }

        return result;

    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(String nodeId) {
        return read(new NetworkNodesKey(nodeId));
    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(NetworkNodesKey nodeId) {
        if (nodeSharedRiskGroup.containsKey(nodeId.getNodeId())) {
            return nodeSharedRiskGroup.get(nodeId.getNodeId());
        }

        Map<SharedRiskGroupKey, SharedRiskGroup> tmp = storage.read(nodeId);
        if (!tmp.isEmpty()) {
            nodeSharedRiskGroup.put(nodeId.getNodeId(), tmp);
            return tmp;
        }

        return new HashMap<>();
    }
}