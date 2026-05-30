/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import java.util.BitSet;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;

/**
 * MC capability for node types that do not constrain the optical spectrum,
 * such as OTN nodes. All service widths are considered compatible.
 */
public class UnconstrainedMcCapability implements McCapability {

    @Override
    public BigDecimal centerFrequencyGranularity() {
        return null;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz) {
        return true;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer) {
        return true;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz) {
        return true;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz, Observer observer) {
        return true;
    }

    @Override
    public BitSet supportableFrequencyRange(
            double slotWidthGranularityGHz,
            double edgeFrequencyTHz,
            int effectiveBits) {

        BitSet bitSet = new BitSet(effectiveBits);
        bitSet.set(0, effectiveBits);

        return bitSet;
    }

    @Override
    public String toString() {
        return "unconstrained (OTN node)";
    }
}
