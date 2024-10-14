/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

import java.math.BigDecimal;

public class ValidInputFactory implements Factory {

    @Override
    public Valid instantiate(
            double lowerEdgeFrequencyTHz,
            double anchorFrequencyTHz,
            double centerFrequencyGranularityGHz,
            double slotGranularityGHz,
            int effectiveBits) {

        BigDecimal granularityGHz = BigDecimal.valueOf(centerFrequencyGranularityGHz);
        BigDecimal lowerEdgeFrequency = BigDecimal.valueOf(lowerEdgeFrequencyTHz);
        BigDecimal upperEdgeFrequency = lowerEdgeFrequency.add(
                granularityGHz.multiply(BigDecimal.valueOf(effectiveBits))
                .multiply(BigDecimal.valueOf(0.001)));

        return new ValidInput(
                new ValidSlot(
                        lowerEdgeFrequency,
                        BigDecimal.valueOf(anchorFrequencyTHz),
                        upperEdgeFrequency,
                        granularityGHz,
                        BigDecimal.valueOf(slotGranularityGHz)
                )
        );
    }
}
