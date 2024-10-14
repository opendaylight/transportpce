/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import java.math.BigDecimal;

public class ValidSlot implements Slot {

    private final BigDecimal lowerEdgeFrequency;

    private final BigDecimal anchorFrequencyTHz;

    private final BigDecimal upperEdgeFrequency;

    private final BigDecimal centerFrequencyGranularityGHz;

    private final BigDecimal slotWidthGranularityGHz;

    /**
     * A class suitable for validating frequency slot (i.e. center frequency with a frequency slot width).
     *
     * @param lowerEdgeFrequency The lowest frequency in the frequency grid.
     * @param anchorFrequencyTHz The frequency grid anchor frequency.
     * @param upperEdgeFrequency The highest frequency in the frequency grid.
     * @param centerFrequencyGranularityGHz Each center frequency is separated by this range.
     * @param slotWidthGranularityGHz Service input frequency range need to be dividable by this range.
     */
    public ValidSlot(BigDecimal lowerEdgeFrequency,
                     BigDecimal anchorFrequencyTHz,
                     BigDecimal upperEdgeFrequency,
                     BigDecimal centerFrequencyGranularityGHz,
                     BigDecimal slotWidthGranularityGHz) {

        this.lowerEdgeFrequency = lowerEdgeFrequency;
        this.anchorFrequencyTHz = anchorFrequencyTHz;
        this.upperEdgeFrequency = upperEdgeFrequency;
        this.centerFrequencyGranularityGHz = centerFrequencyGranularityGHz;
        this.slotWidthGranularityGHz = slotWidthGranularityGHz;
    }

    @Override
    public boolean isValidCenterFrequency(BigDecimal centerFrequencyTHz, Observer observer) {

        BigDecimal difference = centerFrequencyTHz.subtract(anchorFrequencyTHz);

        BigDecimal centerFrequencyGranularityTHz = centerFrequencyGranularityGHz
                .multiply(BigDecimal.valueOf(0.001));

        if (difference.remainder(centerFrequencyGranularityTHz).compareTo(BigDecimal.ZERO) != 0) {

            observer.error(String.format(
                    "Center frequency %s (THz) is not evenly dividable by %s (GHz)",
                    centerFrequencyTHz,
                    centerFrequencyGranularityTHz.multiply(BigDecimal.valueOf(1000))
            ));

            return false;
        }

        if (centerFrequencyTHz.compareTo(lowerEdgeFrequency) < 0
                || centerFrequencyTHz.compareTo(upperEdgeFrequency) > 0) {

            observer.error(String.format(
                    "Center frequency %s (THz) is outside the range %s - %s (THz)",
                    centerFrequencyTHz,
                    lowerEdgeFrequency,
                    upperEdgeFrequency
            ));

            return false;
        }

        return true;
    }


    @Override
    public boolean isValidSlotWidth(BigDecimal slotWidthGHz, Observer observer) {

        if (slotWidthGHz.remainder(slotWidthGranularityGHz).compareTo(BigDecimal.ZERO) != 0) {

            observer.error(String.format(
                    "Slot width %s (GHz) is not evenly dividable by %s (GHz)",
                    slotWidthGHz,
                    slotWidthGranularityGHz
            ));

            return false;
        }

        return true;
    }

    @Override
    public boolean isValidSlot(BigDecimal centerFrequencyTHz, BigDecimal slotWidthGHz, Observer observer) {

        if (!isValidCenterFrequency(centerFrequencyTHz, observer)) {
            return false;
        }

        if (!isValidSlotWidth(slotWidthGHz, observer)) {
            return false;
        }

        BigDecimal differenceTHz = slotWidthGHz.divide(BigDecimal.TWO).divide(BigDecimal.valueOf(1000));
        BigDecimal lower = centerFrequencyTHz.subtract(differenceTHz);
        BigDecimal upper = centerFrequencyTHz.add(differenceTHz);

        if (lower.compareTo(lowerEdgeFrequency) < 0) {
            observer.error(String.format(
                    "Center frequency %s (THz) with slot width %s (GHz) has a lower frequency outside the range %s-%s",
                    centerFrequencyTHz,
                    slotWidthGHz,
                    lowerEdgeFrequency,
                    upperEdgeFrequency)
            );
            return false;
        }

        if (upper.compareTo(upperEdgeFrequency) > 0) {
            observer.error(String.format(
                    "Center frequency %s (THz) with slot width %s (GHz) has a higher frequency outside the range %s-%s",
                    centerFrequencyTHz,
                    slotWidthGHz,
                    lowerEdgeFrequency,
                    upperEdgeFrequency)
            );

            return false;
        }

        return true;
    }
}
