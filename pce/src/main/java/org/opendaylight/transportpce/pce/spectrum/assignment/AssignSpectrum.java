/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment;

import java.util.BitSet;
import org.opendaylight.transportpce.pce.spectrum.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssignSpectrum implements  Assign {

    private static final Logger LOG = LoggerFactory.getLogger(AssignSpectrum.class);

    private final Index slotIndex;

    public AssignSpectrum(Index slotIndex) {
        this.slotIndex = slotIndex;
    }

    @Override
    public Range range(int effectiveBits, int baseFrequencyIndex, BitSet spectrumOccupation,
            int centerFrequencyGranularity, int serviceSlotWidth) {

        int firstCenterFrequencyIndex = slotIndex.firstCenterFrequencyIndex(centerFrequencyGranularity,
                baseFrequencyIndex, serviceSlotWidth
        );

        BitSet referenceBitSet = new BitSet(serviceSlotWidth);
        referenceBitSet.set(0, serviceSlotWidth);

        int bandWidth = serviceSlotWidth / 2;
        int lowerFrequencyIndex;
        int upperFrequencyIndex;

        for (int i = firstCenterFrequencyIndex; i < effectiveBits; i += centerFrequencyGranularity) {
            lowerFrequencyIndex = i - bandWidth;
            upperFrequencyIndex = i + bandWidth;

            if (spectrumOccupation.get(lowerFrequencyIndex, upperFrequencyIndex).equals(referenceBitSet)) {
                return new IndexRange(lowerFrequencyIndex, upperFrequencyIndex - 1);
            }

        }

        return new IndexRange(0, 0);
    }
}
