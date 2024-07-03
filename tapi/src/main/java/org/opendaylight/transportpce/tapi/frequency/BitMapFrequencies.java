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

public class BitMapFrequencies implements BitMap {

    private final byte[] frequencyByteArray;

    /**
     * 0 indicates that the frequency range is not used,
     * 1 indicates that the frequency range is used.
     *
     * <p>
     * Each frequency range is assumed to be 1 byte, i.e. 8 bits. Each bit
     * represents a frequency.
     *
     * <p>
     * The size of the byte array is equal to the number of frequency ranges.
     */
    public BitMapFrequencies(byte[] frequencyByteArray) {
        this.frequencyByteArray = new byte[frequencyByteArray.length];
        System.arraycopy(frequencyByteArray, 0, this.frequencyByteArray, 0, frequencyByteArray.length);
    }

    @Override
    public byte[] assignedFrequencyRanges() {

        byte[] freqBitSetCopy = new byte[frequencyByteArray.length];

        System.arraycopy(frequencyByteArray, 0, freqBitSetCopy, 0, frequencyByteArray.length);

        return freqBitSetCopy;
    }

    @Override
    public BitSet assignedFrequencies() {

        BitSet bitSet = new BitSet(frequencyByteArray.length * Byte.SIZE);

        for (int i = 0; i < frequencyByteArray.length; i++) {
            if (frequencyByteArray[i] != 0) {
                bitSet.set(i * Byte.SIZE, i * Byte.SIZE + Byte.SIZE);
            }
        }

        return bitSet;
    }

    @Override
    public BitSet availableFrequencies() {
        BitSet bitSet = new BitSet(frequencyByteArray.length * Byte.SIZE);

        for (int i = 0; i < frequencyByteArray.length; i++) {
            if (frequencyByteArray[i] == 0) {
                bitSet.set(i * Byte.SIZE, i * Byte.SIZE + Byte.SIZE);
            }
        }

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
