/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

public interface Factory {

    /**
     * Create a frequency object by adding half of the bandwidth to the center frequency.
     */
    Frequency upper(Double centerFrequencyTHz, Double bandwidthGHz);

    /**
     * Create a frequency object by subtracting half of the bandwidth from the center frequency.
     */
    Frequency lower(Double centerFrequencyTHz, Double bandwidthGHz);

    /**
     * Create a new light frequency. The frequency is calculated as follows:
     * startFrequencyTHz + (granularityGHz * 0.001 * bitNumber).
     *
     * <p>
     * Granularity actually implies a range. The bitNumber indicates the left edge of the range.
     * Meaning, the frequency is in the range [bitNumber, bitNumber + 1]. Or in other words, assuming
     * the start frequency is 191.325, then bitNumber = 0 = 191.325THz.
     *
     * <p>
     * To get the higher frequency of the range, use bitNumber + 1.
     *
     * @param startFrequencyTHz e.g. 193.1 THz
     * @param granularityGHz e.g. 6.25 GHz
     * @param bitNumber e.g. 768
     */
    Frequency frequency(Double startFrequencyTHz, Double granularityGHz, int bitNumber);

}
