/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.topology.storage.OpenROADM;
import org.opendaylight.transportpce.tapi.openroadm.topology.storage.TAPI;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.factory.OwnNodeEdgePoint;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopology implements Topology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    private final TapiContext tapiContext;

    private final TAPI tapiStorage;

    private final OpenROADM openRoadmStorage;

    private final OwnNodeEdgePoint ownNodeEdgePointFactory;

    public OpenRoadmTopology(
            TapiContext tapiContext,
            TAPI tapiStorage,
            OpenROADM openRoadmStorage,
            OwnNodeEdgePoint ownNodeEdgePointFactory) {

        this.tapiContext = tapiContext;
        this.tapiStorage = tapiStorage;
        this.openRoadmStorage = openRoadmStorage;
        this.ownNodeEdgePointFactory = ownNodeEdgePointFactory;
    }

    @Override
    public boolean copyTopologyToTAPI(TopologyUpdateResult topologyUpdateResult) {
        LOG.info("TAPI - Received topology update");
        LOG.debug("Topology update result: {}", topologyUpdateResult);

        Map<String, Set<String>> nodeIdTerminationPointIdMap = nodeIdTerminationPointIdMap(topologyUpdateResult);

        if (nodeIdTerminationPointIdMap.isEmpty()) {
            LOG.warn("Unable to update TAPI topology, no Termination Points found in OpenROADM topology.");
            return false;
        }

        TopologyKey topologyKey = new TopologyKey(TapiConstants.T0_FULL_MULTILAYER_UUID);
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                tapiTopologyNodes = tapiTopologyNodes(tapiContext, topologyKey);
        if (tapiTopologyNodes.isEmpty()) {
            return false;
        }

        LOG.debug("TAPI topology nodes: {}", tapiTopologyNodes.keySet());

        Set<Map.Entry<NodeKey, Node>> openRoadmTopology = openRoadmStorage.openRoadmTopology(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedOwnNodeEdgePoints =
                ownNodeEdgePointFactory.createUpdatedOwnNodeEdgePoints(
                        tapiTopologyNodes,
                        openRoadmTopology,
                        nodeIdTerminationPointIdMap
                );

        LOG.info("Updating {} node(s) in TAPI context {}...", updatedOwnNodeEdgePoints.size(),
                topologyKey.getUuid().getValue());

        boolean result = tapiStorage.updateTopology(topologyKey, updatedOwnNodeEdgePoints);

        LOG.info("All nodes updated in TAPI context {}.", topologyKey.getUuid().getValue());

        return result;
    }

    private Map<String, Set<String>> nodeIdTerminationPointIdMap(TopologyUpdateResult topologyUpdateResult) {
        Map<TopologyChangesKey, TopologyChanges> topologyChanges =
                Objects.requireNonNull(topologyUpdateResult.getTopologyChanges());

        Map<String, Set<String>> nodeIdTerminationPointIdMap = topologyChanges.values().stream()
                .collect(Collectors.groupingBy(TopologyChanges::getNodeId,
                        Collectors.mapping(TopologyChanges::getTpId, Collectors.toSet())));

        LOG.info("Node ID to Termination Point ID map: {}", nodeIdTerminationPointIdMap);

        return nodeIdTerminationPointIdMap;
    }

    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiTopologyNodes(
            TapiContext objTapiContext, TopologyKey topologyKey) {

        Map<TopologyKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.context.Topology> tapiTopologyContext = objTapiContext.getTopologyContext();

        if (tapiTopologyContext != null && !tapiTopologyContext.isEmpty()) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.context.Topology tapiTopology =
                    tapiTopologyContext.get(topologyKey);

            return tapiTopology.getNode();
        }

        LOG.error("No TAPI topology context found for topology key: {}", topologyKey);
        return new HashMap<>();
    }

}
