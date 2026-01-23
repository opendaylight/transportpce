/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;

/**
 * Creates {@link TerminationPointMapping}s by translating OpenROADM termination points
 * into their corresponding TAPI NEP representations.
 *
 * <h2>How the mapping is derived</h2>
 *
 * <p>This factory performs the following steps:
 * <ol>
 *   <li>Resolve a {@link TerminationPointId} (including {@code OpenroadmTpType}) from
 *       the OpenROADM {@code nodeId + tpId} using {@link OpenRoadmTpTypeResolver} and
 *       a provided OpenROADM {@link Network} topology snapshot.</li>
 *   <li>Map the resolved OpenROADM TP type to the applicable set of TAPI photonic sublayers
 *       using {@link TapiPhotonicSublayerMapper}.</li>
 *   <li>Create one {@link OwnedNodeEdgePointName} per applicable sublayer and return a
 *       {@link TerminationPointMapping} containing the termination point id and derived NEP names.</li>
 * </ol>
 *
 * <h2>Strict vs. lenient behavior</h2>
 *
 * <p>{@link #create(String, String, Network)} is strict: it uses the mapper's strict API and will fail
 * fast for unsupported termination point types.
 *
 * <p>{@link #create(Map, Network)} is lenient: it uses
 * {@link TapiPhotonicSublayerMapper#optionalNepPhotonicSublayer(OpenroadmTpType, Set)} (OpenroadmTpType)}
 * and silently skips unsupported termination point types.
 *
 * <p>{@link #create(TopologyUpdateResult, Network)} is a convenience overload that delegates to
 * {@link #create(Map, Network)} and returns an empty set when the input is {@code null}.
 */
public class OpenRoadmToTapiTerminationPointMappingFactory implements TerminationPointMappingFactory {

    private final OpenRoadmTpTypeResolver openRoadmTpTypeResolver;

    private final TapiPhotonicSublayerMapper photonicSublayerMapper;

    /**
     * Creates a new mapping factory.
     *
     * @param openRoadmTpTypeResolver resolver used to derive {@link TerminationPointId} (including TP type)
     *     from {@code nodeId + tpId}
     * @param photonicSublayerMapper mapper translating OpenROADM TP type to applicable TAPI photonic sublayers
     * @throws NullPointerException if any argument is {@code null}
     */
    public OpenRoadmToTapiTerminationPointMappingFactory(
            OpenRoadmTpTypeResolver openRoadmTpTypeResolver,
            TapiPhotonicSublayerMapper photonicSublayerMapper) {

        this.openRoadmTpTypeResolver = openRoadmTpTypeResolver;
        this.photonicSublayerMapper = photonicSublayerMapper;
    }

    /** {@inheritDoc} */
    @Override
    public Set<TerminationPointMapping> create(
            Map<TopologyChangesKey, TopologyChanges> topologyChangesMap,
            Network network) {

        Set<TerminationPointMapping> terminationPointMappings = new HashSet<>();
        topologyChangesMap.values().forEach(changes -> {
            String nodeId = changes.getNodeId();
            String tpId = changes.getTpId();
            TerminationPointId terminationPointId = openRoadmTpTypeResolver.terminationPointId(nodeId, tpId, network);

            Optional<Set<NepPhotonicSublayer>> applicableSublayers =
                    photonicSublayerMapper.optionalNepPhotonicSublayer(terminationPointId.openRoadmTpType());

            applicableSublayers.ifPresent(nepPhotonicSublayers -> terminationPointMappings.add(
                    new TerminationPointMapping(
                            terminationPointId,
                            OwnedNodeEdgePointName.create(terminationPointId, nepPhotonicSublayers)
                    )
            ));
        });

        return terminationPointMappings;
    }

    /** {@inheritDoc} */
    @Override
    public TerminationPointMapping create(String nodeId, String tpId, Network network) {
        TerminationPointId terminationPointId = openRoadmTpTypeResolver.terminationPointId(nodeId, tpId, network);
        Set<NepPhotonicSublayer> applicableSublayers = photonicSublayerMapper.nepPhotonicSublayer(
                terminationPointId.openRoadmTpType());

        return new TerminationPointMapping(
                terminationPointId,
                OwnedNodeEdgePointName.create(terminationPointId, applicableSublayers)
        );
    }

    /** {@inheritDoc} */
    @Override
    public Set<TerminationPointMapping> create(TopologyUpdateResult topologyUpdateResult, Network network) {
        if (topologyUpdateResult == null) {
            return Set.of();
        }
        return create(topologyUpdateResult.nonnullTopologyChanges(), network);
    }
}
