/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.topology.ConvertTopoORtoTapiAtInit;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.NameAndValue;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;

public class OpenRoadmTerminationPoint implements ORTerminationPoint {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenRoadmTerminationPoint.class);

    private final NetworkTransactionService networkTransactionService;

    public OpenRoadmTerminationPoint(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNodeEdgePoints(
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap,
            TapiContext tapiContext) {

        Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints = terminationPoints(
                nodeIdTerminationPointIdMap,
                openRoadmTopology
        );

        ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit = convertTopoORtoTapiAtInit(
                TapiConstants.T0_FULL_MULTILAYER, tapiContext);

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> newOwnNodeEdgePoints = srgOwnNodeEdgePoints(
                terminationPoints.get(OpenroadmNodeType.SRG),
                convertTopoORtoTapiAtInit
        );

        newOwnNodeEdgePoints.putAll(degOwnNodeEdgePoints(
                terminationPoints.get(OpenroadmNodeType.DEGREE),
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

    private Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints(
            Map<String, Set<String>> nodeIdTerminationPointIdMap,
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology) {

        Collection collection = new TerminationPointCollection();

        for (Map.Entry<String, Set<String>> entrySet : nodeIdTerminationPointIdMap.entrySet()) {

            Map<NodeKey, Node> nodeMap = openRoadmTopology.stream().filter(
                    orTopology -> orTopology.getKey().equals(new NodeKey(new NodeId(entrySet.getKey())))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            nodeMap.values().stream().map(node -> String.format(
                    "OpenROADM node ID: %s",
                    node.getNodeId().getValue()
            )).forEach(LOG::info);

            collection.add(this.terminationPointCollection(nodeMap, entrySet));
        }

        LOG.debug("Termination point(s): {}", collection.terminationPoints());

        return collection.terminationPoints(
                List.of(OpenroadmNodeType.SRG, OpenroadmNodeType.DEGREE)
        );
    }

    /**
     * Find the supporting node, e.g. ROADM-A1 for the node ROADM-A1-DEG1
     */
    private String supportingNodeName(Node node, NetworkId networkId) {
        return Objects.requireNonNull(node.getSupportingNode())
                .values().stream()
                .filter(supportingNode -> (
                        supportingNode
                                .getNetworkRef().equals(networkId)))
                .findFirst()
                .orElseThrow()
                .getNodeRef().getValue();
    }

    private Collection terminationPointCollection(
            Map<NodeKey, Node> nodeMap, Map.Entry<String, Set<String>> nodeIdTerminationPointIdMap) {

        Collection collection = new TerminationPointCollection();

        for (Map.Entry<NodeKey, Node> entry : nodeMap.entrySet()) {
            Node node = entry.getValue();
            String supportingNodeName = supportingNodeName(node, NetworkId.getDefaultInstance("openroadm-network"));
            Node1 node1 = node.augmentationOrElseThrow(Node1.class);

            List<TerminationPoint> node1TpValues = Objects.requireNonNull(node1.getTerminationPoint())
                    .values().stream().toList();

            List<TerminationPoint> tempTerminationPointList = new ArrayList<>();
            for (String tpId : nodeIdTerminationPointIdMap.getValue()) {
                LOG.info("Processing Termination Point ID: {}", tpId);
                tempTerminationPointList.addAll(node1TpValues.stream()
                        .filter(tp -> tp.getTpId().equals(TpId.getDefaultInstance(tpId)))
                        .toList());
            }

            OpenroadmNodeType nodeType = node.augmentationOrElseThrow(org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev230526.Node1.class).getNodeType();

            collection.add(nodeType, supportingNodeName, new HashSet<>(tempTerminationPointList));
        }

        return collection;
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
                            entries.getValue().stream().filter(this::terminationPointIsSRG).toList(),
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
     * Determines if the Termination Point is a SRG type.
     */
    boolean terminationPointIsSRG(TerminationPoint tp) {
        return Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                .getTpType().getIntValue()
                == OpenroadmTpType.SRGTXRXPP.getIntValue()
                || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                .getTpType().getIntValue()
                == OpenroadmTpType.SRGRXPP.getIntValue()
                || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                .getTpType().getIntValue()
                == OpenroadmTpType.SRGTXPP.getIntValue();
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
            List<TerminationPoint> list = entries.getValue().stream().filter(this::terminationPointIsDegree).toList();
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

        logUpdatedNodes(ownNodeEdgePoints);

        return ownNodeEdgePoints;
    }

    private boolean terminationPointIsDegree(TerminationPoint tp) {
        return Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                .getTpType().getIntValue()
                == OpenroadmTpType.DEGREETXRXTTP.getIntValue()
                || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                .getTpType().getIntValue()
                == OpenroadmTpType.DEGREERXTTP.getIntValue()
                || Objects.requireNonNull(tp.augmentation(TerminationPoint1.class))
                .getTpType().getIntValue()
                == OpenroadmTpType.DEGREETXTTP.getIntValue();
    }

    private void logUpdatedNodes(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownNodeEdgePoints) {
        ownNodeEdgePoints
                .values()
                .stream()
                .map(line ->
                        String.format("Updated DEG OwnNodeEdgePointName: %s", Objects.requireNonNull(line.getName())
                                .values().stream()
                                .map(NameAndValue::getValue).collect(Collectors.joining())))
                .forEach(LOG::info);
    }

    private ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit(String topoType, TapiContext tapiContext) {
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(topoType.getBytes(StandardCharsets.UTF_8)).toString());
        TapiLink tapiLink = new TapiLinkImpl(this.networkTransactionService, tapiContext);
        return new ConvertTopoORtoTapiAtInit(topoUuid, tapiLink);
    }
}
