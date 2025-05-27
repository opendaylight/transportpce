/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.sync.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CopyFromOpenRoadm implements Copy {

    private static final Logger LOG = LoggerFactory.getLogger(CopyFromOpenRoadm.class);

    private final NetworkTransactionService networkTransactionService;

    @Activate
    public CopyFromOpenRoadm(@Reference NetworkTransactionService networkTransactionService) {
        LOG.info("CopyFromOpenRoadm component activated");
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public void copyOpenRoadmTopologyToTapi(TopologyUpdateResult topologyUpdateResult) {
        LOG.info("Received topology update from OpenROADM: {}", topologyUpdateResult);

        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifiers.OPENROADM_TOPOLOGY_II
                );

        try {
            //Network network = topologyFuture.get().orElseThrow();
            //Map<NodeKey, Node> node = Optional.ofNullable(topologyFuture.get().orElseThrow().getNode()).orElseThrow();
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology = Optional.of(Optional.ofNullable(topologyFuture.get()
                    .orElseThrow().getNode()).orElseThrow()).orElseThrow().entrySet();
            LOG.info("Network present: {}", openRoadmTopology.stream().map(Map.Entry::getKey).toList());

            Map<TopologyChangesKey, TopologyChanges> topologyChanges =
                    Objects.requireNonNull(topologyUpdateResult.getTopologyChanges());

            Map<String, Set<String>> nodeIdTerminationPointIdMap = topologyChanges.values().stream()
                    .collect(Collectors.groupingBy(TopologyChanges::getNodeId,
                            Collectors.mapping(TopologyChanges::getTpId, Collectors.toSet())));

            //Find all the nodes...
            LOG.info("Node IDs: {}", nodeIdTerminationPointIdMap);
            List<Node> nodes = new ArrayList<>(nodeIdTerminationPointIdMap.size());

            Map<String, Set<TerminationPoint>> degTerminationPointList = new HashMap<>();
            Map<String, Set<TerminationPoint>> srgTerminationPointList = new HashMap<>();
            for (Map.Entry<String, Set<String>> entrySet : nodeIdTerminationPointIdMap.entrySet()) {
                String nodeId = entrySet.getKey();

                Map<NodeKey, Node> nodeMap = openRoadmTopology.stream().filter(
                        entry -> entry.getKey().equals(new NodeKey(new NodeId(nodeId)))
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                for (Map.Entry<NodeKey, Node> entry : nodeMap.entrySet()) {
                    Node node = entry.getValue();
                    Node1 node1 = node.augmentationOrElseThrow(Node1.class);

                    List<TerminationPoint> node1TpValues = Objects.requireNonNull(node1.getTerminationPoint())
                            .values().stream().toList();

                    List<TerminationPoint> tempTerminationPointList = new ArrayList<>();
                    for (String tpId : entrySet.getValue()) {
                        LOG.info("Processing Termination Point ID: {}", tpId);
                        tempTerminationPointList.addAll(node1TpValues.stream()
                                .filter(tp -> tp.getTpId().equals(TpId.getDefaultInstance(tpId)))
                                .toList());
                    }

                    OpenroadmNodeType nodeType = node.augmentationOrElseThrow(org.opendaylight.yang.gen.v1.http
                                    .org.openroadm.common.network.rev230526.Node1.class).getNodeType();

                    if (nodeType.getIntValue() == 11) {
                        degTerminationPointList.put(nodeId, new HashSet<>(tempTerminationPointList));
                    } else if (nodeType.getIntValue() == 12) {
                        srgTerminationPointList.put(nodeId, new HashSet<>(tempTerminationPointList));
                    }

                }
                nodes.addAll(nodeMap.values());
            }
            LOG.info("\nFound node(s): {}", nodes);
            LOG.info("\nDegree termination point(s): {}", degTerminationPointList);
            LOG.info("\nSRG termination point(s): {}", srgTerminationPointList);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
