/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;

/**
 * Factory for creating {@link TerminationPointMapping} instances.
 *
 * <p>A {@code TerminationPointMapping} captures the relationship between an
 * OpenROADM termination point (typically identified by {@code nodeId + tpId}
 * and its resolved {@link TerminationPointId}) and the corresponding TAPI
 * owned node edge points (NEPs) represented by {@link OwnedNodeEdgePointName}s.
 *
 * <p>Implementations typically:
 * <ul>
 *   <li>resolve the OpenROADM termination point type (e.g. SRG-PP / DEG-TTP / XPDR)
 *       from a provided OpenROADM {@link Network} topology snapshot</li>
 *   <li>determine which TAPI photonic sublayers apply</li>
 *   <li>derive one or more NEP names for the termination point</li>
 * </ul>
 *
 * <p>Bulk factory methods are provided to efficiently create mappings from
 * topology change events.
 */
public interface TerminationPointMappingFactory {

    /**
     * Creates a mapping for a single OpenROADM termination point.
     *
     * <p>The provided {@link Network} is used to resolve termination point metadata
     * (including {@link org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType}).
     *
     * @param nodeId OpenROADM node identifier (e.g. "ROADM-C1")
     * @param tpId OpenROADM termination point identifier (e.g. "SRG1-PP1-TXRX")
     * @param network OpenROADM topology snapshot used to resolve the termination point type
     * @return mapping for the given termination point; never {@code null}
     * @throws NullPointerException if {@code nodeId}, {@code tpId}, or {@code network} is {@code null}
     * @throws IllegalArgumentException if the termination point cannot be resolved or is unsupported
     * @throws TpTypeResolutionException if resolution fails due to missing node/TP/augmentation/type in {@code network}
     */
    TerminationPointMapping create(String nodeId, String tpId, Network network);

    /**
     * Creates mappings for a set of topology changes.
     *
     * <p>The provided map typically comes from {@link TopologyUpdateResult} and contains
     * one entry per changed termination point. Implementations may ignore changes that
     * do not correspond to a supported OpenROADM termination point type.
     *
     * @param topologyChangesMap map of topology changes keyed by {@link TopologyChangesKey}
     * @param network OpenROADM topology snapshot used to resolve termination point types
     * @return set of mappings derived from the provided changes; never {@code null}
     * @throws NullPointerException if {@code topologyChangesMap} or {@code network} is {@code null}
     */
    Set<TerminationPointMapping> create(Map<TopologyChangesKey, TopologyChanges> topologyChangesMap, Network network);

    /**
     * Creates mappings from a topology update result.
     *
     * <p>This is a convenience overload that extracts the non-null topology changes
     * from {@code topologyUpdateResult} and delegates to
     * {@link #create(Map, Network)}.
     *
     * @param topologyUpdateResult topology update result; may be {@code null}
     * @param network OpenROADM topology snapshot used to resolve termination point types
     * @return set of mappings derived from the result, or an empty set if {@code topologyUpdateResult} is {@code null}
     * @throws NullPointerException if {@code network} is {@code null}
     */
    Set<TerminationPointMapping> create(TopologyUpdateResult topologyUpdateResult, Network network);

}
