/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Value object representing a resolved TAPI NEP identifier together with its derivation inputs.
 *
 * <p>This record is intended to bridge two concerns:
 * <ul>
 *   <li><b>Datastore addressing</b>: {@link #iid()} is the {@link DataObjectIdentifier.WithKey} required by
 *       MD-SAL repositories.</li>
 *   <li><b>Observability</b>: {@link #topologySeed()}, {@link #nodeSeed()}, and {@link #nepSeed()} retain the
 *       semantic strings used to derive deterministic UUIDs, which is useful for logging and debugging.</li>
 * </ul>
 *
 * <p>The UUIDs ({@link #topologyUuid()}, {@link #nodeUuid()}, {@link #nepUuid()}) are name-based UUIDs derived
 * from the seed strings using {@link java.util.UUID#nameUUIDFromBytes(byte[])} (via {@link TapiIdentifierFactory}).
 *
 * <p>{@link #toLogString()} provides a compact, human-friendly representation based on the seed strings.
 *
 * @param iid instance identifier used to read/write the {@link OwnedNodeEdgePoint} in the datastore
 * @param topologySeed seed string used to derive {@code topologyUuid} (often a stable topology name)
 * @param nodeSeed seed string used to derive {@code nodeUuid} (e.g. {@code "<supportingNodeId>+<layer>"})
 * @param nepSeed seed string used to derive {@code nepUuid} (typically the NEP name)
 * @param topologyUuid derived topology UUID
 * @param nodeUuid derived node UUID
 * @param nepUuid derived NEP UUID
 */
public record NepIdentifier(
        DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> iid,
        String topologySeed,
        String nodeSeed,
        String nepSeed,
        Uuid topologyUuid,
        Uuid nodeUuid,
        Uuid nepUuid) {

    /**
     * Returns a compact, human-friendly string suitable for log messages.
     *
     * <p>This representation prefers semantic seed strings over UUIDs to keep logs readable.
     *
     * @return log-friendly string
     */
    public String toLogString() {
        return "NEP[Topology=%s, Node=%s, NEP=%s]"
                .formatted(topologySeed, nodeSeed, nepSeed);
    }

    @Override
    public String toString() {
        return "NEP[topologySeed=%s, nodeSeed=%s, nepSeed=%s, topoUuid=%s, nodeUuid=%s, nepUuid=%s]"
                .formatted(topologySeed, nodeSeed, nepSeed,
                        topologyUuid.getValue(), nodeUuid.getValue(), nepUuid.getValue());
    }
}
