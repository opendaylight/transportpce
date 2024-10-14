/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;


import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Set;
import org.opendaylight.transportpce.pce.frequency.interval.Interval;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;

public class FrequencySpectrumSet implements SpectrumSet {

    private final Spectrum spectrum;


    public FrequencySpectrumSet(Spectrum spectrum) {
        this.spectrum = spectrum;
    }

    @Override
    public BitSet subset(Set<Interval> intervals, BitSet availableSlots, int effectiveBits) {

        BitSet subset = new BitSet(effectiveBits);

        BigDecimal start;
        BigDecimal end;

        for (Interval interval : intervals) {

            start = interval.start();
            end = interval.end();

            if (start != null && end != null) {
                BitSet frequencyRange = spectrum.frequencySlots(
                        start,
                        end
                );
                if (spectrum.isSubset(frequencyRange, availableSlots)) {
                    subset.or(frequencyRange);
                } else {
                    return new BitSet();
                }
            }
        }

        return subset;
    }

    @Override
    public BitSet intersection(Set<Interval> intervals, BitSet availableSlots, int effectiveBits) {

        BitSet bitSet = set(intervals, effectiveBits);

        bitSet.and(availableSlots);

        return bitSet;

    }

    @Override
    public BitSet set(Set<Interval> intervals, int effectiveBits) {

        BitSet set = null;

        BigDecimal start;
        BigDecimal end;

        for (Interval interval : intervals) {

            start = interval.start();
            end = interval.end();

            if (start != null && end != null) {
                BitSet frequencyRange = spectrum.frequencySlots(
                        start,
                        end
                );

                if (set == null) {
                    set = frequencyRange;
                } else {
                    set.and(frequencyRange);
                }
            }
        }

        if (set == null) {
            return new BitSet(effectiveBits);
        }
        return set;
    }
}
