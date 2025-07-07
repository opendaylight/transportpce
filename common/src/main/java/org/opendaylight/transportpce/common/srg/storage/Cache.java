/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.common.srg.revision.Rev250702;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroupKey;

/**
 * Cached storage for Shared Risk Groups.
 * This class implements the Storage interface and provides caching functionality
 * for Shared Risk Groups associated with network nodes.
 * Suitable when one wants to avoid repeated database reads.
 */
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
    public boolean save(String nodeId, Rev250702 rev250702) {
        return save(nodeId, rev250702.srg());
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
