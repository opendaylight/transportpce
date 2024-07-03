/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.util.BitSet;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class BitMapFrequenciesTest {

    @Test
    void assignedFrequencyRanges() {
        byte[] frequencies = {0, 1, 1, 0, 0, 0, 0, 1};
        byte[] expected = {0, 1, 1, 0, 0, 0, 0, 1};

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertArrayEquals(expected, assignedFrequencies.assignedFrequencyRanges());
    }

    @Test
    void assignedFrequencyRangesAreInMutable() {
        byte[] frequencies = {0, 1, 1, 0, 0, 0, 0, 1};
        byte[] expected = {0, 1, 1, 0, 0, 0, 0, 1};

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        //Assert this doesn't affect the contents of the assignedFrequencies object
        frequencies[0] = 1;

        Assert.assertArrayEquals(expected, assignedFrequencies.assignedFrequencyRanges());
    }


    @Test
    void assignedFrequenciesStart() {
        byte[] frequencies = {1, 0, 0, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(0, 8);

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.assignedFrequencies());
    }

    @Test
    void assignedFrequenciesMiddle() {
        byte[] frequencies = {0, 1, 0, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(8, 16);

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.assignedFrequencies());
    }

    @Test
    void assignedFrequenciesConsecutive() {
        byte[] frequencies = {0, 1, 1, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();
        expected.set(8, 24);

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.assignedFrequencies());
    }

    @Test
    void assignedFrequenciesEnd() {
        byte[] frequencies = {0, 0, 0, 0, 0, 0, 0, 56};
        BitSet expected = new BitSet();
        expected.set(56, 64);

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.assignedFrequencies());
    }

    @Test
    void assignedFrequenciesAll() {
        byte[] frequencies = {1, 1, 1, 1, 1, 1, 1, 1};
        BitSet expected = new BitSet();
        expected.set(0, 64);

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.assignedFrequencies());
    }

    @Test
    void assignedFrequenciesNone() {
        byte[] frequencies = {0, 0, 0, 0, 0, 0, 0, 0};
        BitSet expected = new BitSet();

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.assignedFrequencies());
    }

    @Test
    void availableFrequencies() {
        byte[] frequencies = {0, 1, 0, 0, 0, 0, 0, 0};

        BitSet expected = new BitSet();
        expected.set(0, 8);
        expected.set(16, 64);

        BitMapFrequencies assignedFrequencies = new BitMapFrequencies(frequencies);

        Assert.assertEquals(expected, assignedFrequencies.availableFrequencies());
    }
}