/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.topology.ORTopology;
import org.opendaylight.transportpce.tapi.topology.ConvertTopoORtoTapiAtInit;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.NameAndValue;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;

public class OpenRoadmTerminationPoint implements ORTerminationPoint {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenRoadmTerminationPoint.class);

    private final NetworkTransactionService networkTransactionService;

    private final NodeTerminationPoint nodeIdTerminationPointIdMap;

    private final ORTopology openRoadmTopology;

    /**
     * Constructor for OpenRoadmTerminationPoint.
     * @param networkTransactionService the network transaction service
     * @param nodeIdTerminationPointIdMap A map of node IDs to their termination point IDs.
     *                                    e.g. ROADM-A1-DEG1 -> [DEG1-CTP-TXRX, DEG1-TTP-TXRX]
     * @param openRoadmTopology The OpenROADM topology.
     */
    public OpenRoadmTerminationPoint(
            NetworkTransactionService networkTransactionService,
            NodeTerminationPoint nodeIdTerminationPointIdMap,
            ORTopology openRoadmTopology) {

        this.networkTransactionService = networkTransactionService;
        this.nodeIdTerminationPointIdMap = nodeIdTerminationPointIdMap;
        this.openRoadmTopology = openRoadmTopology;
    }

    @Override
    public Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNodeEdgePoints(TapiContext tapiContext) {
        NodeTypeCollection terminationPoints = openRoadmTopology.terminationPoints(nodeIdTerminationPointIdMap);

        ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit = convertTopoORtoTapiAtInit(
                TapiConstants.T0_FULL_MULTILAYER, tapiContext);

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> newOwnNodeEdgePoints = srgOwnedNodeEdgePoints(
                terminationPoints,
                convertTopoORtoTapiAtInit
        );

        newOwnNodeEdgePoints.putAll(degreeOwnedNodeEdgePoints(
                terminationPoints,
                convertTopoORtoTapiAtInit
        ));

        newOwnNodeEdgePoints.values().stream().map(edgePoint -> String.format(
                "OwnedNodeEdgePoint(s) created: %s",
                Objects.requireNonNull(edgePoint.getName()).values().stream()
                        .map(NameAndValue::getValue)
                        .collect(Collectors.joining(", "))
        )).forEach(LOG::info);

        return newOwnNodeEdgePoints;
    }

    /**
     * OwnNodeEdgePoints for SRG Termination Points.
     */
    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> srgOwnedNodeEdgePoints(
            NodeTypeCollection srgTerminationPointsCollection,
            ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit) {

        Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> srgTp =
                srgTerminationPointsCollection.terminationPoints(OpenroadmNodeType.SRG);
        LOG.debug("Creating OwnedNodeEdgePoints for SRG Termination Points: {}", srgTp);

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNodeEdgePoints = new HashMap<>();
        for (Map.Entry<String, Set<TerminationPoint>> entries : srgTp.get(OpenroadmNodeType.SRG).entrySet()) {
            ownedNodeEdgePoints.putAll(
                    convertTopoORtoTapiAtInit.populateNepsForRdmNode(
                            true,
                            entries.getKey(),
                            entries.getValue().stream().filter(this::isSrg).toList(),
                            true,
                            TapiConstants.PHTNC_MEDIA_OTS,
                            entries.getKey()
                    )
            );
        }

        ownedNodeEdgePoints
                .values()
                .stream()
                .map(line ->
                        String.format("Updated SRG OwnNodeEdgePointName: %s", Objects.requireNonNull(line.getName())
                                .values().stream()
                                .map(NameAndValue::getValue).collect(Collectors.joining())))
                .forEach(LOG::info);

        return ownedNodeEdgePoints;
    }

    /**
     * Determines if the Termination Point is a SRG type.
     */
    boolean isSrg(TerminationPoint tp) {
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
    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> degreeOwnedNodeEdgePoints(
            NodeTypeCollection degreeTerminationPoinCollection,
            ConvertTopoORtoTapiAtInit convertTopoORtoTapiAtInit) {

        Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> degreeTp =
                degreeTerminationPoinCollection.terminationPoints(OpenroadmNodeType.DEGREE);
        LOG.debug("Creating OwnedNodeEdgePoints for DEG Termination Points: {}", degreeTp);

        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownNodeEdgePoints = new HashMap<>();
        for (Map.Entry<String, Set<TerminationPoint>> entries : degreeTp.get(OpenroadmNodeType.DEGREE).entrySet()) {

            List<TerminationPoint> list = entries.getValue().stream().filter(this::isDegree).toList();
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

    private boolean isDegree(TerminationPoint tp) {
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
