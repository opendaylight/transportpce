/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.FrequencySpectrumSet;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;

class IntervalCollectionTest {

    @Test
    public void twoEqualFrequencyRanges_returnIntersectionWithAvailableBitset() {

        // The mock is hardwired to return true when determining
        // whether a bitset is a subset of available bitset.
        Spectrum spectrum = Mockito.mock(Spectrum.class);
        Mockito.when(spectrum.isSubset(Mockito.any(), Mockito.any())).thenReturn(true);

        IntervalCollection intervalCollection = new IntervalCollection(new FrequencySpectrumSet(spectrum), 768);
        FrequencyTHz startOne = FrequencyTHz.getDefaultInstance("194.1");
        FrequencyTHz endOne = FrequencyTHz.getDefaultInstance("194.2");

        // First bitset that is a subset of the available bitset further down below.
        // This is a fake bitset intended to represent the range 194.1 - 194.2.
        // The point is to test how intersections are processed and for that purpose
        // it doesn't matter if this bitset actually represents the range 194.1 - 194.2.
        BitSet one = new BitSet();
        one.set(10, 20);

        //Wiring up the mock to return the (fake) BitSet one in case the input
        // is the range 194.1 - 194.2.
        Mockito.when(
                spectrum.frequencySlots(
                        startOne.getValue().decimalValue(),
                        endOne.getValue().decimalValue()
                )).thenReturn(one);

        intervalCollection.add(new FrequencyInterval(startOne, endOne));

        // Another fake bitset with the exact same range as BitSet one
        FrequencyTHz startTwo = FrequencyTHz.getDefaultInstance("194.1");
        FrequencyTHz endTwo = FrequencyTHz.getDefaultInstance("194.2");

        intervalCollection.add(new FrequencyInterval(startTwo, endTwo));

        //This is a bitset representing the available frequencies (e.g. the
        //available frequencies on the nodes).
        BitSet available = new BitSet();
        available.set(9, 42);

        //The expected result is the intersection between BitSet one and BitSet available
        //BitSet two is identical to BitSet one and shouldn't affect anything.
        BitSet expected = new BitSet();
        expected.set(10, 20);

        Assertions.assertEquals(expected, intervalCollection.intersection(available));

        //Even though we've added two ranges, we don't expect more than one range to be processed
        //because the ranges are identical.
        Mockito.verify(spectrum, Mockito.times(1)).frequencySlots(
                startOne.getValue().decimalValue(),
                endOne.getValue().decimalValue()
        );
    }

    @Test
    public void twoFrequencyRangesNotSubsetOfAvailable_returnEmptyBitset() {

        // The mock is hardwired to return false when determining
        // whether a bitset is a subset of available bitset.
        Spectrum spectrum = Mockito.mock(Spectrum.class);
        Mockito.when(spectrum.isSubset(Mockito.any(), Mockito.any())).thenReturn(false);

        IntervalCollection intervalCollection = new IntervalCollection(new FrequencySpectrumSet(spectrum), 768);
        FrequencyTHz startOne = FrequencyTHz.getDefaultInstance("194.1");
        FrequencyTHz endOne = FrequencyTHz.getDefaultInstance("194.2");

        // First bitset that is not a subset of the available bitset further down below.
        BitSet one = new BitSet();
        one.set(10, 20);
        Mockito.when(
                spectrum.frequencySlots(
                        startOne.getValue().decimalValue(),
                        endOne.getValue().decimalValue()
                )).thenReturn(one);

        intervalCollection.add(new FrequencyInterval(startOne, endOne));

        FrequencyTHz startTwo = FrequencyTHz.getDefaultInstance("194.4");
        FrequencyTHz endTwo = FrequencyTHz.getDefaultInstance("194.5");

        // Second bitset that is not a subset of the available bitset further down below.
        BitSet two = new BitSet();
        two.set(30, 40);
        Mockito.when(
                spectrum.frequencySlots(
                        startTwo.getValue().decimalValue(),
                        endTwo.getValue().decimalValue()
                )).thenReturn(two);

        intervalCollection.add(new FrequencyInterval(startTwo, endTwo));

        // Note that one and two are not a subset of the available bitset.
        BitSet available = new BitSet();
        available.set(9, 42);

        // Since one and two are not a subset of the available bitset,
        // we expect an empty bitset returned.
        BitSet expected = new BitSet();
        Assertions.assertEquals(expected, intervalCollection.subset(available));

    }


