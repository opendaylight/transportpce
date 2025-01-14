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

class SpectrumIndexTest {

    @Test
    void centerFrequencyGranularityEqualToServiceSlotWidth() {

        Index spectrumIndex = new SpectrumIndex();

        Assertions.assertEquals(4, spectrumIndex.firstCenterFrequencyIndex(8, 284, 8));

    }

    @Test
    void centerFrequencyGranularityLessThanServiceSlotWidth() {

        Index spectrumIndex = new SpectrumIndex();

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

        Assertions.assertEquals(12, spectrumIndex.firstCenterFrequencyIndex(16, 284, 8));

    }
}
