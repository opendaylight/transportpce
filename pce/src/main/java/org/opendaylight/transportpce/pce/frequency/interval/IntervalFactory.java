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
     *
     * <p>
     * A frequency window can be best described as the frequency range
     * from within a service frequency is selected. Meaning the client
     * may restrict selecting a subset of this range.
     */
    Collection frequencyWindow(PathComputationRequestInput input, Spectrum frequencySpectrum);

    /**
     * Factory method creating a frequency interval collection.
     *
     * <p>
     * A specific frequency range. The client may wish to use a specific
     * range. Unlike frequencyWindow, which is more relaxed, the client
     * is asking for a specific non-optional range.
     */
    Collection flexGrid(PathComputationRequestInput input, Spectrum frequencySpectrum);

    /**
     * Factory method creating a frequency interval.
     *
     * <p>
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param widthGHz the width of the interval in GHz.
     * @param slots the number of slots in the interval. Each slot is widthGHz.
     */
    Interval interval(BigDecimal centerFrequencyTHz, BigDecimal widthGHz, int slots);

    /**
     * The lower frequency by subtracting the number of slots divided by 2 and multiplied by the frequency width.
     *
     * <p>
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param widthGHz the width of the interval in GHz.
     * @param slots the number of slots in the interval. Each slot is widthGHz.
     */
    BigDecimal lowerFrequency(BigDecimal centerFrequencyTHz, Double widthGHz, int slots);

    /**
     * The upper frequency by adding the number of slots divided by 2 and multiplied by the frequency width.
     *
     * <p>
     * @param centerFrequencyTHz the center frequency of the interval in THz.
     * @param widthGHz the width of the interval in GHz.
     * @param slots the number of slots in the interval. Each slot is widthGHz.
     */
    BigDecimal upperFrequency(BigDecimal centerFrequencyTHz, Double widthGHz, int slots);

}
