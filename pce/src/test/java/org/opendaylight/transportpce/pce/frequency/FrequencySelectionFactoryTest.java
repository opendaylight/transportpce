/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency;

import java.math.BigDecimal;
import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.frequency.interval.Collection;
import org.opendaylight.transportpce.pce.frequency.interval.EntireSpectrum;
import org.opendaylight.transportpce.pce.frequency.interval.FrequencyInterval;
import org.opendaylight.transportpce.pce.frequency.interval.IntervalCollection;
import org.opendaylight.transportpce.pce.frequency.spectrum.FrequencySpectrum;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.FrequencySpectrumSet;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.SpectrumIndex;

class FrequencySelectionFactoryTest {

    @Test
    void centralFrequencyRangeIsASubsetOfClientRange_centralFrequencyRangeIsChosen() {
        Collection clientRangeWishList = new IntervalCollection(
                new FrequencySpectrumSet(
                        new FrequencySpectrum(
                                new SpectrumIndex(191.325, 6.25, 768),
                                768
                        )
                ),
                768
        );

        Collection clientCentralFrequencyWishList = new IntervalCollection(
                new FrequencySpectrumSet(
                        new FrequencySpectrum(
                                new SpectrumIndex(191.325, 6.25, 768),
                                768
                        )
                ),
                768
        );

        clientRangeWishList.add(new FrequencyInterval(BigDecimal.valueOf(191.4), BigDecimal.valueOf(192.8)));
        clientCentralFrequencyWishList.add(
                new FrequencyInterval(BigDecimal.valueOf(192.45), BigDecimal.valueOf(192.54375)));
        BitSet customerAvailableFrequencyRange = new BitSet(768);
        customerAvailableFrequencyRange.set(0, 769);
        BitSet availableFrequencyRange = new BitSet(768);
        availableFrequencyRange.set(0, 769);

        BitSet expected = new BitSet(768);
        expected.set(180, 195);

        Select frequencySelectionFactory = new FrequencySelectionFactory();

        Assertions.assertEquals(expected, frequencySelectionFactory.availableFrequencies(
                clientRangeWishList,
                clientCentralFrequencyWishList,
                availableFrequencyRange,
                customerAvailableFrequencyRange)
        );

    }


    @Test
    void centralFrequencyRangeIsNotASubsetOfClientRange_noFrequencyRangeIsPossible() {

        Collection clientRangeWishList = new IntervalCollection(
                new FrequencySpectrumSet(
                        new FrequencySpectrum(
                                new SpectrumIndex(191.325, 6.25, 768),
                                768
                        )
                ),
                768
        );

        Collection clientCentralFrequencyWishList = new IntervalCollection(
                new FrequencySpectrumSet(
                        new FrequencySpectrum(
                                new SpectrumIndex(191.325, 6.25, 768),
                                768
                        )
                ),
                768
        );

        clientRangeWishList.add(new FrequencyInterval(BigDecimal.valueOf(195.4), BigDecimal.valueOf(195.8)));
        clientCentralFrequencyWishList.add(
                new FrequencyInterval(BigDecimal.valueOf(192.45), BigDecimal.valueOf(192.54375)));

        BitSet customerAvailableFrequencyRange = new BitSet(768);
        customerAvailableFrequencyRange.set(0, 769);

        BitSet availableFrequencyRange = new BitSet(768);
        availableFrequencyRange.set(0, 769);

        BitSet expected = new BitSet(768);

        Select frequencySelectionFactory = new FrequencySelectionFactory();

        Assertions.assertEquals(expected, frequencySelectionFactory.availableFrequencies(
                clientRangeWishList,
                clientCentralFrequencyWishList,
                availableFrequencyRange,
                customerAvailableFrequencyRange
            )
        );

    }

    @Test
    void clientRangeWishListAndClientCentralFrequencyWishListIsEmpty_emptyBitSet() {
        Collection clientRangeWishList = new IntervalCollection(
                new FrequencySpectrumSet(
                        new FrequencySpectrum(
                                new SpectrumIndex(191.325, 6.25, 768),
                                768
                        )
                ),
                768
        );

        Collection clientCentralFrequencyWishList = new IntervalCollection(
                new FrequencySpectrumSet(
                        new FrequencySpectrum(
                                new SpectrumIndex(191.325, 6.25, 768),
                                768
                        )
                ),
                768
        );

        BitSet customerAvailableFrequencyRange = new BitSet(768);
        customerAvailableFrequencyRange.set(0, 32);

        BitSet availableFrequencyRange = new BitSet(768);
        availableFrequencyRange.set(0, 769);

        BitSet expected = new BitSet(768);

        Select frequencySelectionFactory = new FrequencySelectionFactory();
        BitSet bitSet = frequencySelectionFactory.availableFrequencies(
                clientRangeWishList,
                clientCentralFrequencyWishList,
                availableFrequencyRange,
                customerAvailableFrequencyRange);

        Assertions.assertEquals(expected, bitSet);
    }

    @Test
    void availableCustomerRangeLimitsTheAvailableRange() {

        BitSet customerAvailableFrequencyRange = new BitSet(768);
        customerAvailableFrequencyRange.set(0, 32);

        BitSet availableFrequencyRange = new BitSet(768);
        availableFrequencyRange.set(0, 769);

        BitSet expected = new BitSet(768);
        expected.set(0, 32);

        Collection clientRangeWishList = new EntireSpectrum(768);
        Collection clientCentralFrequencyWishList = new EntireSpectrum(768);

        Select frequencySelectionFactory = new FrequencySelectionFactory();
        BitSet bitSet = frequencySelectionFactory.availableFrequencies(
                clientRangeWishList,
                clientCentralFrequencyWishList,
                availableFrequencyRange,
                customerAvailableFrequencyRange);

        Assertions.assertEquals(expected, bitSet);
    }

}