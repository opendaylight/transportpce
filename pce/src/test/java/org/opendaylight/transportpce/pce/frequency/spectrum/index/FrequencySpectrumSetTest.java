/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.frequency.interval.FrequencyInterval;
import org.opendaylight.transportpce.pce.frequency.interval.Interval;
import org.opendaylight.transportpce.pce.frequency.spectrum.FrequencySpectrum;
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
        assertEquals(expected, frequencySpectrumSet.subset(realSubset, available, 768));
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
}