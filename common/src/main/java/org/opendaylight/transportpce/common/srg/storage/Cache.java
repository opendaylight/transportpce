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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.shared.risk.group.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.shared.risk.group.SharedRiskGroupKey;

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
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(String nodeId) {
        return read(new NodesKey(nodeId));
    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(NodesKey nodeId) {
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
