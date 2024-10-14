/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.util.BitSet;

/**
 * A collection of (frequency) intervals.
 */
public interface Collection {

    /**
     * Add an interval to this collection.
     *
     * <p>
     * Only adds the interval to the collection if
     * the interval isn't already present.
     * @return true if the interval was added.
     */
    boolean add(Interval interval);

    /**
     * Returns the Bitset subset of this frequency interval collection
     * to the available BitSet.
     * If this frequency collection is not a subset of availableSlots, an
     * empty BitSet is returned.
     */
    BitSet subset(BitSet availableSlots);

    /**
     * Returns the Bitset subset.
     * @see #subset(BitSet)
     */
    BitSet subset(Collection collection);

    /**
     * Returns the Bitset intersection of this frequency interval collection
     * to the available BitSet.
     */
    BitSet intersection(BitSet availableSlots);

    /**
     * Returns the Bitset intersection.
     * @see #intersection(BitSet)
     */
    BitSet intersection(Collection collection);

    /**
     * Return this collection of intervals as a set.
     */
    BitSet set();

    /**
     * The size of this collection.
     */
    int size();

}
