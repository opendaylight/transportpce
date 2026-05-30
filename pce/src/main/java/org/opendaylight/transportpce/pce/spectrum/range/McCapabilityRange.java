/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.range;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.BitSet;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;

/**
 * A {@link FrequencyRange} bounded by explicit minimum- and maximum-edge frequencies.
 *
 * <p>Typically constructed from an OpenROADM mc-capability-profile's
 * min-edge-freq and max-edge-freq. When projected onto a grid, frequencies
 * outside [minFrequency, maxFrequency] are excluded; the range is clamped
 * to the grid boundaries rather than extending beyond them.
 *
 * <p>Use {@link #from} to construct from OpenROADM {@link FrequencyTHz} values,
 * with null-handling for absent edge frequency attributes.
 */
public class McCapabilityRange implements FrequencyRange {

    private final BigDecimal minFrequency;

    private final BigDecimal maxFrequency;

    public McCapabilityRange(BigDecimal minFrequency, BigDecimal maxFrequency) {
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
    }

    @Override
    public BitSet gridRange(double slotWidthGranularityGHz, double edgeFrequencyTHz, int effectiveBits) {
        BigDecimal slotWidthTHz = BigDecimal.valueOf(slotWidthGranularityGHz).multiply(BigDecimal.valueOf(0.001));

        BigDecimal gridMinFrequencyTHz = BigDecimal.valueOf(edgeFrequencyTHz);
        BigDecimal gridMaxFrequencyTHz = gridMinFrequencyTHz.add(
                        BigDecimal.valueOf(effectiveBits)
                                .multiply(slotWidthTHz));

        BigDecimal minDiffTHz = minFrequency.max(gridMinFrequencyTHz).subtract(gridMinFrequencyTHz);
        BigDecimal maxDiffTHz = maxFrequency.min(gridMaxFrequencyTHz).subtract(gridMinFrequencyTHz);

        BigDecimal minIndex = minDiffTHz.divide(slotWidthTHz, 0, RoundingMode.UP);
        BigDecimal maxIndex = maxDiffTHz.divide(slotWidthTHz, 0, RoundingMode.DOWN);

        BitSet bitSet = new BitSet(effectiveBits);
        bitSet.set(minIndex.intValue(), maxIndex.intValue());

        return bitSet;
    }

    /**
     * Creates a {@link FrequencyRange} from OpenROADM mc-capability-profile edge frequencies.
     *
     * <p>If both arguments are null (no frequency constraint advertised), returns
     * an {@link EntireGridRange}. If only one is null, the missing bound defaults
     * to the corresponding grid edge (min → {@code edgeFrequencyTHz},
     * max → {@code edgeFrequencyTHz + effectiveBits * slotWidthGranularityGHz}).
     *
     * @param minFrequencyTHz lower edge frequency, or null to use the grid minimum.
     * @param maxFrequencyTHz upper edge frequency, or null to use the grid maximum.
     * @param slotWidthGranularityGHz frequency width of each slot.
     * @param edgeFrequencyTHz lowest frequency on the spectrum grid.
     * @param effectiveBits number of bits in the spectrum grid.
     * @return a {@link FrequencyRange} covering the supported frequency bounds.
     */
    public static FrequencyRange from(
            FrequencyTHz minFrequencyTHz,
            FrequencyTHz maxFrequencyTHz,
            double slotWidthGranularityGHz,
            double edgeFrequencyTHz,
            int effectiveBits) {

        if (minFrequencyTHz == null && maxFrequencyTHz == null) {
            return new EntireGridRange();
        }

        double minTHz = edgeFrequencyTHz;
        if (minFrequencyTHz != null) {
            minTHz = minFrequencyTHz.getValue().doubleValue();
        }
        BigDecimal min = BigDecimal.valueOf(minTHz);

        BigDecimal max;
        if (maxFrequencyTHz != null) {
            max = BigDecimal.valueOf(maxFrequencyTHz.getValue().doubleValue());
        } else {
            BigDecimal slotWidthTHz = BigDecimal.valueOf(slotWidthGranularityGHz).multiply(BigDecimal.valueOf(0.001));
            max = BigDecimal.valueOf(edgeFrequencyTHz).add(BigDecimal.valueOf(effectiveBits).multiply(slotWidthTHz));
        }

        if (min.compareTo(max) < 0) {
            return new McCapabilityRange(min, max);
        }

        return new McCapabilityRange(max, min);
    }
}
