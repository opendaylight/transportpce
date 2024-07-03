/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.util.BitSet;
import java.util.Map;

public interface BitMap {

    /**
     * Get frequency ranges.
     *
     * <p>
     * This method returns a byte array representing used frequencies.
     *
     * <p>
     * The size of the byte array is equal to the number of frequency ranges.
     *
     * <p>
     * Each byte in the byte array represents a frequency range.
     * Each byte is an 8-bit integer, where each bit represents a frequency.
     *  - 1 indicates that the frequency range is used.
     *  - 0 indicates that the frequency range is not used.
     *
     * <p>
     * @return A byte array representing used frequencies.
     */
    byte[] assignedFrequencyRanges();

    /**
     * Get frequencies.
     *
     * <p>
     * This method returns a BitSet representing used frequencies.
     *
     * <p>
     * Each bit in the set represents a frequency.
     *  - 1 indicates that the frequency is used.
     *  - 0 indicates that the frequency is available.
     *
     * @return A BitSet representing used frequencies.
     */
    BitSet assignedFrequencies();

    /**
     * Get frequencies.
     *
     * <p>
     * This method returns a BitSet representing available frequencies.
     * This is the equivalent of inverting a bitset of used frequencies,
     * i.e. inverting the result of assignedFrequencies().
     *
     * <p>
     * Each bit in the set represents a frequency.
     *  - 1 indicates that the frequency is available.
     *  - 0 indicates that the frequency is unavailable.
     *
     *  <p>
     * @return A BitSet representing available frequencies.
     */
    BitSet availableFrequencies();

    /**
     * Return a map of ASSIGNED (i.e. used) frequency ranges.
     *
     * <p>
     * The key is the lower frequency bound and the value is the upper frequency bound.
     */
    Map<Double, Double> assignedFrequency(Numeric numeric);

    /**
     * Return a map of AVAILABLE frequency ranges.
     *
     * <p>
     * The key is the lower frequency bound and the value is the upper frequency bound.
     */
    Map<Double, Double> availableFrequency(Numeric numeric);

}
