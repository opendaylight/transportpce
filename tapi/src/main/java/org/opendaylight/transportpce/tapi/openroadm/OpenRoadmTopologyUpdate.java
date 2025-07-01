/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.tapi.NodeEdgePoints;
import org.opendaylight.transportpce.tapi.openroadm.tapi.TapiTopologyNodes;
import org.opendaylight.transportpce.tapi.openroadm.tapi.TopologyNodes;
import org.opendaylight.transportpce.tapi.openroadm.topology.ORTopology;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenROADM;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TAPI;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NodeTerminationPoint;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.OpenRoadmTerminationPoint;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.OpenRoadmTerminationPointMapper;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopologyUpdate implements TopologyUpdate {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopologyUpdate.class);

    private final TapiContext tapiContext;

    private final TAPI tapiStorage;

    private final OpenROADM openRoadmStorage;

    private final NetworkTransactionService networkTransactionService;

    public OpenRoadmTopologyUpdate(
            TapiContext tapiContext,
            TAPI tapiStorage,
            OpenROADM openRoadmStorage,
            NetworkTransactionService networkTransactionService) {

        this.tapiContext = tapiContext;
        this.tapiStorage = tapiStorage;
        this.openRoadmStorage = openRoadmStorage;
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public boolean copyToTAPI(TopologyUpdateResult topologyUpdateResult) {
        LOG.info("TAPI - Received topology update");
        LOG.debug("Topology update result: {}", topologyUpdateResult);

        NodeTerminationPoint nodeIdTerminationPointIdMap = OpenRoadmTerminationPointMapper.mapper(topologyUpdateResult);

        if (nodeIdTerminationPointIdMap.isEmpty()) {
            LOG.warn("Unable to update TAPI topology, no Termination Points found in OpenROADM topology.");
            return false;
        }

        TopologyKey topologyKey = new TopologyKey(TapiConstants.T0_FULL_MULTILAYER_UUID);
        TopologyNodes tapiTopologyNodes = tapiTopologyNodes(tapiContext, topologyKey);
        if (tapiTopologyNodes.isEmpty()) {
            return false;
        }

        LOG.debug("TAPI topology nodes: {}", tapiTopologyNodes.nodeKeys());

        ORTopology openRoadmTopology = openRoadmStorage.openRoadmTopology(
                LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        NodeEdgePoints updatedNodeEdgePoints =
                tapiTopologyNodes.updatedNodeEdgePoints(
                        new OpenRoadmTerminationPoint(
                                networkTransactionService,
                                nodeIdTerminationPointIdMap,
                                openRoadmTopology
                        ),
                        tapiContext
                );

        return updatedNodeEdgePoints.updateDataStore(topologyKey, tapiStorage);
    }

    private TopologyNodes tapiTopologyNodes(TapiContext objTapiContext, TopologyKey topologyKey) {
        Map<TopologyKey, Topology> tapiTopologyContext = objTapiContext.getTopologyContext();

        if (tapiTopologyContext != null && !tapiTopologyContext.isEmpty()) {
            Topology tapiTopology = tapiTopologyContext.get(topologyKey);

            return new TapiTopologyNodes(tapiTopology.getNode());
        }

        LOG.error("No TAPI topology context found for topology key: {}", topologyKey);
        return new TapiTopologyNodes(new HashMap<>());
    }

}
