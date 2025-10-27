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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;

class AssignSpectrumHighToLowTest {

    @Test
    @DisplayName("When no bits are available, range() should return an empty range (0,0)")
    void testEmptyRange() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        assertEquals(new IndexRange(0, 0), assignSpectrum.range(768, 284, new BitSet(), 8, 8));
    }

    @Test
    @DisplayName("When last bits are available, range() should select the highest available 8-slot range")
    void testLastBitsAreAvailable() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(750, 768);

        assertEquals(new IndexRange(760, 767), assignSpectrum.range(768, 284, available, 8, 8));
    }

    @Test
    @DisplayName("When second-to-last bits are available, range() should select that range correctly")
    void testSecondToLastBitsAreAvailable() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(740, 760);

        assertEquals(new IndexRange(752, 759), assignSpectrum.range(768, 284, available, 8, 8));
    }

    @Test
    @DisplayName("When only the first bits are available, range() should select from the start (0–7)")
    void testFirstBitsAreAvailable() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 8);

        assertEquals(new IndexRange(0, 7), assignSpectrum.range(768, 284, available, 8, 8));
    }

    @Test
    @DisplayName("When first bits are available twice the center-frequency granularity, range() should adjust "
            + "correctly")
    void testFirstBitsAreAvailableTwiceCenterFrequencyGranularity() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 32);

        assertEquals(new IndexRange(4, 19), assignSpectrum.range(768, 284, available, 16, 16));
    }

    @Test
    @DisplayName("When available range and located at the lowest frequencies and is smaller than requested "
        + "bandwidth, range() should return an empty range (0,0)")
    void testFirstBitsAreAvailableButTheAvailableRangeIsLessThanTheRequestedBandwidth() {
        Assign assignSpectrum = new AssignSpectrumHighToLow(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 4);

        assertEquals(new IndexRange(0, 0), assignSpectrum.range(768, 284, available, 1, 8));
    }
}
