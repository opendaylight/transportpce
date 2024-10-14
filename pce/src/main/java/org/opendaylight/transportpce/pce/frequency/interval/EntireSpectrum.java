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
 * This is a special kind of frequency collection.
 * The collection represents an entire frequency spectrum, i.e. all frequencies.
 */
public class EntireSpectrum implements Collection {

    private final int effectiveBits;

    public EntireSpectrum(int effectiveBits) {
        this.effectiveBits = effectiveBits;
    }

    @Override
    public boolean add(Interval interval) {
        return true;
    }

    @Override
    public BitSet subset(BitSet availableSlots) {
        return availableSlots;
    }

    @Override
    public BitSet subset(Collection collection) {
        return collection.set();
    }

    @Override
    public BitSet intersection(BitSet availableSlots) {
        return availableSlots;
    }

    @Override
    public BitSet intersection(Collection collection) {
        return collection.set();
    }

    @Override
    public BitSet set() {
        BitSet entireSpectrum = new BitSet(effectiveBits);
        entireSpectrum.set(0, effectiveBits);

        return entireSpectrum;
    }

    @Override
    public int size() {
        return 1;
    }

}
