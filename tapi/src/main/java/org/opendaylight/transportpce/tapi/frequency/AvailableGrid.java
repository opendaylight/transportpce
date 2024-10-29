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

public class AvailableGrid implements Available {

    private final byte[] availableFrequencyGrid;

    /**
     * The available frequency slots are represented by a (signed) byte array.
     *
     * <p>The size of the byte array multiplied by byte size is equal to the
     * total number of frequency slots.
     * e.g. 96 bytes = 768 bits = 768 frequency slots.
     *
     * <p>The available frequency slots...
     *     byte[] frequencies = {-58, -1} => {0b11000110, 0b11111111}
     * ...can be reversed using the method assignedFrequencies():
     *     availableFrequencies() => {0b00111001, 0b00000000}
     */
    public AvailableGrid(byte[] availableFrequencyGrid) {
        this.availableFrequencyGrid = new byte[availableFrequencyGrid.length];
        System.arraycopy(availableFrequencyGrid, 0, this.availableFrequencyGrid, 0, availableFrequencyGrid.length);
    }

    @Override
    public byte[] availableFrequencyRanges() {

        byte[] freqBitSetCopy = new byte[availableFrequencyGrid.length];

        System.arraycopy(availableFrequencyGrid, 0, freqBitSetCopy, 0, availableFrequencyGrid.length);

        return freqBitSetCopy;
    }

    @Override
    public BitSet assignedFrequencies() {

        BitSet bitSet = new BitSet(availableFrequencyGrid.length * Byte.SIZE);

        bitSet.or(BitSet.valueOf(availableFrequencyGrid));

        bitSet.flip(0, availableFrequencyGrid.length * Byte.SIZE);

        return bitSet;
    }

    @Override
    public BitSet availableFrequencies() {

        BitSet bitSet = new BitSet(availableFrequencyGrid.length * Byte.SIZE);

        bitSet.or(BitSet.valueOf(availableFrequencyGrid));

        return bitSet;

    }

    @Override
    public Map<Double, Double> assignedFrequency(Numeric numeric) {
        return numeric.assignedFrequency(this);
    }

    @Override
    public Map<Double, Double> availableFrequency(Numeric numeric) {
        return numeric.availableFrequency(this);
    }
}
