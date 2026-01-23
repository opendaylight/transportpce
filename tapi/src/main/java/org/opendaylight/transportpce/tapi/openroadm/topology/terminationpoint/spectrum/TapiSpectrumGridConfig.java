/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import org.opendaylight.transportpce.common.fixedflex.GridConstant;

/**
 * Parameters used to derive default/supportable spectrum ranges for TAPI spectrum capability PACs.
 *
 * @param startEdgeFrequencyThz start edge frequency in THz (e.g. 191.325)
 * @param granularity frequency grid granularity (e.g. 6.25)
 * @param effectiveBits number of effective bits used in grid calculations (e.g. 768)
 */
public record TapiSpectrumGridConfig(
        double startEdgeFrequencyThz,
        double granularity,
        int effectiveBits) {

    public TapiSpectrumGridConfig {
        // Defensive checks
        if (Double.isNaN(startEdgeFrequencyThz) || startEdgeFrequencyThz <= 0.0) {
            throw new IllegalArgumentException("startEdgeFrequencyThz must be > 0");
        }
        if (Double.isNaN(granularity) || granularity <= 0.0) {
            throw new IllegalArgumentException("granularity must be > 0");
        }
        if (effectiveBits <= 0) {
            throw new IllegalArgumentException("effectiveBits must be > 0");
        }
    }

    /** Default config based on {@link GridConstant}. */
    public static TapiSpectrumGridConfig defaults() {
        return new TapiSpectrumGridConfig(
                GridConstant.START_EDGE_FREQUENCY_THZ,
                GridConstant.GRANULARITY,
                GridConstant.EFFECTIVE_BITS);
    }
}
