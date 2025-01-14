/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment;

import java.util.BitSet;

public interface Assign {

    /**
     * Calculate an available spectrum index range for a new service.
     *
     * @param effectiveBits The nr of effective bits in the frequency spectrum.
     * @param baseFrequencyIndex The base frequency index location of the spectrum grid (e.g. 284 for 193.1)
     * @param spectrumOccupation A bitset representing the available (1) and occupied (0) frequency slots.
     * @param centerFrequencyGranularity A collection of center frequency granularites, typically found in the
     *                                             service path.
     * @param serviceSlotWidth The nr of slots the new service needs.
     */
    Range range(
        int effectiveBits,
        int baseFrequencyIndex,
        BitSet spectrumOccupation,
        int centerFrequencyGranularity,
        int serviceSlotWidth
    );

}
