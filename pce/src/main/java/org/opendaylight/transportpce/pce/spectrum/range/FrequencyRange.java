/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.range;

import java.util.BitSet;

/**
 * A frequency range that can be expressed as a set of spectrum grid slots.
 *
 * <p>Implementations define the frequency boundaries of the range.
 * {@link #gridRange} maps those boundaries onto a specific grid, returning
 * the subset of slots that fall within this range.
 *
 * @see McCapabilityRange
 * @see EntireGridRange
 */
public interface FrequencyRange {

    /**
     * Returns a BitSet of grid slots that fall within this frequency range.
     *
     * <p>0 = outside range, 1 = within range.
     * @param slotWidthGranularityGHz frequency width of each slot.
     * @param edgeFrequencyTHz lowest frequency on the spectrum grid.
     * @param effectiveBits nr of bits in the spectrum grid.
     * @return a BitSet where each bit represents a {@code slotWidthGranularityGHz}-wide slot,
     *         set to 1 if the slot falls within this frequency range.
     */
    BitSet gridRange(double slotWidthGranularityGHz, double edgeFrequencyTHz, int effectiveBits);

}
