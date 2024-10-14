/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
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

class EntireSpectrumTest {

    @Test
    void add() {
        Collection entireSpectrum = new EntireSpectrum(768);
        Interval interval = Mockito.mock(Interval.class);
        Assertions.assertTrue(entireSpectrum.add(interval));
    }

    @Test
    void subset() {
        Collection entireSpectrum = new EntireSpectrum(768);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        Assertions.assertEquals(expected, entireSpectrum.subset(expected));
    }

    @Test
    void testSubset() {

        Collection entireSpectrum = new EntireSpectrum(768);

        Collection collection = Mockito.mock(Collection.class);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        Mockito.when(collection.set()).thenReturn(expected);

        Assertions.assertEquals(expected, entireSpectrum.subset(collection));

    }

    @Test
    void intersection() {
        Collection entireSpectrum = new EntireSpectrum(768);

        Collection collection = Mockito.mock(Collection.class);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        Mockito.when(collection.set()).thenReturn(expected);

        Assertions.assertEquals(expected, entireSpectrum.intersection(expected));
    }

    @Test
    void testIntersection() {
        Collection entireSpectrum = new EntireSpectrum(768);

        Collection collection = Mockito.mock(Collection.class);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        Mockito.when(collection.set()).thenReturn(expected);

        Assertions.assertEquals(expected, entireSpectrum.intersection(collection));
    }

    @Test
    void set() {
        Collection entireSpectrum = new EntireSpectrum(768);

        BitSet expected = new BitSet();
        expected.set(0, 768);

        Assertions.assertEquals(expected, entireSpectrum.set());
    }

    @Test
    void bitset() {
        Collection entireSpectrum = new EntireSpectrum(768);

        BitSet expected = new BitSet();
        expected.set(0, 768);

        Assertions.assertEquals(expected, entireSpectrum.set());
    }

    @Test
    void size() {
        Collection entireSpectrum = new EntireSpectrum(768);

        Assertions.assertEquals(1, entireSpectrum.size());
    }
}