    @Test
    public void rangesThatAreNotASubsetOfAvailableBitset_returnsAnEmptyBitset() {

        Spectrum spectrum = Mockito.mock(Spectrum.class);

        IntervalCollection intervalCollection = new IntervalCollection(new FrequencySpectrumSet(spectrum), 768);
        FrequencyTHz startOne = FrequencyTHz.getDefaultInstance("194.1");
        FrequencyTHz endOne = FrequencyTHz.getDefaultInstance("194.2");

        BitSet one = new BitSet();
        one.set(10, 20);
        Mockito.when(
                spectrum.frequencySlots(
                        startOne.getValue().decimalValue(),
                        endOne.getValue().decimalValue()
                )).thenReturn(one);

        intervalCollection.add(new FrequencyInterval(startOne, endOne));

        BitSet available = new BitSet();
        available.set(15, 42);

        BitSet expected = new BitSet();

        Assertions.assertEquals(expected, intervalCollection.subset(available));

    }

    @Test
    public void rangesThatAreWithinTheIntersection_returnsIntersection() {

        Spectrum spectrum = Mockito.mock(Spectrum.class);

        IntervalCollection intervalCollection = new IntervalCollection(new FrequencySpectrumSet(spectrum), 768);
        FrequencyTHz startOne = FrequencyTHz.getDefaultInstance("194.1");
        FrequencyTHz endOne = FrequencyTHz.getDefaultInstance("194.2");

        BitSet one = new BitSet();
        one.set(10, 20);
        Mockito.when(
                spectrum.frequencySlots(
                        startOne.getValue().decimalValue(),
                        endOne.getValue().decimalValue()
                )).thenReturn(one);

        intervalCollection.add(new FrequencyInterval(startOne, endOne));

        BitSet available = new BitSet();
        available.set(15, 42);

        BitSet expected = new BitSet();
        expected.set(15, 20);

        Assertions.assertEquals(expected, intervalCollection.intersection(available));

    }

    @Test
    public void rangesThatAreNotASubsetOfAvailableBitset_returnsAnEmptyBitSet() {

        BitSet emptyBitSet = new BitSet();
        emptyBitSet.set(112, 144);

        FrequencyTHz startOne = FrequencyTHz.getDefaultInstance("192.1");
        FrequencyTHz endOne = FrequencyTHz.getDefaultInstance("192.2");

        Spectrum spectrum = Mockito.mock(Spectrum.class);
        Mockito.when(
                spectrum.frequencySlots(
                        startOne.getValue().decimalValue(),
                        endOne.getValue().decimalValue()
                )
        ).thenReturn(emptyBitSet);

        IntervalCollection intervalCollection = new IntervalCollection(new FrequencySpectrumSet(spectrum), 768);
        intervalCollection.add(new FrequencyInterval(startOne, endOne));

        BitSet available = new BitSet();
        available.set(0, 96);

        BitSet expected = new BitSet();

        Assertions.assertEquals(expected, intervalCollection.intersection(available));

    }

    @Test
    public void assertTheIntersectionOfTheAvailableBitsetAndFrequencyCollectionIsReturned() {

        Spectrum spectrum = Mockito.mock(Spectrum.class);
        Mockito.when(spectrum.isSubset(Mockito.any(), Mockito.any())).thenReturn(true);

        IntervalCollection intervalCollection = new IntervalCollection(new FrequencySpectrumSet(spectrum), 768);
        FrequencyTHz startOne = FrequencyTHz.getDefaultInstance("194.1");
        FrequencyTHz endOne = FrequencyTHz.getDefaultInstance("194.2");

        BitSet one = new BitSet();
        one.set(15, 20);
        Mockito.when(
                spectrum.frequencySlots(
                        startOne.getValue().decimalValue(),
                        endOne.getValue().decimalValue()
                )).thenReturn(one);

        intervalCollection.add(new FrequencyInterval(startOne, endOne));

        BitSet available = new BitSet();
        available.set(10, 42);

        BitSet expected = new BitSet();
        expected.set(15, 20);

        Assertions.assertEquals(expected, intervalCollection.intersection(available));

    }
}
