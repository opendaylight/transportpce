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
 * A class capable of representing frequency slots as a bit map should implement
 * this interface.
 *
 * <p>
 * Frequency slots are represented by a (signed) byte array.
 * The size of the byte array multiplied by byte size is equal to the
 * total number of frequency slots.
 * e.g. 96 bytes = 768 bits = 768 frequency slots.
 *
 * <p>
 * Each frequency range is assumed to be 1 signed byte, i.e. 8 bits where
 * each bit represents a frequency range with a width of e.g. 6.25 GHz.
 *
 * <p>
 * Examples:
 * byte[] frequencies = {0}       => {0b00000000} (no frequency slots used)
 * byte[] frequencies = {1}       => {0b00000001}
 * byte[] frequencies = {56}      => {0b00111000}
 * byte[] frequencies = {127}     => {0b01111111}
 * byte[] frequencies = {-128}    => {0b10000000}
 * byte[] frequencies = {-1}      => {0b11111111} (all frequency slots used)
 * byte[] frequencies = {-58}     => {0b11000110}
 * byte[] frequencies = {-58, -1} => {0b11000110, 0b11111111}
 *
 * <p>
 * The assigned frequency slots...
 *     byte[] frequencies = {-58, -1} => {0b11000110, 0b11111111}
 * ...can be reversed using the method availableFrequencies():
 *     availableFrequencies() => {0b00111001, 0b00000000}
 */
public interface Assigned {

    /**
     * Assigned frequency ranges.
     *
     * <p>
     * This method returns a byte array representing used frequencies.
     *
     * @return A byte array representing used frequencies.
     */
    byte[] assignedFrequencyRanges();

    /**
     * Assigned frequencies.
     *
     * <p>
     * This method returns a BitSet representing used frequencies.
     *
     * <p>
     * Each bit in the set represents a frequency.
     *  - 1 (true) indicates that the frequency is used.
     *  - 0 (false) indicates that the frequency is available.
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
     * i.e. inverting the result of the method assignedFrequencies().
     *
     * <p>
     * Each bit in the set represents a frequency.
     *  - 1 (true) indicates that the frequency is available.
     *  - 0 (false) indicates that the frequency is unavailable.
     *
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
