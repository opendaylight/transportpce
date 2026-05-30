/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.range;

import java.util.BitSet;

/**
 * A {@link FrequencyRange} that covers the entire spectrum grid.
 *
 * <p>Used when a node does not constrain the supported frequency range, for
 * example when an XPDR mc-capability-profile does not advertise min/max-edge-freq.
 */
public class EntireGridRange implements FrequencyRange {
    @Override
    public BitSet gridRange(double slotWidthGranularityGHz, double edgeFrequencyTHz, int effectiveBits) {
        BitSet bitSet = new BitSet(effectiveBits);
        bitSet.set(0, effectiveBits);

        return bitSet;
    }
}
