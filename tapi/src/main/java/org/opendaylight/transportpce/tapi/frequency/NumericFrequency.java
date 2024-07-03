/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
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
    public Map<Double, Double> assignedFrequency(BitMap frequency) {
        return this.frequency(frequency.assignedFrequencies());
    }

    @Override
    public Map<Double, Double> availableFrequency(BitMap frequency) {
        return this.frequency(frequency.availableFrequencies());
    }

    private Map<Double, Double> frequency(BitSet byteArray) {
        // Provides a Map <LowerFreq, HigherFreq> describing start and stop frequencies of all slots that are available
        // in the ByteArray describing the spectrum : bit sets initially sets to 1/true
        // In case the byte array has been inverted before calling this method, it provides respectively a map
        // describing all occupied slots!
        //This code is mostly copied as is from the original class ConvertORToTapiTopology.java.

        Map<Double,Double> freqMap = new LinkedHashMap<>();

        Double startFreq = startFrequency;
        Double stopFreq = 0.0;
        boolean occupied = !byteArray.get(0);

        for (int index = 0 ; index < nrOfBits ; index++) {
            if (occupied) {
                if (byteArray.get(index)) {
                    startFreq = math.getStartFrequencyFromIndex(index);
                    stopFreq = math.getStartFrequencyFromIndex(index);
                    occupied = false;
                }
            } else {
                if (!byteArray.get(index)) {
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
