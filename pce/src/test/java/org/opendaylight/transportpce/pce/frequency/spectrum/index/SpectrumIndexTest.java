/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.common.Decimal64;

class SpectrumIndexTest {

    @Test
    void testAllIndexes() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);

        for (int i = 0; i <= 768; i++) {
            Decimal64 frequency = Decimal64.valueOf(
                    BigDecimal.valueOf(191.325).add(
                            BigDecimal.valueOf(i)
                            .multiply(BigDecimal.valueOf(6.25)
                            .multiply(BigDecimal.valueOf(0.001)))
                    )
            );
            Assertions.assertEquals(i, spectrumSlots.index(frequency));
        }
    }

    @Test
    void indexFrequency() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("191.375");
        Assertions.assertEquals(8, spectrumSlots.index(frequency));
    }

    @Test
    void testFrequencyTooLow_ThrowException() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("191.324");
        Assertions.assertThrows(IllegalArgumentException.class, () -> spectrumSlots.index(frequency));
    }

    @Test
    void testLowestPossibleFrequency() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("191.325");
        Assertions.assertEquals(0, spectrumSlots.index(frequency));
    }

    @Test
    void testHighestPossibleFrequency() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("196.125");
        Assertions.assertEquals(768, spectrumSlots.index(frequency));
    }

    @Test
    void testFrequencyTooHigh_ThrowException() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("196.126");
        Assertions.assertThrows(IllegalArgumentException.class, () -> spectrumSlots.index(frequency));
    }

    @Test
    void testNullFrequency_ThrowException() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Assertions.assertThrows(IllegalArgumentException.class, () -> spectrumSlots.index(null));
    }

    @Test
    void testNullIndexFrequency_ThrowException() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Assertions.assertThrows(IllegalArgumentException.class, () -> spectrumSlots.index(191.325, 6.25, 768, null));
    }

    @Test
    void testNullWidth_ThrowException() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("191.375");
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> spectrumSlots.index(191.325, null, 768, frequency)
        );
    }

    @Test
    void testNullEdgeBandFrequency_ThrowException() {
        SpectrumIndex spectrumSlots = new SpectrumIndex(191.325, 6.25, 768);
        Decimal64 frequency = Decimal64.valueOf("191.375");
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> spectrumSlots.index(null, 6.25, 768, frequency)
        );
    }
}