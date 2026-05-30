/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import java.util.BitSet;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;

public interface McCapability {

    /**
     * Granularity of allowed center frequencies in GHz.
     * The base reference frequency is 193.1 THz (ITU-T G.694.1).
     * May return null for node types that do not constrain optical spectrum (e.g. OTN).
     */
    BigDecimal centerFrequencyGranularity();

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     */
    boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     * The observer is notified about errors.
     */
    boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     *
     * @see McCapability#isCompatibleWithServiceFrequency(BigDecimal)
     */
    boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width.
     * The observer is notified about errors.
     *
     * @see McCapability#isCompatibleWithServiceFrequency(BigDecimal)
     * @see McCapability#isCompatibleWithServiceFrequency(BigDecimal, Observer)
     */
    boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz, Observer observer);

    /**
     * Returns a bitset of frequencies that are supported by this MC interface.
     *
     * <p>0 = not supported, 1 = supported.
     * @param slotWidthGranularityGHz frequency width of each slot.
     * @param edgeFrequencyTHz lowest frequency on the spectrum grid.
     * @param effectiveBits nr of bits in the spectrum grid.
     * @return a bitset of frequencies that are supported by this MC interface.
     */
    BitSet supportableFrequencyRange(double slotWidthGranularityGHz, double edgeFrequencyTHz, int effectiveBits);
}
