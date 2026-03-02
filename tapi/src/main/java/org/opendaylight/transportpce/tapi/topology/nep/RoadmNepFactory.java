/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.topology.nep;

import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;

public interface RoadmNepFactory {

    /**
     * Populates (builds) and returns a map of TAPI {@link OwnedNodeEdgePoint}s (NEPs) for a given ROADM node.
     *
     * @see #populateNepsForRdmNode(boolean, String, List, boolean, String, TapiLink, int)
     */
    Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            boolean srg,
            String nodeId,
            List<TerminationPoint> tpList,
            boolean withSip,
            String nepPhotonicSublayer,
            TapiLink tapiLink);

    /**
     * Populates (builds) and returns a map of TAPI {@link OwnedNodeEdgePoint}s (NEPs) for a given ROADM node.
     *
     * <p>For each {@link TerminationPoint} entry in {@code tpMap}, this method:
     * <ul>
     *   <li>Builds a Photonic Media NEP with a deterministic UUID derived from
     *       {@code nodeId + nepPhotonicSublayer + tpId}.</li>
     *   <li>Sets common attributes such as administrative/operational state, lifecycle state,
     *       direction, and supported CEP layer protocol qualifier instances based on {@code nepPhotonicSublayer}.</li>
     *   <li>If {@code nepPhotonicSublayer} is not {@code MC} nor {@code OTSI_MC}, retrieves used/available frequency
     *       information from the datastore (depending on TP type) and attaches the corresponding photonic spec
     *       via {@code tapiFactory.addPhotSpecToRoadmOnep(...)}.</li>
     *   <li>For SRG-related TPs, if used frequencies are found, recursively creates additional NEPs for the
     *       {@code MC} and {@code OTSI_MC} sublayers for the same TP.</li>
     *   <li>If {@code withSip} is {@code true}, creates a CEP for the NEP (typically for SRG OTS) using
     *       {@code tapiFactory.createCepRoadm(...)} and stores it in {@code srgOtsCepMap} under a deterministic
     *       UUID-key mapping.</li>
     * </ul>
     *
     * @param srg
     *     Indicates whether the processing context is SRG-related; forwarded to CEP creation.
     * @param nodeId
     *     The ROADM node identifier used to compose NEP names and UUIDs.
     * @param tpList
     *     List of termination point identifiers to their corresponding {@link TerminationPoint} objects.
     * @param withSip
     *     When {@code true}, creates and augments the NEP with a CEP and stores it into {@code srgOtsCepMap}.
     * @param nepPhotonicSublayer
     *     The photonic sublayer to build NEPs for (e.g. {@code PHTNC_MEDIA_OTS}, {@code PHTNC_MEDIA_OMS},
     *     {@code MC}, {@code OTSI_MC}). Affects qualifiers and whether photonic specs are added.
     * @param depth
     *     Indicates the current recursion level of the method.
     *     {@code 0} represents the top-level call; higher values indicate
     *     nested recursive invocations.
     * @param tapiLink
     *     Used to set administrative and operational status.
     * @return
     *     A map keyed by {@link OwnedNodeEdgePointKey} containing the constructed {@link OwnedNodeEdgePoint}s.
     */
    Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            boolean srg,
            String nodeId,
            List<TerminationPoint> tpList,
            boolean withSip,
            String nepPhotonicSublayer,
            TapiLink tapiLink,
            int depth);
}
