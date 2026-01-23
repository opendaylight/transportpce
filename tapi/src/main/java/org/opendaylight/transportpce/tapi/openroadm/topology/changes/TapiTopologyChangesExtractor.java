/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.changes;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.frequency.TeraHertzFactory;
import org.opendaylight.transportpce.tapi.frequency.grid.FrequencyMath;
import org.opendaylight.transportpce.tapi.frequency.grid.NumericFrequency;
import org.opendaylight.transportpce.tapi.frequency.range.FrequencyRangeFactory;
import org.opendaylight.transportpce.tapi.openroadm.TopologyNodeId;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.MdSalOpenRoadmTerminationPointReader;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmTerminationPointReader;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointSpectrumCapability;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMapping;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.DefaultOpenRoadmSpectrumRangeExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.DefaultTapiSpectrumCapabilityPacFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.OpenRoadmSpectrumRangeExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.TapiSpectrumCapabilityPacFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.TapiSpectrumGridConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;

/**
 * Default {@link TopologyChangesExtractor} that builds TAPI spectrum-capability information
 * for a set of OpenROADM termination points that have already been mapped to TAPI NEP names.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Reading OpenROADM {@link TerminationPoint}s from the datastore via
 *       {@link OpenRoadmTerminationPointReader}</li>
 *   <li>Extracting occupied/available spectrum ranges from the termination point data via
 *       {@link OpenRoadmSpectrumRangeExtractor}</li>
 *   <li>Building a TAPI Spectrum Capability PAC from the extracted spectrum ranges via
 *       {@link TapiSpectrumCapabilityPacFactory}</li>
 * </ul>
 *
 * <p>The input to this extractor is a set of {@link TerminationPointMapping}s produced elsewhere
 * (for example by a {@code TerminationPointMappingFactory}). Each mapping contains a resolved
 * OpenROADM termination point identity and one-or-more derived TAPI NEP names. For every NEP name,
 * this extractor produces an {@link OwnedNodeEdgePointSpectrumCapability}.
 *
 * <p>Note: this class does not write to the datastore; it only reads OpenROADM termination points
 * and constructs derived objects.
 */
public class TapiTopologyChangesExtractor implements TopologyChangesExtractor {

    private final OpenRoadmTerminationPointReader openRoadmTerminationPointReader;

    private final OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor;

    private final TapiSpectrumCapabilityPacFactory tapiSpectrumCapabilityPacFactory;

    /**
     * Creates an extractor with explicit dependencies.
     *
     * @param openRoadmTerminationPointReader reader used to fetch OpenROADM termination points
     * @param openRoadmSpectrumRangeExtractor extractor used to compute occupied/available spectrum ranges
     * @param tapiSpectrumCapabilityPacFactory factory used to build TAPI Spectrum Capability PACs
     */
    public TapiTopologyChangesExtractor(
            OpenRoadmTerminationPointReader openRoadmTerminationPointReader,
            OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor,
            TapiSpectrumCapabilityPacFactory tapiSpectrumCapabilityPacFactory) {

        this.openRoadmTerminationPointReader = openRoadmTerminationPointReader;
        this.openRoadmSpectrumRangeExtractor = openRoadmSpectrumRangeExtractor;
        this.tapiSpectrumCapabilityPacFactory = tapiSpectrumCapabilityPacFactory;
    }

    /** {@inheritDoc} */
    @Override
    public Set<OwnedNodeEdgePointSpectrumCapability> spectrumCapabilityPacs(
            Set<TerminationPointMapping> terminationPointMappings) {

        Objects.requireNonNull(terminationPointMappings, "terminationPointMappings");

        Set<OwnedNodeEdgePointSpectrumCapability> spectrumCapabilities = new HashSet<>();

        terminationPointMappings.forEach(mapping -> {
            TerminationPointId terminationPointId = mapping.terminationPointId();

            Optional<TerminationPoint> optionalTp = openRoadmTerminationPointReader.readTerminationPoint(
                    new TopologyNodeId(terminationPointId.nodeId()),
                    terminationPointId.tpId()
            );

            SpectrumCapabilityPac pac = tapiSpectrumCapabilityPacFactory.create(
                    openRoadmSpectrumRangeExtractor, optionalTp);

            mapping.nodeEdgePointNames().forEach(name -> spectrumCapabilities.add(
                    new OwnedNodeEdgePointSpectrumCapability(name, terminationPointId, pac)
            ));
        });


        return spectrumCapabilities;
    }

    /**
     * Creates a fully wired {@link TopologyChangesExtractor} using default implementations.
     *
     * <p>This factory method is intended for production wiring in components that already have access
     * to an MD-SAL {@link NetworkTransactionService} and the grid parameters required to decode
     * OpenROADM frequency bitmaps.
     *
     * <p>The returned extractor uses:
     * <ul>
     *   <li>{@link MdSalOpenRoadmTerminationPointReader} for datastore reads</li>
     *   <li>{@link DefaultOpenRoadmSpectrumRangeExtractor} configured with the provided grid parameters</li>
     *   <li>{@link DefaultTapiSpectrumCapabilityPacFactory} for PAC creation</li>
     * </ul>
     *
     * @param networkTransactionService transaction service used to read from MD-SAL
     * @param tapiSpectrumGridConfig spectrum settings
     * @return a {@link TopologyChangesExtractor} instance; never {@code null}
     * @throws NullPointerException if {@code networkTransactionService} is {@code null}
     */
    public static TopologyChangesExtractor create(
            NetworkTransactionService networkTransactionService,
            TapiSpectrumGridConfig tapiSpectrumGridConfig) {

        return new TapiTopologyChangesExtractor(
                new MdSalOpenRoadmTerminationPointReader(networkTransactionService),
                new DefaultOpenRoadmSpectrumRangeExtractor(
                        new NumericFrequency(
                                tapiSpectrumGridConfig.startEdgeFrequencyThz(),
                                tapiSpectrumGridConfig.effectiveBits(),
                                new FrequencyMath()
                        ),
                        new TeraHertzFactory(),
                        new FrequencyRangeFactory()
                ),
                new DefaultTapiSpectrumCapabilityPacFactory(
                        new TeraHertzFactory(),
                        tapiSpectrumGridConfig
                )
        );
    }

}
