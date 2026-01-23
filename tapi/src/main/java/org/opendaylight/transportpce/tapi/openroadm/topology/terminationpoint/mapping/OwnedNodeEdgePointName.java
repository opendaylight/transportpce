/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;

/**
 * Value object representing a TAPI {@code owned-node-edge-point} {@link Name} along with its
 * associated {@link NepPhotonicSublayer}.
 *
 * <p>This class is primarily used when translating OpenROADM termination points into TAPI topology
 * objects. A single OpenROADM termination point may correspond to multiple TAPI owned node edge points,
 * typically one per photonic sublayer.
 *
 * @param value
 *     the TAPI {@link Name} representing the owned node edge point name (value + valueName)
 * @param nepPhotonicSublayer
 *     the photonic sublayer that this owned node edge point corresponds to
 */
public record OwnedNodeEdgePointName(Name value, NepPhotonicSublayer nepPhotonicSublayer) {

    public OwnedNodeEdgePointName {
        Objects.requireNonNull(value);

        Objects.requireNonNull(value.getValue());
        if (value.getValue().isBlank()) {
            throw new IllegalArgumentException("OwnedNodeEdgePointName value cannot be blank");
        }

        Objects.requireNonNull(value.getValueName());
        if (value.getValueName().isBlank()) {
            throw new IllegalArgumentException("OwnedNodeEdgePointName value name cannot be blank");
        }
    }

    /**
     * Creates the set of TAPI {@link OwnedNodeEdgePointName}s corresponding to a given
     * OpenROADM termination point on a node.
     *
     * <p>A single OpenROADM termination point may map to multiple TAPI owned node edge points,
     * typically due to different photonic sublayers. The resulting set contains one
     * {@code OwnedNodeEdgePointName} per applicable photonic sublayer.
     *
     * @param nodeId
     *     the OpenROADM node identifier (e.g. {@code ROADM-A})
     * @param tpId
     *     the OpenROADM termination point identifier on the node
     *     (e.g. {@code SRG1-PP1-TXRX})
     * @param applicableSublayers
     *     the applicable correspondig photonic sublayers for the termination point
     *     (e.g. NepPhotonicSublayer.OTSI_MC, NepPhotonicSublayer.MC, NepPhotonicSublayer.PHTNC_MEDIA_OTS)
     * @return
     *     a set of {@link OwnedNodeEdgePointName}s corresponding to the given
     *     OpenROADM termination point
     */
    public static Set<OwnedNodeEdgePointName> create(
            String nodeId,
            String tpId,
            Set<NepPhotonicSublayer> applicableSublayers) {

        Set<OwnedNodeEdgePointName> ownedNodeEdgePointNames = new LinkedHashSet<>();
        applicableSublayers.forEach(subLayer -> {
            ownedNodeEdgePointNames.add(OwnedNodeEdgePointName.create(nodeId, subLayer, tpId));
        });

        return ownedNodeEdgePointNames;
    }

    /**
     * Creates a single {@link OwnedNodeEdgePointName} for the given OpenROADM node and termination point
     * for a specific photonic sublayer.
     *
     * <p>The created TAPI {@link Name} uses a value format on the form:
     * {@code nodeId + "+" + sublayer + "+" + tpId}.
     *
     * @param nodeId
     *     the OpenROADM node identifier (e.g. {@code ROADM-A})
     * @param nepPhotonicSublayer
     *     the photonic sublayer that the owned node edge point represents
     * @param tpId
     *     the OpenROADM termination point identifier on the node (e.g. {@code ROADM-A-SRG1})
     * @return
     *     an {@link OwnedNodeEdgePointName} instance containing the generated TAPI {@link Name}
     * @throws NullPointerException
     *     if any input is {@code null}
     * @throws IllegalArgumentException
     *     if {@code nodeId} or {@code tpId} are blank
     */
    public static OwnedNodeEdgePointName create(String nodeId, NepPhotonicSublayer nepPhotonicSublayer, String tpId) {
        Objects.requireNonNull(nepPhotonicSublayer);
        nodeId = requireNonBlank(nodeId, "OwnedNodeEdgePointName nodeId");
        tpId = requireNonBlank(tpId, "OwnedNodeEdgePointName tpId");

        return new OwnedNodeEdgePointName(
                new NameBuilder()
                        .setValueName(nepPhotonicSublayer.tapiName() + "NodeEdgePoint")
                        .setValue("%s+%s+%s".formatted(nodeId, nepPhotonicSublayer.photonic, tpId))
                        .build(),
                nepPhotonicSublayer
        );
    }

    /**
     * Creates the set of {@link OwnedNodeEdgePointName}s corresponding to the given termination point id.
     *
     * <p>This overload is a convenience wrapper around {@link #create(String, String, Set)} that extracts
     * required identifiers from a {@link TerminationPointId}.
     *
     * @param terminationPointId
     *     termination point identifier containing the supporting node id and termination point id
     * @param applicableSublayers
     *     the applicable photonic sublayers for the termination point
     * @return
     *     a set of {@link OwnedNodeEdgePointName}s corresponding to the given termination point id
     * @throws NullPointerException
     *     if any input is {@code null}
     */
    public static Set<OwnedNodeEdgePointName> create(
            TerminationPointId terminationPointId,
            Set<NepPhotonicSublayer> applicableSublayers) {

        Set<OwnedNodeEdgePointName> ownedNodeEdgePointNames = new LinkedHashSet<>();
        applicableSublayers.forEach(subLayer -> {
            ownedNodeEdgePointNames.add(OwnedNodeEdgePointName.create(
                    terminationPointId.supportingNodeId(),
                    subLayer,
                    terminationPointId.tpId()));
        });

        return ownedNodeEdgePointNames;
    }

    /**
     * Validates that a string is not {@code null} or blank.
     *
     * @param string
     *     the string to validate
     * @param what
     *     a human-readable name used for exception messages
     * @return
     *     the original string
     * @throws NullPointerException
     *     if {@code string} is {@code null}
     * @throws IllegalArgumentException
     *     if {@code string} is blank
     */
    private static String requireNonBlank(String string, String what) {
        Objects.requireNonNull(string, what + " cannot be null");

        if (string.isBlank()) {
            throw new IllegalArgumentException(what + " cannot be blank");
        }

        return string;
    }

    /**
     * Returns the underlying TAPI {@link Name} value.
     *
     * <p>This is useful when logging or when building other TAPI identifiers that reference
     * the owned node edge point by name.
     */
    @Override
    public String toString() {
        return value.getValue();
    }

    public String asString() {
        return value.getValue();
    }
}
