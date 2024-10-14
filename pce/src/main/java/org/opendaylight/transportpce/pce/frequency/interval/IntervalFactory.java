/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.math.BigDecimal;
import org.opendaylight.transportpce.pce.frequency.input.InvalidClientInputException;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;

public interface IntervalFactory {

    /**
     * Factory method creating a frequency interval collection.
     *
     * <p>
     * The client may restrict selecting a service frequency
     * range as a subset of this range.
     */
    Collection frequencyRange(PathComputationRequestInput input, Spectrum frequencySpectrum);

    /**
     * Factory method creating a frequency interval collection.
     *
     * <p>
     * A specific frequency range. The client may wish to use a specific
     * frequency slot. Unlike frequencyRange, which is more relaxed, the client
     * is asking for a specific non-optional range.
     * @throws InvalidClientInputException If path computation request contains invalid input.
     */
    Collection frequencySlot(PathComputationRequestInput input, Spectrum frequencySpectrum);

    /**
     * Factory method creating a frequency interval.
     * i.e. centerFrequencyTHz ± ((slotFrequencyWidthGHz / 1000) x (slots / 2)).
     *
     * <p>
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param slotFrequencyWidthGHz the width of the interval in GHz.
     * @param slots >=2. The number of slots in the interval. Each slot is slotFrequencyWidthGHz.
     * @return A frequency interval with a range of slots * slotFrequencyWidthGHz and centerFrequencyTHz in the middle.
     */
    Interval interval(BigDecimal centerFrequencyTHz, BigDecimal slotFrequencyWidthGHz, int slots);

    /**
     * The lower frequency by subtracting the number of slots divided by 2 and multiplied by the frequency width.
     * i.e. centerFrequencyTHz - ((slotFrequencyWidthGHz / 1000) x (slots / 2))
     *
     * <p>
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param slotFrequencyWidthGHz the width of the interval in GHz.
     * @param slots >=2. The number of slots in the interval. Each slot is slotFrequencyWidthGHz.
     * @return centerFrequencyTHz - ((slotFrequencyWidthGHz / 1000) x (slots / 2))
     * @throws InvalidIntervalException In case the input cannot produce a valid interval.
     */
    BigDecimal lowerFrequency(BigDecimal centerFrequencyTHz, Double slotFrequencyWidthGHz, int slots);

    /**
     * The upper frequency by adding the number of slots divided by 2 and multiplied by the frequency width.
     * i.e. centerFrequencyTHz + ((slotFrequencyWidthGHz / 1000) x (slots / 2))
     *
     * <p>
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param slotFrequencyWidthGHz the width of the interval in GHz.
     * @param slots >=2. The number of slots in the interval. Each slot is slotFrequencyWidthGHz.
     * @return centerFrequencyTHz + ((slotFrequencyWidthGHz / 1000) x (slots / 2))
     * @throws InvalidIntervalException In case the input cannot produce a valid interval.
     */
    BigDecimal upperFrequency(BigDecimal centerFrequencyTHz, Double slotFrequencyWidthGHz, int slots);

}
