/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record OpenRoadmTerminationPointMapper(Map<String, Set<String>> mapper) implements  NodeTerminationPoint {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTerminationPointMapper.class);

    @Override
    public Map<String, Set<String>> nodeTerminationPointIdMap() {
        return mapper;
    }

    @Override
    public boolean isEmpty() {
        return mapper.isEmpty();
    }

    public static NodeTerminationPoint mapper(TopologyUpdateResult topologyUpdateResult) {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges =
                Objects.requireNonNull(topologyUpdateResult.getTopologyChanges());

        Map<String, Set<String>> nodeIdTerminationPointIdMap = topologyChanges.values().stream()
                .collect(Collectors.groupingBy(TopologyChanges::getNodeId,
                        Collectors.mapping(TopologyChanges::getTpId, Collectors.toSet())));

        LOG.info("Node ID to Termination Point ID map: {}", nodeIdTerminationPointIdMap);

        return new OpenRoadmTerminationPointMapper(nodeIdTerminationPointIdMap);
    }
}
