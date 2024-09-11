/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.yangtools.yang.common.Uint64;

class SortedRangeTest {

    @Test
    void addValidRangeReturnTrue() {
        Frequency lowerFrequency = new TeraHertz(191.35);
        Frequency upperFrequency = new TeraHertz(191.40);
        Range range = new SortedRange();

        assertTrue(range.add(lowerFrequency, upperFrequency));
    }

    @Test
    void addInValidRangeReturnFalse() {
        Frequency lowerFrequency = new TeraHertz(191.40);
        Frequency upperFrequency = new TeraHertz(191.35);
        Range range = new SortedRange();

        assertThrows(InvalidFrequencyRangeException.class, () -> range.add(lowerFrequency, upperFrequency));
    }

    @Test
    void addValidRangeInDoubleReturnTrue() {
        Range range = new SortedRange();

        assertTrue(range.add(191.35, 191.40));
    }

    @Test
    void addNonOverlappingFrequencyRangeReturnTrue() {
        Frequency lowerFrequency1 = new TeraHertz(191.35);
        Frequency upperFrequency1 = new TeraHertz(191.40);

        Frequency lowerFrequency2 = new TeraHertz(191.40);
        Frequency upperFrequency2 = new TeraHertz(191.50);

        Frequency lowerFrequency3 = new TeraHertz(191.30);
        Frequency upperFrequency3 = new TeraHertz(191.35);

        Range range = new SortedRange();

        assertTrue(range.add(lowerFrequency1, upperFrequency1));
        assertTrue(range.add(lowerFrequency2, upperFrequency2));
        assertTrue(range.add(lowerFrequency3, upperFrequency3));
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Test
    void addOverlappingFrequencyRangeReturnFalse() {
        Frequency lowerFrequency1 = new TeraHertz(191.35);
        Frequency upperFrequency1 = new TeraHertz(191.40);

        Frequency lowerFrequency2 = new TeraHertz(191.36);
        Frequency upperFrequency2 = new TeraHertz(191.41);

        Frequency lowerFrequency3 = new TeraHertz(191.34);
        Frequency upperFrequency3 = new TeraHertz(191.39);

        Frequency lowerFrequency4 = new TeraHertz(191.34);
        Frequency upperFrequency4 = new TeraHertz(191.41);

        Frequency lowerFrequency5 = new TeraHertz(191.35);
        Frequency upperFrequency5 = new TeraHertz(191.39);

        Range range = new SortedRange();

        assertTrue(range.add(lowerFrequency1, upperFrequency1));
        assertFalse(range.add(lowerFrequency2, upperFrequency2));
        assertFalse(range.add(lowerFrequency3, upperFrequency3));
        assertFalse(range.add(lowerFrequency4, upperFrequency4));
        assertFalse(range.add(lowerFrequency5, upperFrequency5));
    }

    @Test
    void addRangeAsCenterFrequencyAndWidth() {
        Factory factory = mock(Factory.class);
        when(factory.lower(191.35, 50.0)).thenReturn(new TeraHertz(191.325));
        when(factory.upper(191.35, 50.0)).thenReturn(new TeraHertz(191.375));

        Range range = new SortedRange();
        range.add(191.35, 50.0, factory);

        Range expected = new SortedRange();
        expected.add(new TeraHertz(191.325), new TeraHertz(191.375));

        assertEquals(expected.ranges(), range.ranges());
    }

    @Test
    void addRange() {
        Range range1 = new SortedRange();
        range1.add(191.325, 191.375);

        Range range2 = new SortedRange();
        range2.add(range1);

        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(191.325), new TeraHertz(191.375));

        assertEquals(expected, range2.ranges());
    }


    @Test
    void asTeraHertz() {
        Frequency lowerFrequency1 = new TeraHertz(191.35);
        Frequency upperFrequency1 = new TeraHertz(191.40);

        Frequency lowerFrequency2 = new TeraHertz(191.40);
        Frequency upperFrequency2 = new TeraHertz(191.50);

        Frequency lowerFrequency3 = new TeraHertz(191.30);
        Frequency upperFrequency3 = new TeraHertz(191.35);

        Range range = new SortedRange();
        range.add(lowerFrequency1, upperFrequency1);
        range.add(lowerFrequency2, upperFrequency2);
        range.add(lowerFrequency3, upperFrequency3);

        Map<Double, Double> expected = Map.of(191.30, 191.35,191.35, 191.40, 191.40, 191.50);

        assertEquals(expected, range.asTeraHertz());
    }

    @Test
    void asHertz() {
        Frequency lowerFrequency1 = new TeraHertz(191.35);
        Frequency upperFrequency1 = new TeraHertz(191.40);

        Frequency lowerFrequency2 = new TeraHertz(191.40);
        Frequency upperFrequency2 = new TeraHertz(191.50);

        Frequency lowerFrequency3 = new TeraHertz(191.30);
        Frequency upperFrequency3 = new TeraHertz(191.35);

        Range range = new SortedRange();
        range.add(lowerFrequency1, upperFrequency1);
        range.add(lowerFrequency2, upperFrequency2);
        range.add(lowerFrequency3, upperFrequency3);

        Map<Uint64, Uint64> expected = Map.of(
                Uint64.valueOf(191300000000000L), Uint64.valueOf(191350000000000L),
                Uint64.valueOf(191350000000000L), Uint64.valueOf(191400000000000L),
                Uint64.valueOf(191400000000000L), Uint64.valueOf(191500000000000L)
        );

        assertEquals(expected, range.asHertz());
    }

}
