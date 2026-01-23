/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.topology.ORtoTapiTopoConversionTools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;

/**
 * Factory for creating TAPI {@link SpectrumCapabilityPac} instances.
 *
 * <p>A {@code SpectrumCapabilityPac} describes a node edge point's spectrum ranges:
 * <ul>
 *   <li><b>supportable spectrum</b>: what is possible on the NEP</li>
 *   <li><b>available spectrum</b>: what is currently free</li>
 *   <li><b>occupied spectrum</b>: what is currently in use</li>
 * </ul>
 *
 * <p>Spectrum ranges are represented as {@code Map<Frequency, Frequency>} where each entry maps:
 * {@code lowerFrequency -> upperFrequency}.
 */
public interface TapiSpectrumCapabilityPacFactory {

    /**
     * Creates a {@link SpectrumCapabilityPac} from precomputed occupied and available spectrum ranges.
     *
     * @param usedFreqMap occupied spectrum ranges (lower -&gt; upper), may be {@code null} or empty
     * @param availableFreqMap available spectrum ranges (lower -&gt; upper), may be {@code null} or empty
     * @return a {@link SpectrumCapabilityPac}
     */
    SpectrumCapabilityPac create(
            Map<Frequency, Frequency> usedFreqMap,
            Map<Frequency, Frequency> availableFreqMap);

    /**
     * Creates a {@link SpectrumCapabilityPac} for an OpenROADM termination point using the provided
     * {@link TerminationPoint1} augmentation.
     *
     * <p>This method contains the OpenROADM TP-type specific conversion logic (switch/case), i.e.
     * selecting the correct extraction method from {@link ORtoTapiTopoConversionTools}.
     *
     * @param openRoadmSpectrumRangeExtractor converter used to derive used/available spectrum ranges
     * @param terminationPoint OpenROADM topology augmentation for the termination point
     * @return a {@link SpectrumCapabilityPac}
     * @throws NullPointerException if any argument is {@code null}
     */
    SpectrumCapabilityPac create(
            OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor,
            TerminationPoint terminationPoint);

    /**
     * Convenience overload that returns a default {@link SpectrumCapabilityPac} when the termination point
     * augmentation is not present.
     *
     * @param openRoadmSpectrumRangeExtractor converter used to derive used/available spectrum ranges
     * @param optionalTerminationPoint1 OpenROADM topology augmentation, if present
     * @return a {@link SpectrumCapabilityPac}
     */
    default SpectrumCapabilityPac create(
            OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor,
            Optional<TerminationPoint> optionalTerminationPoint1) {

        Objects.requireNonNull(openRoadmSpectrumRangeExtractor, "openRoadmSpectrumRangeExtractor");
        Objects.requireNonNull(optionalTerminationPoint1, "optionalTerminationPoint1");

        return optionalTerminationPoint1
                .map(tp -> create(openRoadmSpectrumRangeExtractor, tp))
                .orElseGet(() -> create(Collections.emptyMap(), Collections.emptyMap()));
    }
}
