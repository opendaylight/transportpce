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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;
import org.opendaylight.transportpce.pce.spectrum.observer.VoidObserver;
import org.opendaylight.transportpce.pce.spectrum.range.FrequencyRange;

/**
 * MC capability for OpenROADM 7.1 XPDRs.
 *
 * <p>OpenROADM Device White Paper (Release 7.1), Section 4.8.3:
 *
 * <p>Xponders use an MC Capability Profile (mc-capability-profile) to
 * advertise the frequency provisioning range and frequency granularity
 * supported by their network ports.
 *
 * <p>For xponders, only the frequency-related attributes are relevant:
 *   - min-edge-freq
 *   - max-edge-freq
 *   - center-freq-granularity
 *
 * <p>The spectrum allocation attributes:
 *   - slot-width-granularity
 *   - min-slots
 *   - max-slots
 *
 * <p>are not used for xponders.
 *
 * <p>MC capabilities are required only on the transponder network port and
 * are used by the controller when selecting and provisioning the OCH/OTSi
 * frequency.
 */
public class XpdrMcCapability implements McCapability {

    private final BigDecimal centerFreqGranularity;

    private final FrequencyRange supportedFrequencyRange;

    public XpdrMcCapability(
            @NonNull BigDecimal centerFreqGranularity,
            @NonNull FrequencyRange supportedFrequencyRange) {

        this.centerFreqGranularity = centerFreqGranularity;
        this.supportedFrequencyRange = supportedFrequencyRange;
    }

    @Override
    public BigDecimal centerFrequencyGranularity() {
        return centerFreqGranularity;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz) {
        return isCompatibleWithServiceFrequency(requiredFrequencyWidthGHz, new VoidObserver());
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer) {
        return true;
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz) {
        return isCompatibleWithServiceFrequency(BigDecimal.valueOf(requiredFrequencyWidthGHz), new VoidObserver());
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(double requiredFrequencyWidthGHz, Observer observer) {
        return isCompatibleWithServiceFrequency(BigDecimal.valueOf(requiredFrequencyWidthGHz), observer);
    }

    @Override
    public BitSet supportableFrequencyRange(
            double slotWidthGranularityGHz,
            double edgeFrequencyTHz,
            int effectiveBits) {

        return supportedFrequencyRange.gridRange(slotWidthGranularityGHz, edgeFrequencyTHz, effectiveBits);
    }

    @Override
    public String toString() {
        return String.format(
            "center-freq-granularity: %sGHz (minimum center frequency spacing)",
            centerFreqGranularity.stripTrailingZeros().toPlainString());
    }
}
