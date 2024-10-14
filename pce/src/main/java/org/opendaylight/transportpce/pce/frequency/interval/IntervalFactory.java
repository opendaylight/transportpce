/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.math.BigDecimal;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;

public interface IntervalFactory {

    /**
     * Factory method creating a frequency interval collection.
     * The client may restrict selecting a service frequency
     * range as a subset of this range.
     */
    Collection frequencyRange(PathComputationRequestInput input, Spectrum frequencySpectrum);

    /**
     * Factory method creating a frequency interval collection.
     * A specific frequency range. The client may wish to use a specific
     * frequency slot. Unlike frequencyRange, which is more relaxed, the client
     * is asking for a specific non-optional range.
     *
     * @throws InvalidIntervalException If path computation request contains invalid input.
     */
    Collection frequencySlot(PathComputationRequestInput input, Spectrum frequencySpectrum);

    /**
     * Factory method creating a frequency interval.
     * i.e. centerFrequencyTHz ± ((slotWidthGHz / 1000) x (nrOfSlots / 2)).
     *
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param slotWidthGHz the width of the interval in GHz.
     * @param nrOfSlots >=2. The number of nrOfSlots in the interval. Each slot is slotWidthGHz.
     * @return A frequency interval with a range of nrOfSlots * slotWidthGHz and centerFrequencyTHz in the middle.
     */
    Interval interval(BigDecimal centerFrequencyTHz, BigDecimal slotWidthGHz, int nrOfSlots);

    /**
     * The lower frequency by subtracting the number of nrOfSlots divided by 2 and multiplied by the frequency width.
     * i.e. centerFrequencyTHz - ((slotWidthGHz / 1000) x (nrOfSlots / 2))
     *
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param slotWidthGHz the width of the interval in GHz.
     * @param nrOfSlots >=2. The number of nrOfSlots in the interval. Each slot is slotWidthGHz.
     * @return centerFrequencyTHz - ((slotWidthGHz / 1000) x (nrOfSlots / 2))
     * @throws InvalidIntervalException In case the input cannot produce a valid interval.
     */
    BigDecimal lowerFrequency(BigDecimal centerFrequencyTHz, Double slotWidthGHz, int nrOfSlots);

    /**
     * The upper frequency by adding the number of nrOfSlots divided by 2 and multiplied by the frequency width.
     * i.e. centerFrequencyTHz + ((slotWidthGHz / 1000) x (nrOfSlots / 2))
     *
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param slotWidthGHz the width of the interval in GHz.
     * @param nrOfSlots >=2. The number of nrOfSlots in the interval. Each slot is slotWidthGHz.
     * @return centerFrequencyTHz + ((slotWidthGHz / 1000) x (nrOfSlots / 2))
     * @throws InvalidIntervalException In case the input cannot produce a valid interval.
     */
    BigDecimal upperFrequency(BigDecimal centerFrequencyTHz, Double slotWidthGHz, int nrOfSlots);

}
