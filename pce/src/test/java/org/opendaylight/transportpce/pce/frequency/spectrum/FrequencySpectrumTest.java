/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.Index;
import org.opendaylight.yangtools.yang.common.Decimal64;

class FrequencySpectrumTest {

    @Test
    void frequencySlotsTwoConsecutiveFrequencies() {
        Decimal64 startFrequency = Decimal64.valueOf("192.1");
        Decimal64 endFrequency = Decimal64.valueOf("192.2");
        Index index = mock(Index.class);

        when(index.index(startFrequency)).thenReturn(120);
        when(index.index(endFrequency)).thenReturn(136);

        FrequencySpectrum frequencySpectrum = new FrequencySpectrum(index, 768);
        BitSet expected = new BitSet();
        expected.set(120, 136);

        BigDecimal start = BigDecimal.valueOf(192.1);
        BigDecimal end = BigDecimal.valueOf(192.2);

        assertEquals(expected, frequencySpectrum.frequencySlots(start, end));
    }

    @Test
    void frequencySlotsTwoNonConsecutiveFrequencies() {
        Decimal64 startFrequency = Decimal64.valueOf("192.1");
        Decimal64 endFrequency = Decimal64.valueOf("192.3");
        Index index = mock(Index.class);

        when(index.index(startFrequency)).thenReturn(120);
        when(index.index(endFrequency)).thenReturn(152);

        FrequencySpectrum frequencySpectrum = new FrequencySpectrum(index, 768);
        BitSet expected = new BitSet();
        expected.set(120, 152);

        BigDecimal start = BigDecimal.valueOf(192.1);
        BigDecimal end = BigDecimal.valueOf(192.3);

        assertEquals(expected, frequencySpectrum.frequencySlots(start, end));
    }

    @Test
    void whenNeedleIsFoundInTheMiddleOfHaystack_thenReturnTrue() {
        BitSet needle = new BitSet();
        needle.set(3, 15);
        BitSet haystack = new BitSet();
        haystack.set(0, 17);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertTrue(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenNeedleContainsMoreItemsThanHaystack_thenReturnFalse() {
        BitSet needle = new BitSet();
        needle.set(0, 15);
        BitSet haystack = new BitSet();
        haystack.set(1, 17);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }


    @Test
    void whenHaystackIsMissingOnItemInNeedle_thenReturnFalse() {
        BitSet needle = new BitSet();
        needle.set(3, 6);
        BitSet haystack = new BitSet();
        haystack.set(1, 17);
        haystack.set(4, false);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenHaystackIsEmptyAndNeedleIsNot_thenReturnFalse() {
        BitSet needle = new BitSet();
        needle.set(2, 6);
        BitSet haystack = new BitSet();

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenNeedleIsEmptyAndHaystackIsNot_thenReturnFalse() {
        BitSet needle = new BitSet();
        BitSet haystack = new BitSet();
        haystack.set(2, 6);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenNeedleIsFoundInTheBeginningOfHaystack_thenReturnTrue() {
        BitSet needle = new BitSet();
        needle.set(0, 16);
        BitSet haystack = new BitSet();
        haystack.set(0, 16);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertTrue(spectrum.isSubset(needle, haystack));
    }


    @Test
    void openEndedFrequencyRange() {
        Index index = mock(Index.class);
        //when(slots.getLowerSpectralIndexFromFrequency(any(BigDecimal.class))).thenReturn(280);
        //when(slots.getLowerSpectralIndexFromFrequency(any(Decimal64.class))).thenReturn(280);

        when(index.index(any(Decimal64.class))).thenReturn(280);
        when(index.index(any(Decimal64.class))).thenReturn(280);

        FrequencySpectrum frequencySpectrum = new FrequencySpectrum(index, 768);
        BitSet expected = new BitSet();
        expected.set(280, 769);

        BigDecimal startFrequency = BigDecimal.valueOf(193.1);
        assertEquals(expected, frequencySpectrum.frequencySlots(startFrequency, null));
    }

    @Test
    void whenAllTheBitsInNeedleAreFoundInTheBeginningOfHaystack_thenReturnTrue() {
        BitSet needle = new BitSet();
        needle.set(0, 16);

        BitSet haystack = new BitSet();
        haystack.set(0, 128);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertTrue(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenAllTheBitsInNeedleAreFoundInTheEndOfHaystack_thenReturnTrue() {
        BitSet needle = new BitSet();
        needle.set(102, 128);

        BitSet haystack = new BitSet();
        haystack.set(0, 128);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertTrue(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenAllTheBitsInNeedleAreFoundInTheMiddleOfHaystack_thenReturnTrue() {
        BitSet needle = new BitSet();
        needle.set(64, 84);

        BitSet haystack = new BitSet();
        haystack.set(0, 128);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertTrue(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenHaystackDoesNotContainTheFirstBitInNeedle_thenReturnFalse() {
        BitSet needle = new BitSet();
        needle.set(0, 16);

        BitSet haystack = new BitSet();
        haystack.set(1, 128);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenHaystackDoesNotContainTheLastBitInNeedle_thenReturnFalse() {
        BitSet needle = new BitSet();
        needle.set(0, 16);

        BitSet haystack = new BitSet();
        haystack.set(0, 15);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }

    @Test
    void whenHaystackDoesNotContainABitInTheMiddleOfNeedle_thenReturnFalse() {
        BitSet needle = new BitSet();
        needle.set(0, 16);

        BitSet haystack = new BitSet();
        haystack.set(0, 64);
        haystack.set(12, false);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        assertFalse(spectrum.isSubset(needle, haystack));
    }


    @Test
    void whenNeedleIsSearchedForInHaystack_verifyNeedleHasNotBeenModified() {
        BitSet needle = new BitSet();
        needle.set(0, 16);

        BitSet control = new BitSet();
        control.set(0,16);

        BitSet haystack = new BitSet();
        haystack.set(1, 15);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        spectrum.isSubset(needle, haystack);

        assertEquals(needle, control);
    }

    @Test
    void whenNeedleIsSearchedForInHaystack_verifyHaystackHasNotBeenModified() {
        BitSet needle = new BitSet();
        needle.set(0, 16);

        BitSet haystack = new BitSet();
        haystack.set(1, 15);

        BitSet control = new BitSet();
        control.set(1,15);

        Index index = mock(Index.class);
        Spectrum spectrum = new FrequencySpectrum(index, 768);

        spectrum.isSubset(needle, haystack);

        assertEquals(haystack, control);
    }
}
