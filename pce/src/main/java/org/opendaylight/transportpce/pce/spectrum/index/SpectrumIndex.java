/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpectrumIndex implements Index {

    private static final Logger LOG = LoggerFactory.getLogger(SpectrumIndex.class);

    @Override
    public int firstCenterFrequencyIndex(
            int centerFrequencySlotWidth, int baseFrequencySlotIndex, int serviceSlotWidth) {

        if (serviceSlotWidth % 2 != 0) {
            throw new ServiceSlotWidthException(
                    String.format("Cannot process an odd service slot width: %s", serviceSlotWidth));
        }

        int slotIndexCandidate = (baseFrequencySlotIndex % centerFrequencySlotWidth);

        while (slotIndexCandidate < serviceSlotWidth / 2) {
            slotIndexCandidate += centerFrequencySlotWidth;
        }

        LOG.info("First possible center frequency slot index {} "
                        + "(center frequency slot width {},"
                        + "base frequency slot index {},"
                        + "service slot width {}",
                slotIndexCandidate, centerFrequencySlotWidth, baseFrequencySlotIndex, serviceSlotWidth);

        return slotIndexCandidate;
    }

    @Override
    public int lastCenterFrequencyIndex(int centerFrequencySlotWidth, int baseFrequencySlotIndex, int serviceSlotWidth,
            int effectiveBits) {

        if (serviceSlotWidth % 2 != 0) {
            throw new ServiceSlotWidthException(
                    String.format("Cannot process an odd service slot width: %s", serviceSlotWidth));
        }

        int slotIndexCandidate =
                effectiveBits - (((effectiveBits - baseFrequencySlotIndex) % centerFrequencySlotWidth));

        while ((effectiveBits - slotIndexCandidate) < serviceSlotWidth / 2) {
            slotIndexCandidate -= centerFrequencySlotWidth;
        }

        LOG.info("Last possible center frequency slot index {} "
                        + "(center frequency slot width {},"
                        + "base frequency slot index {},"
                        + "service slot width {},"
                        + "effective bits {})",
                slotIndexCandidate, centerFrequencySlotWidth, baseFrequencySlotIndex, serviceSlotWidth, effectiveBits);

        return slotIndexCandidate;
    }
}
