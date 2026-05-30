/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.util.BitSet;

public interface CapabilityCollection {

    /**
     * Add a MCCapability to this collection.
     */
    boolean add(McCapability mcCapability);

    /**
     * Determine if this MC interface is compatible with the required
     * service frequency width (i.e. slotWidthGranularityGHz x slotCount).
     *
     * @param slotWidthGranularityGHz Typically the frequency width of each slot in a 768 grid.
     * @param slotCount Typically the nr of slots the service requires on a 768 slot grid.
     */
    boolean isCompatibleService(double slotWidthGranularityGHz, int slotCount);

    /**
     * Filters the available spectrum grid to only include frequencies that are supported
     * by all capabilities in this collection.
     *
     * <p>0 = outside range, 1 = within range.
     *
     * @param availableFrequencyGrid available slots as a BitSet (1 = available).
     * @param slotWidthGranularityGHz Typically the frequency width of each slot in a 768 grid.
     * @param edgeFrequencyTHz Typically the lowest frequency on the spectrum grid.
     * @param effectiveBits Typically the nr of bits in the spectrum grid.
     * @return a BitSet of slots supported by this collection (0 = outside range, 1 = within range).
     */
    BitSet usableFrequencyRange(
            BitSet availableFrequencyGrid,
            double slotWidthGranularityGHz,
            double edgeFrequencyTHz,
            int effectiveBits);
}
