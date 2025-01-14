/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.index;

public class SpectrumIndex implements Index {

    @Override
    public int firstCenterFrequencyIndex(
        int centerFrequencySlotWidth, int baseFrequencySlotIndex, int serviceSlotWidth) {

        if (serviceSlotWidth % 2 != 0) {
            throw new RuntimeException("Cannot process an odd serviceSlotWidth");
        }

        int slotIndexCandidate = (baseFrequencySlotIndex % centerFrequencySlotWidth);

        while (slotIndexCandidate < serviceSlotWidth / 2) {
            slotIndexCandidate += centerFrequencySlotWidth;
        }

        return slotIndexCandidate;
    }
}
