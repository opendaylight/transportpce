/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import java.util.Map;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
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
    SpectrumCapabilityPac create(Map<Frequency, Frequency> usedFreqMap,
            Map<Frequency, Frequency> availableFreqMap);

}
