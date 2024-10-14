/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.spectrum.index;

import java.math.BigDecimal;
import org.opendaylight.yangtools.yang.common.Decimal64;

/**
 * This class is heavily dependent on global state.
 *
 * <p>By isolating it and hiding its methods behind an
 * interface, dependent classes may mock the response.
 * Therefore, any logic in dependent classes may be unit
 * tested without fear of global variables etc. "getting
 * in the way".
 */
public class SpectrumIndex implements Index {

    private final Double lowerEdgeFrequency;

    private final Double frequencyWidthGranularityGhz;

    private final int nrOfEffectiveBits;

    public SpectrumIndex(Double lowerEdgeFrequency, Double frequencyWidthGranularityGhz, int effectiveBits) {
        this.lowerEdgeFrequency = lowerEdgeFrequency;
        this.frequencyWidthGranularityGhz = frequencyWidthGranularityGhz;
        this.nrOfEffectiveBits = effectiveBits;
    }

    @Override
    public int index(Decimal64 frequency) {
        return index(lowerEdgeFrequency, frequencyWidthGranularityGhz, nrOfEffectiveBits, frequency);
    }

    public int index(Double startFrequency, Double widthGHz, int effectiveBits, Decimal64 frequency) {

        if (startFrequency == null || widthGHz == null || effectiveBits == 0 || frequency == null) {
            throw new IllegalArgumentException("Null argument");
        }

        BigDecimal frequencyBigDecimal = BigDecimal.valueOf(frequency.doubleValue());
        BigDecimal edgeFrequency = BigDecimal.valueOf(startFrequency);

        if (frequencyBigDecimal.compareTo(edgeFrequency) < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Frequency out of bounds (expected no lower than %s), found: %s",
                            edgeFrequency.doubleValue(),
                            frequency
                    )
            );
        }

        BigDecimal endFrequency = edgeFrequency
                .add(BigDecimal.valueOf(widthGHz)
                        .multiply(BigDecimal.valueOf(0.001)
                        .multiply(BigDecimal.valueOf(effectiveBits)))
                );

        if (frequencyBigDecimal.compareTo(endFrequency) > 0) {
            throw new IllegalArgumentException(
                    String.format("Frequency %s greater than maximum %s ", frequency, endFrequency)
            );
        }

        int index = (frequencyBigDecimal
                .subtract(edgeFrequency)
        ).multiply(
                BigDecimal.valueOf(1000)
                        .divide(BigDecimal.valueOf(widthGHz))
        ).intValue();

        if (index < 0 || index > effectiveBits) {
            throw new IllegalArgumentException("Frequency not in range " + frequency);
        }

        return index;
    }
}
