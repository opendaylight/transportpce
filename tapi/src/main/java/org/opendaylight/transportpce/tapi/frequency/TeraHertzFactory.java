/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.math.BigDecimal;

public class TeraHertzFactory implements Factory {

    @Override
    public Frequency upper(Double centerFrequencyTHz, Double bandwidthGHz) {
        return new TeraHertz(BigDecimal.valueOf(centerFrequencyTHz)
                .add(BigDecimal.valueOf(bandwidthGHz).divide(BigDecimal.valueOf(1000)).divide(BigDecimal.TWO))
        );
    }

    @Override
    public Frequency lower(Double centerFrequencyTHz, Double bandwidthGHz) {
        return new TeraHertz(BigDecimal.valueOf(centerFrequencyTHz)
                .subtract(BigDecimal.valueOf(bandwidthGHz).divide(BigDecimal.valueOf(1000)).divide(BigDecimal.TWO))
        );
    }

    @Override
    public Frequency frequency(Double startFrequencyTHz, Double granularityGHz, int bitNumber) {
        return new TeraHertz(BigDecimal.valueOf(startFrequencyTHz)
                .add(
                        BigDecimal.valueOf(granularityGHz)
                                .multiply(BigDecimal.valueOf(0.001))
                                .multiply(BigDecimal.valueOf(bitNumber))
                )
        );
    }
}
