/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;

class AssignSpectrumHighToLowTest {

    @Test
    void testEmptyRange() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        assertEquals(new IndexRange(0, 0), assignSpectrum.range(768, 284, new BitSet(), 8, 8));
    }

    @Test
    void testLastBitsAreAvailable() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(750, 768);

        assertEquals(new IndexRange(760, 767), assignSpectrum.range(768, 284, available, 8, 8));
    }

    @Test
    void testSecondToLastBitsAreAvailable() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(740, 760);

        assertEquals(new IndexRange(752, 759), assignSpectrum.range(768, 284, available, 8, 8));
    }

    @Test
    void testFirstBitsAreAvailable() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 8);

        assertEquals(new IndexRange(0, 7), assignSpectrum.range(768, 284, available, 8, 8));
    }

    @Test
    void testFirstBitsAreAvailableTwiceCenterFrequencyGranularity() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 32);

        assertEquals(new IndexRange(4, 19), assignSpectrum.range(768, 284, available, 16, 16));
    }
}
