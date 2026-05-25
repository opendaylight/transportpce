/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;
import org.opendaylight.transportpce.pce.spectrum.observer.VoidObserver;

/**
 * MC capability for OpenROADM 7.1 XPDRs.
 *
 * <p>The 7.1 device model WP does not mandate slot-width-granularity or slot counts for
 * transponder mc-capability profiles. Devices that follow the spec may therefore omit
 * those fields, leaving the defaults (50 GHz / 1 slot) which are unusable for flex-grid
 * services. This implementation uses center-freq-granularity as the effective slot-width,
 * which is the correct interpretation for flex-grid transponders.
 */
public class XpdrMcCapability implements McCapability {

    private final BigDecimal centerFreqGranularity;

    public XpdrMcCapability(@NonNull BigDecimal centerFreqGranularity) {
        this.centerFreqGranularity = centerFreqGranularity;
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
    public String toString() {
        return String.format(
            "center-freq-granularity: %sGHz (used as slot-width, per 7.1 XPDR spec)",
            centerFreqGranularity.stripTrailingZeros().toPlainString());
    }
}
