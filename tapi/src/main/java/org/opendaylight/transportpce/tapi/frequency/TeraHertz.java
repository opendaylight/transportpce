/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.math.BigDecimal;
import java.util.Objects;
import org.opendaylight.yangtools.yang.common.Uint64;

public class TeraHertz implements Frequency {

    private final BigDecimal frequencyInTHz;

    /**
     * Create a new light frequency.
     *
     * @param frequencyInTHz e.g. 193.1 THz
     */
    public TeraHertz(BigDecimal frequencyInTHz) {
        this.frequencyInTHz = frequencyInTHz;
    }

    /**
     * Create a new light frequency.
     *
     * @param frequencyInTHz e.g. 193.1 THz
     */
    public TeraHertz(Double frequencyInTHz) {
        this.frequencyInTHz = BigDecimal.valueOf(frequencyInTHz);
    }

    @Override
    public BigDecimal teraHertz() {
        return BigDecimal.valueOf(frequencyInTHz.doubleValue());
    }

    @Override
    public Uint64 hertz() {
        return Uint64.valueOf(frequencyInTHz.multiply(BigDecimal.valueOf(1e12)).toBigInteger());
    }

    @Override
    public int compareTo(Frequency frequency) {
        return frequencyInTHz
                .stripTrailingZeros()
                .compareTo(
                        frequency.teraHertz().stripTrailingZeros()
                );
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Frequency light)) {
            return false;
        }
        return light.compareTo(this) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(frequencyInTHz.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return frequencyInTHz.stripTrailingZeros().toString();
    }
}
