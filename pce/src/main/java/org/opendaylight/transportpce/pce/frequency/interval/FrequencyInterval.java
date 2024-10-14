/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.math.BigDecimal;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.spectrum.allocation.FrequencyRange;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;


public class FrequencyInterval implements Interval {

    private final BigDecimal start;

    private final BigDecimal end;

    public FrequencyInterval(BigDecimal start, BigDecimal end) {
        this.start = start.stripTrailingZeros();
        this.end = end.stripTrailingZeros();
    }

    public FrequencyInterval(FrequencyTHz start, FrequencyTHz end) {
        this(
                BigDecimal.valueOf(start.getValue().doubleValue()),
                BigDecimal.valueOf(end.getValue().doubleValue())
        );
    }

    public FrequencyInterval(FrequencyRange frequency) {
        this(
                BigDecimal.valueOf(frequency.getMinFrequency().getValue().doubleValue()),
                BigDecimal.valueOf(frequency.getMaxFrequency().getValue().doubleValue())
        );
    }

    @Override
    public BigDecimal start() {
        return start;
    }

    @Override
    public BigDecimal end() {
        return end;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        FrequencyInterval that = (FrequencyInterval) object;
        return Objects.equals(start, that.start)
                && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return String.format("Frequency interval (THz): %s - %s", start, end);
    }
}
