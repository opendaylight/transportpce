/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.topology.ConvertTopoORtoTapiAtInit;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.NameAndValue;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopology implements Topology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    private final NetworkTransactionService networkTransactionService;

    private final TapiContext tapiContext;

    public OpenRoadmTopology(
            NetworkTransactionService networkTransactionService,
            TapiContext tapiContext) {
        this.networkTransactionService = networkTransactionService;
        this.tapiContext = tapiContext;
    }

    @Override
    public boolean copyTopologyToTAPI(TopologyUpdateResult topologyUpdateResult) {
        LOG.info("TAPI - Received topology update");
        LOG.debug("Topology update result: {}", topologyUpdateResult);

        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifiers.OPENROADM_TOPOLOGY_II
                );

        Set<Map.Entry<NodeKey, Node>> openRoadmTopology;
        try {
            openRoadmTopology = Optional.of(Optional.ofNullable(topologyFuture.get()
                    .orElseThrow().getNode()).orElseThrow()).orElseThrow().entrySet();
            LOG.debug("OpenROADM topology: {}", openRoadmTopology.stream().map(Map.Entry::getKey).toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        Map<TopologyChangesKey, TopologyChanges> topologyChanges =
                Objects.requireNonNull(topologyUpdateResult.getTopologyChanges());

        Map<String, Set<String>> nodeIdTerminationPointIdMap = topologyChanges.values().stream()
                .collect(Collectors.groupingBy(TopologyChanges::getNodeId,
                        Collectors.mapping(TopologyChanges::getTpId, Collectors.toSet())));
        if (nodeIdTerminationPointIdMap.isEmpty()) {
            LOG.warn("Unable to update TAPI topology, no Termination Points found in OpenROADM topology.");
            return false;
        }
        LOG.info("Node ID to Termination Point ID map: {}", nodeIdTerminationPointIdMap);

        TopologyKey topologyKey = new TopologyKey(TapiConstants.T0_FULL_MULTILAYER_UUID);
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                tapiTopologyNodes = tapiTopologyNodes(tapiContext, topologyKey);
        if (tapiTopologyNodes.isEmpty()) {
            return false;
        }

        LOG.debug("TAPI topology nodes: {}", tapiTopologyNodes.keySet());

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedOwnNodeEdgePoints = createNodeOwnEdgePoints(
                createOwnNodeEdgePoints(
                        tapiTopologyNodes,
                        openRoadmTopology,
                        nodeIdTerminationPointIdMap
                ),
                tapiTopologyNodes
        );

        LOG.info("Updating {} node(s) in TAPI context {}...", updatedOwnNodeEdgePoints.size(),
                topologyKey.getUuid().getValue());

        for (Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> node : updatedOwnNodeEdgePoints.entrySet()) {

            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNodeEdgePointMap = node.getValue();
            LOG.info("Updating TAPI node uuid: {} with {} updated OwnedNodeEdgePoints...",
                    node.getKey().getUuid().getValue(), ownedNodeEdgePointMap.entrySet());

            for (OwnedNodeEdgePoint ownedNodeEdgePoints : ownedNodeEdgePointMap.values()) {
                if (!updateTopology(topologyKey, node.getKey(), ownedNodeEdgePoints)) {
                    return false;
                }
            }
            LOG.debug("... done updating TAPI node {} context with OwnedNodeEdgePoints.",
                    node.getKey().getUuid().getValue());
        }
        LOG.info("All nodes updated in TAPI context {}.", topologyKey.getUuid().getValue());

        return true;
    }

    private boolean updateTopology(
            TopologyKey topologyKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey nodeKey,
            OwnedNodeEdgePoint ownedNodeEdgePoint) {
        try {
            LOG.debug("Updating node {} in topology {} with OwnedNodeEdgePoint: {}",
                    nodeKey.getUuid().getValue(), topologyKey.getUuid().getValue(), ownedNodeEdgePoint);

            DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nodeId = DataObjectIdentifier
                    .builder(Context.class)
                    .augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.context.TopologyContext.class)
                    .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.context.Topology.class, topologyKey)
                    .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.Node.class, nodeKey)
                    .child(OwnedNodeEdgePoint.class, ownedNodeEdgePoint.key())
                    .build();

            this.networkTransactionService.put(LogicalDatastoreType.OPERATIONAL, nodeId, ownedNodeEdgePoint);
            this.networkTransactionService.commit().get();

            LOG.info("OwnedNodeEdgePoint uuid: {} ({}) updated in TAPI topology {} for node {}",
                    ownedNodeEdgePoint.key().getUuid().getValue(),
                    ownedNodeEdgePoint.nonnullName().values().stream()
                            .map(NameAndValue::getValue)
                            .collect(Collectors.joining()),
                    topologyKey.getUuid().getValue(),
                    nodeKey.getUuid().getValue());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed adding/updating tapi topology {} with edge point {} : {}",
                    topologyKey.getUuid().getValue(),
                    ownedNodeEdgePoint.key().getUuid(),
                    Objects.requireNonNull(ownedNodeEdgePoint.getName())
                            .values().stream().map(NameAndValue::getValue).collect(Collectors.joining()),
                    e);
        }

        return false;
    }

    /**
     * OwnNodeEdgePoints for SRG Termination Points.
     */
    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> srgOwnNodeEdgePoints(
            Map<String, Set<TerminationPoint>> srgTerminationPointList,
            ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit) {

        LOG.debug("Creating OwnedNodeEdgePoints for SRG Termination Points: {}", srgTerminationPointList);

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownNodeEdgePoints = new HashMap<>();
        for (Map.Entry<String, Set<TerminationPoint>> entries : srgTerminationPointList.entrySet()) {
            ownNodeEdgePoints.putAll(
                    convertTopoORtoTapiAtInit.populateNepsForRdmNode(
                            true,
                            entries.getKey(),
                            entries.getValue().stream().filter(
                                    tp -> (Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                                            .getTpType().getIntValue()
                                            == OpenroadmTpType.SRGTXRXPP.getIntValue()
                                            || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                                            .getTpType().getIntValue()
                                            == OpenroadmTpType.SRGRXPP.getIntValue()
                                            || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                                            .getTpType().getIntValue()
                                            == OpenroadmTpType.SRGTXPP.getIntValue())).toList(),
                            true,
                            TapiConstants.PHTNC_MEDIA_OTS,
                            entries.getKey()
                    )
            );
        }

        ownNodeEdgePoints
                .values()
                .stream()
                .map(line ->
                        String.format("Updated SRG OwnNodeEdgePointName: %s", Objects.requireNonNull(line.getName())
                                .values().stream()
                                .map(NameAndValue::getValue).collect(Collectors.joining())))
                .forEach(LOG::info);

        return ownNodeEdgePoints;
    }

    /**
     * OwnNodeEdgePoints for DEG Termination Points.
     */
    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> degOwnNodeEdgePoints(
            Map<String, Set<TerminationPoint>> degTerminationPointList,
            ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit) {

        LOG.debug("Creating OwnedNodeEdgePoints for DEG Termination Points: {}", degTerminationPointList);

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownNodeEdgePoints = new HashMap<>();
        for (Map.Entry<String, Set<TerminationPoint>> entries : degTerminationPointList.entrySet()) {
            List<TerminationPoint> list = entries.getValue().stream().filter(
                    tp -> (Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                            .getTpType().getIntValue()
                            == OpenroadmTpType.DEGREETXRXTTP.getIntValue()
                            || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                            .getTpType().getIntValue()
                            == OpenroadmTpType.DEGREERXTTP.getIntValue()
                            || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                            .getTpType().getIntValue()
                            == OpenroadmTpType.DEGREETXTTP.getIntValue())).toList();
            ownNodeEdgePoints.putAll(
                    convertTopoORtoTapiAtInit.populateNepsForRdmNode(
                            false,
                            entries.getKey(),
                            list,
                            true,
                            TapiConstants.PHTNC_MEDIA_OTS,
                            entries.getKey()
                    )
            );
            ownNodeEdgePoints.putAll(
                    convertTopoORtoTapiAtInit.populateNepsForRdmNode(
                            false,
                            entries.getKey(),
                            list,
                            false,
                            TapiConstants.PHTNC_MEDIA_OMS,
                            entries.getKey()
                    )
            );
        }

        ownNodeEdgePoints
                .values()
                .stream()
                .map(line ->
                        String.format("Updated DEG OwnNodeEdgePointName: %s", Objects.requireNonNull(line.getName())
                                .values().stream()
                                .map(NameAndValue::getValue).collect(Collectors.joining())))
                .forEach(LOG::info);

        return ownNodeEdgePoints;
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

    private ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit() {
        String topoType = TapiConstants.T0_FULL_MULTILAYER;
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(topoType.getBytes(StandardCharsets.UTF_8)).toString());
        TapiLink tapiLink = new TapiLinkImpl(this.networkTransactionService, tapiContext);
        return new ConvertTopoORtoTapiAtInit(topoUuid, tapiLink);
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createNodeEdgePointsFromOpenRoadmTerminationPoints(
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap
    ) {
        Map<String, Set<TerminationPoint>> degTerminationPointList = new HashMap<>();
        Map<String, Set<TerminationPoint>> srgTerminationPointList = new HashMap<>();

        for (Map.Entry<String, Set<String>> entrySet : nodeIdTerminationPointIdMap.entrySet()) {
            Map<NodeKey, Node> nodeMap = openRoadmTopology.stream().filter(
                    orTopology -> orTopology.getKey().equals(new NodeKey(new NodeId(entrySet.getKey())))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            nodeMap.entrySet().stream().map(nodeEntry -> String.format(
                    "OpenROADM node ID: %s",
                    nodeEntry.getValue().getNodeId().getValue()
            )).forEach(LOG::info);

            for (Map.Entry<NodeKey, Node> entry : nodeMap.entrySet()) {
                //Find the supporting node, e.g. ROADM-A1 for the node ROADM-A1-DEG1
                String supportingNodeName = Objects.requireNonNull(entry.getValue().getSupportingNode())
                        .values().stream()
                        .filter(supportingNode -> (
                                supportingNode
                                        .getNetworkRef().equals(NetworkId.getDefaultInstance("openroadm-network"))))
                        .findFirst()
                        .orElseThrow()
                        .getNodeRef().getValue();
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
                    degTerminationPointList.put(supportingNodeName, new HashSet<>(tempTerminationPointList));
                } else if (nodeType.getIntValue() == 12) {
                    srgTerminationPointList.put(supportingNodeName, new HashSet<>(tempTerminationPointList));
                }

            }
        }

        LOG.debug("\nDegree termination point(s): {}", degTerminationPointList);
        LOG.debug("\nSRG termination point(s): {}", srgTerminationPointList);

        ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit = convertTopoORtoTapiAtInit();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> newOwnNodeEdgePoints = srgOwnNodeEdgePoints(
                srgTerminationPointList,
                convertTopoORtoTapiAtInit
        );

        newOwnNodeEdgePoints.putAll(degOwnNodeEdgePoints(
                degTerminationPointList,
                convertTopoORtoTapiAtInit
        ));

        newOwnNodeEdgePoints.values().stream().map(edgePoint -> String.format(
                "OwnedNodeEdgePoint in need of an update: %s",
                Objects.requireNonNull(edgePoint.getName()).values().stream()
                        .map(NameAndValue::getValue)
                        .collect(Collectors.joining(", "))
        )).forEach(LOG::info);

        return newOwnNodeEdgePoints;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createOwnNodeEdgePoints(
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                    tapiTopologyNodes,
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap
    ) {
        if (tapiTopologyNodes.isEmpty()) {
            LOG.error("No TAPI topology nodes found, cannot update Own Node Edge Points.");
            return new HashMap<>();
        }
        return createNodeEdgePointsFromOpenRoadmTerminationPoints(
                openRoadmTopology, nodeIdTerminationPointIdMap
        );
    }

    /**
     * This method finds the nodes in the existing TAPI topology for the given newOwnNodeEdgePoints.
     */
    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> createNodeOwnEdgePoints(
                    Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> newOwnNodeEdgePoints,
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
                    tapiTopologyNodes) {

        Set<Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                        .rev221121.topology.Node>> currentTopologyNodes = tapiTopologyNodes.entrySet();

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                .rev221121.topology.NodeKey,
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedTapiNodes = new HashMap<>();

        newOwnNodeEdgePoints.forEach((key, newNodeEdgePoint) -> {
            LOG.debug("OwnedNodeEdgePoint Key: {}, Value: {}", key.getUuid().getValue(), newNodeEdgePoint);

            Name newNodeEdgePointReferenceName = Objects.requireNonNull(newNodeEdgePoint.getName())
                    .values().stream().findFirst().orElseThrow();

            for (Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                    .rev221121.topology.NodeKey,
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                            .rev221121.topology.Node> currentTapiTopologyNode : currentTopologyNodes) {
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> currentTapiNodeEdgePoints =
                        currentTapiTopologyNode.getValue().getOwnedNodeEdgePoint();

                if (currentTapiNodeEdgePoints != null) {

                    currentTapiNodeEdgePoints
                            .entrySet()
                            .stream()
                            .filter(
                                    edgePoint -> Objects.requireNonNull(
                                            edgePoint.getValue().getName()
                                    ).containsValue(newNodeEdgePointReferenceName))
                            .forEach(edgePoint -> {
                                LOG.debug("Current OwnedNodeEdgePoint: {} found when searching for {}", edgePoint,
                                        newNodeEdgePointReferenceName.getValue());
                                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
                                        .rev221121.topology.NodeKey nodeKey = currentTapiTopologyNode.getKey();
                                LOG.info("Found OwnedNodeEdgePoint {} (uuid: {}) in TAPI topology "
                                                + "on node {} (uuid: {})",
                                        newNodeEdgePointReferenceName.getValue(),
                                        newNodeEdgePoint.key().getUuid().getValue(),
                                        currentTapiTopologyNode.getValue().nonnullName()
                                                .values().stream()
                                                .map(NameAndValue::getValue)
                                                .collect(Collectors.joining()),
                                        nodeKey.getUuid().getValue());

                                if (!updatedTapiNodes.containsKey(nodeKey)) {
                                    updatedTapiNodes.put(nodeKey, new HashMap<>());
                                }
                                updatedTapiNodes.get(nodeKey).put(newNodeEdgePoint.key(), newNodeEdgePoint);
                            });
                }
            }
        });
        LOG.debug("Node OwnedNodeEdgePoints: {}", updatedTapiNodes);
        return updatedTapiNodes;
    }

}
