/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

class EntireSpectrumTest {

    @Test
    void add() {
        Collection entireSpectrum = new EntireSpectrum(768);
        Interval interval = mock(Interval.class);
        assertTrue(entireSpectrum.add(interval));
    }

    @Test
    void subset() {
        Collection entireSpectrum = new EntireSpectrum(768);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        assertEquals(expected, entireSpectrum.subset(expected));
    }

    @Test
    void testSubset() {
        Collection entireSpectrum = new EntireSpectrum(768);
        Collection collection = mock(Collection.class);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        when(collection.set()).thenReturn(expected);

        assertEquals(expected, entireSpectrum.subset(collection));
    }

    @Test
    void intersection() {
        Collection entireSpectrum = new EntireSpectrum(768);
        Collection collection = mock(Collection.class);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        when(collection.set()).thenReturn(expected);

        assertEquals(expected, entireSpectrum.intersection(expected));
    }

    @Test
    void testIntersection() {
        Collection entireSpectrum = new EntireSpectrum(768);
        Collection collection = mock(Collection.class);

        BitSet expected = new BitSet();
        expected.set(16, 24);

        when(collection.set()).thenReturn(expected);

        assertEquals(expected, entireSpectrum.intersection(collection));
    }

    @Test
    void set() {
        Collection entireSpectrum = new EntireSpectrum(768);

        BitSet expected = new BitSet();
        expected.set(0, 768);

        assertEquals(expected, entireSpectrum.set());
    }

    @Test
    void bitset() {
        Collection entireSpectrum = new EntireSpectrum(768);

        BitSet expected = new BitSet();
        expected.set(0, 768);

        assertEquals(expected, entireSpectrum.set());
    }

    @Test
    void size() {
        Collection entireSpectrum = new EntireSpectrum(768);

        assertEquals(1, entireSpectrum.size());
    }
}