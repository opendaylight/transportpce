/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicLayer;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.DefaultNepPhotonicLayerMapper;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.NepPhotonicLayerMapper;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointName;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Factory for building {@link DataObjectIdentifier}s for TAPI topology objects.
 *
 * <p>This class primarily generates instance identifiers (IIDs) for
 * {@link OwnedNodeEdgePoint}s (NEPs) in the TAPI topology subtree.
 *
 * <h2>UUID strategy</h2>
 *
 * <p>The factory supports two usage styles:
 * <ul>
 *   <li><b>Name-based generation</b> – derive {@link Uuid}s from semantic strings
 *       such as topology name, supporting node id and NEP name.</li>
 *   <li><b>Explicit UUIDs</b> – build identifiers directly from already-known UUID strings.</li>
 * </ul>
 *
 * <p>Name-based UUIDs are generated using {@link UUID#nameUUIDFromBytes(byte[])}
 * with the configured {@link Charset} (UTF-8 by default). This makes the identifiers deterministic:
 * the same input strings always produce the same UUIDs.
 *
 * <h2>Node UUID derivation and photonic layer grouping</h2>
 *
 * <p>For convenience, {@link #nepIid(String, TerminationPointId, OwnedNodeEdgePointName)}
 * derives the node UUID from the OpenROADM supporting node id together with a
 * higher-level {@link NepPhotonicLayer} bucket:
 *
 * <pre>{@code
 * nodeUuid = uuid("<supportingNodeId>+<NepPhotonicLayer>")
 * }</pre>
 *
 * <p>The photonic layer bucket is resolved from the NEP photonic sublayer using
 * a {@link NepPhotonicLayerMapper}. This avoids hardcoding string constants
 * (such as {@code "PHOTONIC_MEDIA"}) in identifier construction logic.
 *
 * <p>In addition to returning datastore identifiers, the factory can also return a {@link NepIdentifier}
 * which retains the semantic seed strings used for UUID derivation. This is useful for logging and
 * troubleshooting, since the deterministic UUIDs are not human-friendly by themselves.
 *
 * <p>This class does not access the datastore. It only creates identifiers
 * that can be used by repository/datastore components.
 */
public class TapiIdentifierFactory {

    private final NepPhotonicLayerMapper nepPhotonicLayerMapper;

    /**
     * Charset used when converting UUID seed strings to bytes for name-based UUID generation.
     * Defaults to UTF-8.
     */
    private final Charset charset;

    public TapiIdentifierFactory() {
        this(new DefaultNepPhotonicLayerMapper(), StandardCharsets.UTF_8);
    }

    public TapiIdentifierFactory(Charset charset) {
        this(new DefaultNepPhotonicLayerMapper(), charset);
    }

    public TapiIdentifierFactory(NepPhotonicLayerMapper nepPhotonicLayerMapper) {
        this(nepPhotonicLayerMapper,  StandardCharsets.UTF_8);
    }

    public TapiIdentifierFactory(NepPhotonicLayerMapper nepPhotonicLayerMapper, Charset charset) {
        this.nepPhotonicLayerMapper = Objects.requireNonNull(nepPhotonicLayerMapper, "nepPhotonicLayerMapper");
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    /**
     * Builds a NEP instance identifier from semantic inputs.
     *
     * <p>This method derives deterministic UUIDs from:
     * <ul>
     *   <li>the topology UUID seed string</li>
     *   <li>the supporting node id + derived {@link NepPhotonicLayer}</li>
     *   <li>the {@link OwnedNodeEdgePointName}</li>
     * </ul>
     *
     * @param topologyUuidStr
     *     topology UUID seed string (typically a stable topology name)
     * @param terminationPointId
     *     OpenROADM termination point identity (used for supporting node id)
     * @param ownedNodeEdgePointName
     *     NEP name (used as UUID seed and for photonic sublayer)
     * @return deterministic NEP instance identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    public DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepIid(
            String topologyUuidStr,
            TerminationPointId terminationPointId,
            OwnedNodeEdgePointName ownedNodeEdgePointName) {

        Objects.requireNonNull(topologyUuidStr, "topologyUuidStr");
        Objects.requireNonNull(terminationPointId, "terminationPointId");
        Objects.requireNonNull(ownedNodeEdgePointName, "ownedNodeEdgePointName");

        return nepIdentifier(topologyUuidStr, terminationPointId, ownedNodeEdgePointName).iid();
    }

    /**
     * Builds a NEP instance identifier from explicit UUID strings.
     *
     * <p>This overload assumes the provided strings are already valid UUID values (not seed strings),
     * for example:
     *
     * <pre>{@code
     * topologyUuidStr = "550e8400-e29b-41d4-a716-446655440000"
     * nodeUuidStr     = "8f14e45f-ea0f-4f6a-93b6-7a7c4d9c6e44"
     * nepUuidStr      = "d3b07384-d9a6-44f8-a9f1-0c54b0d3a2c1"
     * }</pre>
     *
     * <p>No hashing/name-based derivation is performed by this overload.
     *
     * @param topologyUuidStr topology UUID string
     * @param nodeUuidStr node UUID string
     * @param nepUuidStr NEP UUID string
     * @return NEP instance identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    public DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepIid(
            String topologyUuidStr,
            String nodeUuidStr,
            String nepUuidStr) {

        Objects.requireNonNull(topologyUuidStr, "topologyUuidStr");
        Objects.requireNonNull(nodeUuidStr, "nodeUuidStr");
        Objects.requireNonNull(nepUuidStr, "nepUuidStr");

        return nepIid(
                new Uuid(topologyUuidStr),
                new Uuid(nodeUuidStr),
                new Uuid(nepUuidStr));
    }

    /**
     * Builds a NEP instance identifier from {@link Uuid} objects.
     *
     * @param topoUuid topology UUID
     * @param nodeUuid node UUID
     * @param nepUuid NEP UUID
     * @return NEP instance identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    public DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepIid(
            Uuid topoUuid,
            Uuid nodeUuid,
            Uuid nepUuid) {

        Objects.requireNonNull(topoUuid, "topoUuid");
        Objects.requireNonNull(nodeUuid, "nodeUuid");
        Objects.requireNonNull(nepUuid, "nepUuid");

        return DataObjectIdentifier
                .builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topoUuid))
                .child(Node.class, new NodeKey(nodeUuid))
                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                .build();
    }

    /**
     * Builds a {@link NepIdentifier} for a NEP derived from semantic inputs.
     *
     * <p>This method is the logging-friendly counterpart to
     * {@link #nepIid(String, TerminationPointId, OwnedNodeEdgePointName)}. It returns both:
     * <ul>
     *   <li>the computed {@link DataObjectIdentifier.WithKey} used to read/write the datastore</li>
     *   <li>the semantic seed strings used for deterministic UUID derivation</li>
     * </ul>
     *
     * <p>UUIDs are derived deterministically using {@link UUID#nameUUIDFromBytes(byte[])} with the
     * configured {@link Charset}.
     *
     * <p>The derived seeds are:
     * <ul>
     *   <li><b>topologySeed</b>: {@code topologyUuidStr} (a seed string, often a stable topology name)</li>
     *   <li><b>nodeSeed</b>: {@code "<supportingNodeId>+<NepPhotonicLayer>"}</li>
     *   <li><b>nepSeed</b>: {@code ownedNodeEdgePointName.toString()}</li>
     * </ul>
     *
     * @param topologyUuidStr topology UUID seed string (typically a stable topology name)
     * @param terminationPointId resolved OpenROADM termination point identity (used for supporting node id)
     * @param ownedNodeEdgePointName NEP name (used as UUID seed and for photonic sublayer)
     * @return a {@link NepIdentifier} containing the IID, seeds, and derived UUIDs; never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public NepIdentifier nepIdentifier(
            String topologyUuidStr,
            TerminationPointId terminationPointId,
            OwnedNodeEdgePointName ownedNodeEdgePointName) {

        Objects.requireNonNull(topologyUuidStr, "topologyUuidStr");
        Objects.requireNonNull(terminationPointId, "terminationPointId");
        Objects.requireNonNull(ownedNodeEdgePointName, "ownedNodeEdgePointName");

        NepPhotonicLayer layer = nepPhotonicLayerMapper.layerOf(ownedNodeEdgePointName.nepPhotonicSublayer());

        String topoSeed = topologyUuidStr;
        String nodeSeed = "%s+%s".formatted(terminationPointId.supportingNodeId(), layer);
        String nepSeed  = ownedNodeEdgePointName.toString();

        Uuid topoUuid = uuid(topoSeed);
        Uuid nodeUuid = uuid(nodeSeed);
        Uuid nepUuid  = uuid(nepSeed);

        return new NepIdentifier(
                nepIid(topoUuid, nodeUuid, nepUuid),
                topoSeed, nodeSeed, nepSeed,
                topoUuid, nodeUuid, nepUuid
        );
    }

    /**
     * Computes a deterministic TAPI {@link Uuid} from a seed string using name-based UUID derivation.
     *
     * @param str seed string
     * @return deterministic UUID
     * @throws NullPointerException if {@code str} is {@code null}
     */
    private Uuid uuid(String str) {
        Objects.requireNonNull(str, "str");
        return new Uuid(UUID.nameUUIDFromBytes(str.getBytes(charset)).toString());
    }
}
