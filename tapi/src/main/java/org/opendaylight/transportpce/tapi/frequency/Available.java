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

/**
 * A class capable of representing available frequency slots as a bit map may implement
 * this interface.
 *
 * <p>Available frequency ranges are represented by a (signed) byte array.
 * The size of the byte array multiplied by byte size is equal to the
 * total number of frequency "slots".
 * e.g. 96 bytes = 768 bits = 768 frequency slots.
 *
 * <p>Each frequency range is assumed to be 1 signed byte, i.e. 8 bits where
 * each bit represents a frequency range with a width of e.g. 6.25 GHz.
 *
 * <p>Examples:
 * byte[] frequencies = {0}       => {0b00000000} (no frequency slots available)
 * byte[] frequencies = {1}       => {0b00000001}
 * byte[] frequencies = {56}      => {0b00111000}
 * byte[] frequencies = {127}     => {0b01111111}
 * byte[] frequencies = {-128}    => {0b10000000}
 * byte[] frequencies = {-1}      => {0b11111111} (all frequency slots available)
 * byte[] frequencies = {-58}     => {0b11000110}
 * byte[] frequencies = {-58, -1} => {0b11000110, 0b11111111}
 *
 * <p>The available frequency slots...
 *     byte[] frequencies = {-58, -1} => {0b11000110, 0b11111111}
 * ...can be reversed using the method assignedFrequencies():
 *     availableFrequencies() => {0b00111001, 0b00000000}
 */
public interface Available {

    /**
     * Available frequency ranges.
     *
     * <p>This method returns a byte array representing available frequency ranges.
     *
     * @return A byte array representing available frequency ranges.
     */
    byte[] availableFrequencyRanges();

    /**
     * Assigned frequencies.
     *
     * <p>This method returns a BitSet representing used frequencies.
     *
     * <p>Each bit in the set represents a frequency range.
     *  - 1 (true) indicates that the frequency range is assigned (used).
     *  - 0 (false) indicates that the frequency range is available.
     *
     * @return A BitSet representing used frequency ranges.
     */
    BitSet assignedFrequencies();

    /**
     * Available frequencies.
     *
     * <p>This method returns a BitSet representing available frequencies.
     * This is the equivalent of inverting a bitset of used frequencies,
     * i.e. inverting the result of the method assignedFrequencies().
     *
     * <p>Each bit in the set represents a frequency.
     *  - 1 (true) indicates that the frequency range is available.
     *  - 0 (false) indicates that the frequency range is unavailable (used).
     *
     * @return A BitSet representing available frequency ranges.
     */
    BitSet availableFrequencies();

    /**
     * Return a map of ASSIGNED (i.e. used) frequency ranges.
     *
     * <p>The key is the lower frequency bound and the value is the upper frequency bound.
     */
    Map<Double, Double> assignedFrequency(Numeric numeric);

    /**
     * Return a map of AVAILABLE frequency ranges.
     *
     * <p>The key is the lower frequency bound and the value is the upper frequency bound.
     */
    Map<Double, Double> availableFrequency(Numeric numeric);

}
