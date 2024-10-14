/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.pce.frequency.interval.FrequencyInterval;
import org.opendaylight.transportpce.pce.frequency.interval.Interval;
import org.opendaylight.transportpce.pce.frequency.spectrum.FrequencySpectrum;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;

class FrequencySpectrumSetTest {

    @Test
    void realSubset() {
        Set<Interval> realSubset = new HashSet<>();
        realSubset.add(
                new FrequencyInterval(
                        FrequencyTHz.getDefaultInstance("191.325"),
                        FrequencyTHz.getDefaultInstance("191.375")
                )
        );

        BitSet available = new BitSet(768);
        available.set(0, 32);

        BitSet expected = new BitSet(768);
        expected.set(0, 8);

        Index frequencyIndex = new SpectrumIndex(191.325, 6.25, 768);
        FrequencySpectrum spectrum = new FrequencySpectrum(frequencyIndex, 768);
        FrequencySpectrumSet frequencySpectrumSet = new FrequencySpectrumSet(spectrum);
        Assertions.assertEquals(expected, frequencySpectrumSet.subset(realSubset, available, 768));
    }


    @Test
    void notRealSubsetReturnsEmptyBitSet() {
        Index frequencyIndex = new SpectrumIndex(191.325, 6.25, 768);
        FrequencySpectrum spectrum = new FrequencySpectrum(frequencyIndex, 768);

        FrequencySpectrumSet frequencySpectrumSet = new FrequencySpectrumSet(spectrum);

        Set<Interval> realSubset = new HashSet<>();
        realSubset.add(
                new FrequencyInterval(
                        FrequencyTHz.getDefaultInstance("191.325"),
                        FrequencyTHz.getDefaultInstance("191.375")
                )
        );

        BitSet available = new BitSet(768);
        available.set(1, 32);

        BitSet expected = new BitSet(768);

        assertEquals(expected, frequencySpectrumSet.subset(realSubset, available, 768));
    }

    @Test
    void assertTwoIntervalsAreJoinedUsingIntersection() {
        Spectrum spectrum = Mockito.mock(FrequencySpectrum.class);

        BitSet oneBitSet = new BitSet(768);
        oneBitSet.set(16, 32);
        BitSet twoBitSet = new BitSet(768);
        twoBitSet.set(24, 48);

        BigDecimal oneStart = BigDecimal.valueOf(191.325);
        BigDecimal oneEnd = BigDecimal.valueOf(191.375);
        BigDecimal twoStart = BigDecimal.valueOf(191.365);
        BigDecimal twoEnd = BigDecimal.valueOf(191.425);

        Mockito.when(spectrum.frequencySlots(oneStart, oneEnd)).thenReturn(oneBitSet);
        Mockito.when(spectrum.frequencySlots(twoStart, twoEnd)).thenReturn(twoBitSet);

        Set<Interval> collection = new HashSet<>();
        Interval intervalOne = new FrequencyInterval(oneStart, oneEnd);
        Interval intervalTwo = new FrequencyInterval(twoStart, twoEnd);
        collection.add(intervalOne);
        collection.add(intervalTwo);

        BitSet expected = new BitSet(768);
        expected.set(24, 32);
        FrequencySpectrumSet frequencySpectrumSet = new FrequencySpectrumSet(spectrum);
        Assertions.assertEquals(expected, frequencySpectrumSet.set(collection, 768));
    }

    @Test
    void assertThreeIntervalsAreJoinedUsingIntersection() {
        BitSet oneBitSet = new BitSet(768);
        oneBitSet.set(16, 32);
        BitSet twoBitSet = new BitSet(768);
        twoBitSet.set(24, 48);
        BitSet threeBitSet = new BitSet(768);
        threeBitSet.set(8, 28);

        Spectrum spectrum = Mockito.mock(FrequencySpectrum.class);

        BigDecimal oneStart = BigDecimal.valueOf(191.325);
        BigDecimal oneEnd = BigDecimal.valueOf(191.375);
        BigDecimal twoStart = BigDecimal.valueOf(191.365);
        BigDecimal twoEnd = BigDecimal.valueOf(191.425);
        BigDecimal threeStart = BigDecimal.valueOf(191.325);
        BigDecimal threeEnd = BigDecimal.valueOf(191.425);

        Mockito.when(spectrum.frequencySlots(oneStart, oneEnd)).thenReturn(oneBitSet);
        Mockito.when(spectrum.frequencySlots(twoStart, twoEnd)).thenReturn(twoBitSet);
        Mockito.when(spectrum.frequencySlots(threeStart, threeEnd)).thenReturn(threeBitSet);

        Interval intervalOne = new FrequencyInterval(oneStart, oneEnd);
        Interval intervalTwo = new FrequencyInterval(twoStart, twoEnd);
        Interval intervalThree = new FrequencyInterval(threeStart, threeEnd);

        Set<Interval> collection = new HashSet<>();
        collection.add(intervalOne);
        collection.add(intervalTwo);
        collection.add(intervalThree);

        BitSet expected = new BitSet(768);
        expected.set(24, 28);
        FrequencySpectrumSet frequencySpectrumSet = new FrequencySpectrumSet(spectrum);
        Assertions.assertEquals(expected, frequencySpectrumSet.set(collection, 768));
    }

    @Test
    void emptySet() {
        Spectrum spectrum = Mockito.mock(FrequencySpectrum.class);
        FrequencySpectrumSet frequencySpectrumSet = new FrequencySpectrumSet(spectrum);
        BitSet expected = new BitSet(768);
        Assertions.assertEquals(expected, frequencySpectrumSet.set(new HashSet<>(), 768));
    }
}
