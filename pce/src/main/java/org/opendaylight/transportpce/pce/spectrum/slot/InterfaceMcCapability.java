/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.slot;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.pce.spectrum.observer.Observer;
import org.opendaylight.transportpce.pce.spectrum.observer.VoidObserver;
import org.opendaylight.transportpce.pce.spectrum.range.EntireGridRange;
import org.opendaylight.transportpce.pce.spectrum.range.FrequencyRange;

public class InterfaceMcCapability implements McCapability {

    private final String node;

    private final BigDecimal slotWidthGranularity;

    private final BigDecimal centerFrequencyGranularity;

    private final int minSlots;

    private final int maxSlots;

    private final FrequencyRange supportedFrequencyRange;

    /**
     * Create a NodeMcCapability object with default values defined in the yang model:
     * - CenterFrequencyGranularity = 50(GHz).
     * - SlotWidthFrequencyGranularity = 50(GHz).
     * - min and max slots set to 1.
     */
    public InterfaceMcCapability() {
        this("Unknown node", BigDecimal.valueOf(50), 1, 1, BigDecimal.valueOf(50),
                new EntireGridRange());
    }

    public InterfaceMcCapability(BigDecimal slotWidthGranularity, int minSlots, int maxSlots,
            FrequencyRange supportedFrequencyRange) {

        this("Unknown node", slotWidthGranularity, minSlots, maxSlots, slotWidthGranularity,
                supportedFrequencyRange);
    }

    public InterfaceMcCapability(@NonNull String node, BigDecimal slotWidthGranularity, int minSlots, int maxSlots,
            FrequencyRange supportedFrequencyRange) {
        this(node, slotWidthGranularity, minSlots, maxSlots, slotWidthGranularity, supportedFrequencyRange);
    }

    public InterfaceMcCapability(@NonNull String node, BigDecimal slotWidthGranularity, int minSlots, int maxSlots,
            BigDecimal centerFrequencyGranularity, FrequencyRange supportedFrequencyRange) {
        this.node = node;
        this.slotWidthGranularity = slotWidthGranularity;
        this.centerFrequencyGranularity = centerFrequencyGranularity;
        this.minSlots = minSlots;
        this.maxSlots = maxSlots;
        this.supportedFrequencyRange = supportedFrequencyRange;
    }

    public InterfaceMcCapability(
            BigDecimal slotWidthGranularity, BigDecimal centerFrequencyGranularity, int minSlots, int maxSlots,
            FrequencyRange supportedFrequencyRange) {

        this("Unknown node", slotWidthGranularity, minSlots, maxSlots, centerFrequencyGranularity,
                supportedFrequencyRange);
    }

    public InterfaceMcCapability(
            double slotWidthGranularity,
            int minSlots,
            int maxSlots,
            FrequencyRange supportedFrequencyRange) {

        this(BigDecimal.valueOf(slotWidthGranularity), minSlots, maxSlots, supportedFrequencyRange);
    }

    public InterfaceMcCapability(@NonNull String node, double slotWidthGranularity, int minSlots, int maxSlots,
            FrequencyRange supportedFrequencyRange) {

        this(node, BigDecimal.valueOf(slotWidthGranularity), minSlots, maxSlots, supportedFrequencyRange);
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz) {
        return isCompatibleWithServiceFrequency(requiredFrequencyWidthGHz, new VoidObserver());
    }

    @Override
    public boolean isCompatibleWithServiceFrequency(BigDecimal requiredFrequencyWidthGHz, Observer observer) {

        BigDecimal remainder = requiredFrequencyWidthGHz.remainder(slotWidthGranularity);

        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            observer.error(unsupportedServiceFrequency(requiredFrequencyWidthGHz));

            return false;
        }

        BigDecimal quotient = requiredFrequencyWidthGHz.divideToIntegralValue(slotWidthGranularity);

        if (quotient.compareTo(BigDecimal.valueOf(minSlots)) >= 0
                && quotient.compareTo(BigDecimal.valueOf(maxSlots)) <= 0) {

            return true;
        }

        observer.error(unsupportedServiceFrequency(requiredFrequencyWidthGHz));

        return false;
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
    public BigDecimal centerFrequencyGranularity() {
        return centerFrequencyGranularity;
    }

    @Override
    public BitSet supportableFrequencyRange(
            double slotWidthGranularityGHz,
            double edgeFrequencyTHz,
            int effectiveBits) {

        return supportedFrequencyRange.gridRange(slotWidthGranularityGHz, edgeFrequencyTHz, effectiveBits);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof InterfaceMcCapability interfaceMcCapability)) {
            return false;
        }
        return node.equals(interfaceMcCapability.node)
                && minSlots == interfaceMcCapability.minSlots
                && maxSlots == interfaceMcCapability.maxSlots
                && Objects.equals(slotWidthGranularity, interfaceMcCapability.slotWidthGranularity)
                && Objects.equals(centerFrequencyGranularity, interfaceMcCapability.centerFrequencyGranularity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotWidthGranularity, centerFrequencyGranularity, minSlots, maxSlots);
    }

    @Override
    public String toString() {
        return String.format(
            "slot-width-granularity: %sGHz, center-freq-granularity: %sGHz, slots: %s..%s",
            slotWidthGranularity != null ? slotWidthGranularity.stripTrailingZeros().toPlainString() : "null",
            centerFrequencyGranularity != null
                    ? centerFrequencyGranularity.stripTrailingZeros().toPlainString() : "null",
            minSlots,
            maxSlots);
    }

    private String unsupportedServiceFrequency(BigDecimal requiredFrequencyWidthGHz) {
        return String.format("%s does not support a service slot width of %sGHz (%s supports "
                        + "slot-width-granularity: %sGHz, and min-slots: %s, and max-slots %s, i.e. slot width: %s).",
                node,
                requiredFrequencyWidthGHz.stripTrailingZeros().toPlainString(),
                node,
                slotWidthGranularity.stripTrailingZeros().toPlainString(),
                minSlots,
                maxSlots,
                slotWidthRange(minSlots, maxSlots, slotWidthGranularity));
    }

    private String slotWidthRange(long minSlotNb, long maxSlotNb, BigDecimal slotWidthGran) {
        BigDecimal minSlotWidth = slotWidthGran.multiply(BigDecimal.valueOf(minSlotNb))
                .stripTrailingZeros();

        if (minSlotNb == maxSlotNb) {
            return String.format("%sGHz", minSlotWidth.toPlainString());
        }

        return String.format("%sGHz to %sGHz", minSlotWidth.toPlainString(),
                this.slotWidthGranularity.multiply(BigDecimal.valueOf(maxSlotNb)).stripTrailingZeros().toPlainString());
    }
}
