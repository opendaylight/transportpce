/*
 * Copyright © 2024 Smartoptics, Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class NumericFrequency implements Numeric {

    private final Double startFrequency;

    private final int nrOfBits;

    private final Math math;

    public NumericFrequency(Double startFrequency, int nrOfBits) {
        this(startFrequency, nrOfBits, new FrequencyMath());
    }

    public NumericFrequency(Double startFrequency, int nrOfBits, Math math) {
        this.startFrequency = startFrequency;
        this.nrOfBits = nrOfBits;
        this.math = math;
    }

    @Override
    public Map<Double, Double> assignedFrequency(Available frequency) {
        return this.range(frequency.assignedFrequencies());
    }

    @Override
    public Map<Double, Double> availableFrequency(Available frequency) {
        return this.range(frequency.availableFrequencies());
    }

    /**
     * Note: This code is mostly copied as is from the original
     * class ConvertORToTapiTopology.java as part of refactoring.
     */
    private Map<Double, Double> range(BitSet bitSet) {

        Map<Double,Double> freqMap = new LinkedHashMap<>();

        Double startFreq = startFrequency;
        Double stopFreq = 0.0;
        boolean occupied = !bitSet.get(0);

        for (int index = 0 ; index < nrOfBits ; index++) {
            if (occupied) {
                if (bitSet.get(index)) {
                    startFreq = math.getStartFrequencyFromIndex(index);
                    stopFreq = math.getStartFrequencyFromIndex(index);
                    occupied = false;
                }
            } else {
                if (!bitSet.get(index)) {
                    stopFreq = math.getStartFrequencyFromIndex(index);
                    occupied = true;
                }
            }
            if (occupied) {
                if (stopFreq > startFreq) {
                    freqMap.put(startFreq, stopFreq);
                    startFreq = stopFreq;
                }
            } else {
                if (index == nrOfBits - 1
                        && Double.compare(startFreq, stopFreq) == 0) {
                    stopFreq = math.getStopFrequencyFromIndex(index);
                    freqMap.put(startFreq, stopFreq);
                }
            }
        }
        return freqMap;
    }
}
