/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.spectrum.index.Index;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;

// These tests will verify the first possible center frequency spectrum index.
class SpectrumIndexTest {

    @Test
    void centerFrequencyGranularityEqualToServiceSlotWidth() {

        Index spectrumIndex = new SpectrumIndex();

        //Given that...
        // 1) ... the base frequency slot index (e.g. 193.1) is found at index 284
        // 2) ... and the center frequency granularity slot width is 8
        // 3) ... and the service frequency slot width is 8 (e.g 8 x 6.25 = 50GHz)
        // we expect the first center frequency slot at index 4
        // (i.e. possible center frequencies are at 4, 12, 20, ...,268, 276, 284, ...)
        Assertions.assertEquals(4, spectrumIndex.firstCenterFrequencyIndex(8, 284, 8));

    }

    @Test
    void centerFrequencyGranularityLessThanServiceSlotWidth() {

        Index spectrumIndex = new SpectrumIndex();

        //Given that...
        // 1) ... the base frequency slot index (e.g. 193.1) is found at index 284
        // 2) ... and the center frequency granularity slot width is 2
        // 3) ... and the service frequency slot width is 8 (e.g 8 x 6.25 = 50GHz)
        // we expect the first center frequency slot at index 4.
        // A service of 8 slots need a center frequency ± 4 slots. Choosing center frequency slot index = 2
        // will not work since all indexes >= 0.
        Assertions.assertEquals(4, spectrumIndex.firstCenterFrequencyIndex(2, 284, 8));

    }

    @Test
    void minimumCenterFrequencyGranularityLessThanServiceSlotWidth() {

        Index spectrumIndex = new SpectrumIndex();

        Assertions.assertEquals(4, spectrumIndex.firstCenterFrequencyIndex(1, 284, 8));

    }

    @Test
    void centerFrequencyGranularityMoreThanServiceSlotWidth() {

        Index spectrumIndex = new SpectrumIndex();

        //Given that...
        // 1) ... the base frequency slot index (e.g. 193.1) is found at index 284
        // 2) ... and the center frequency granularity slot width is 2
        // 3) ... and the service frequency slot width is 8 (e.g 8 x 6.25 = 50GHz)
        // we expect the first center frequency slot at index 12
        // (i.e. possible center frequencies are at 12, 28, 44, ...,252, 268, 284, ...)
        Assertions.assertEquals(12, spectrumIndex.firstCenterFrequencyIndex(16, 284, 8));

    }

    @Test
    void centerFrequencyGranularityLessThanServiceSlotWidths() {

        Index spectrumIndex = new SpectrumIndex();

        //Given that...
        // 1) ... the base frequency slot index (e.g. 193.1) is found at index 284
        // 2) ... and the center frequency granularity slot width is 2
        // 3) ... and the service frequency slot width is 38 (e.g 26 x 6.25 = 162.5GHz)
        // we expect the first center frequency slot at index 12
        // (i.e. possible center frequencies are at 28, 44, ...,252, 268, 284, ...)
        Assertions.assertEquals(28, spectrumIndex.firstCenterFrequencyIndex(16, 284, 26));

    }

    @Test
    void centerFrequencyGranularityMoreThanServiceSlotWidthDifferentReferenceIndex() {

        Index spectrumIndex = new SpectrumIndex();

        //Given that...
        // 1) ... the base frequency slot index (e.g. 193.1) is found at index 280 (as opposed to 284)
        // 2) ... and the center frequency granularity slot width is 16
        // 3) ... and the service frequency slot width is 8 (e.g 8 x 6.25 = 50GHz)
        // we expect the first center frequency slot at index 8
        //(i.e. possible center frequencies are at 8, 24, 40, ...,248, 264, 280, ...)
        Assertions.assertEquals(8, spectrumIndex.firstCenterFrequencyIndex(16, 280, 8));

    }
}
