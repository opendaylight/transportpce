/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.changes;

import java.util.Set;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointSpectrumCapability;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMapping;

/**
 * Extracts derived TAPI-related artifacts from OpenROADM topology change information.
 *
 * <p>Implementations typically take a set of {@link TerminationPointMapping}s (i.e. OpenROADM
 * termination points resolved to one-or-more TAPI NEP names) and enrich them with additional
 * information read from the datastore/topology model (for example spectrum ranges) to build
 * objects needed for updating the TAPI topology.
 */
public interface TopologyChangesExtractor {

    /**
     * Builds NEP spectrum-capability information for the provided termination point mappings.
     *
     * <p>For each {@link TerminationPointMapping}, implementations generally:
     * <ol>
     *   <li>Read the corresponding OpenROADM {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
     *       .ietf.network.topology.rev180226.networks.network.node.TerminationPoint} (if available)</li>
     *   <li>Extract occupied/available spectrum (or default to empty when missing)</li>
     *   <li>Construct a TAPI spectrum capability PAC for each mapped NEP name</li>
     * </ol>
     *
     * @param terminationPointMappings mappings for OpenROADM termination points to TAPI NEP names
     * @return a set of spectrum capability objects, one per NEP name in the mappings; never {@code null}
     * @throws NullPointerException if {@code terminationPointMappings} is {@code null}
     */
    Set<OwnedNodeEdgePointSpectrumCapability> spectrumCapabilityPacs(
            Set<TerminationPointMapping> terminationPointMappings);

}
