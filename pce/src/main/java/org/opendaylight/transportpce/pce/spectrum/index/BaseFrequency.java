/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.index;

import java.math.BigDecimal;

public class BaseFrequency implements Base {

    @Override
    public int referenceFrequencySpectrumIndex(
        double referenceFrequencyTHz, double edgeFrequencyTHz, double frequencyGranularityGHz) {

        BigDecimal referenceFrequency = BigDecimal.valueOf(referenceFrequencyTHz);
        BigDecimal edgeFrequency = BigDecimal.valueOf(edgeFrequencyTHz);
        BigDecimal granularity = (BigDecimal.valueOf(frequencyGranularityGHz)).multiply(BigDecimal.valueOf(0.001));

        if (referenceFrequency.compareTo(edgeFrequency) < 0) {
            throw new NoIndexFoundException(
                String.format(
                    "Cannot find an index for %s (THz) since it's lower than the edge frequency %s (THz).",
                    referenceFrequencyTHz,
                    edgeFrequencyTHz
                )
            );
        }

        if (referenceFrequency.subtract(edgeFrequency).remainder(granularity).compareTo(BigDecimal.ZERO) != 0) {
            throw new NoIndexFoundException(
                String.format(
                    "Cannot find an index for %s (THz) since it's not on a grid"
                        + " separated by %s (GHz) starting with the edge frequency %s (THz).",
                    referenceFrequencyTHz,
                    frequencyGranularityGHz,
                    edgeFrequencyTHz
                )
            );
        }

        return referenceFrequency.subtract(edgeFrequency).divide(granularity).intValue();
    }
}
