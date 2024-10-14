/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.SpectrumSet;

public class IntervalCollection implements Collection {

    private final Set<Interval> intervals = new HashSet<>();

    private final SpectrumSet spectrumSet;

    private final int effectiveBits;

    public IntervalCollection(SpectrumSet spectrumSet, int effectiveBits) {
        this.spectrumSet = spectrumSet;
        this.effectiveBits = effectiveBits;
    }

    @Override
    public boolean add(Interval interval) {
        return intervals.add(interval);
    }

    @Override
    public BitSet subset(BitSet availableSlots) {
        return spectrumSet.subset(intervals, availableSlots, effectiveBits);
    }

    @Override
    public BitSet subset(Collection collection) {
        return spectrumSet.subset(intervals, collection.set(), effectiveBits);
    }

    @Override
    public BitSet intersection(BitSet availableSlots) {
        return spectrumSet.intersection(intervals, availableSlots, effectiveBits);
    }

    @Override
    public BitSet intersection(Collection collection) {
        return spectrumSet.intersection(intervals, collection.set(), effectiveBits);
    }

    @Override
    public BitSet set() {
        return spectrumSet.set(intervals, effectiveBits);
    }

    @Override
    public int size() {
        return intervals.size();
    }

    @Override
    public String toString() {
        return "IntervalCollection{"
                + "intervals=" + intervals
                + '}';
    }
}
