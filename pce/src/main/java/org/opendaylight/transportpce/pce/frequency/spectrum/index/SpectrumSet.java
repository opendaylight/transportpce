/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;

import java.util.BitSet;
import java.util.Set;
import org.opendaylight.transportpce.pce.frequency.interval.Interval;

/**
 * This interface provides methods intended for mathematical set theory operations
 * of frequency intervals/BitSets.
 */
public interface SpectrumSet {

    /**
     * Returns the Bitset subset of the interval collection
     * to the available BitSet.
     * If the frequency collection is not a real subset of availableSlots, an
     * empty BitSet is returned. Meaning all the slots in the collection
     * must be available in the availableSlots, otherwise the method returns
     * an empty BitSet.
     */
    BitSet subset(Set<Interval> intervals, BitSet availableSlots, int effectiveBits);

    /**
     * Returns the Bitset intersection of the frequency interval collection
     * to the available BitSet.
     * @param effectiveBits the effective number of bits, i.e. entire BitSet length of which
     *                      the intervals are a subset of.
     */
    BitSet intersection(Set<Interval> intervals, BitSet availableSlots, int effectiveBits);

    /**
     * Turn a frequency interval into a BitSet.
     * @param intervals the frequency interval collection.
     * @param effectiveBits the effective number of bits, i.e. entire BitSet length of which
     *                      the intervals are a subset of.
     */
    BitSet set(Set<Interval> intervals, int effectiveBits);

}
