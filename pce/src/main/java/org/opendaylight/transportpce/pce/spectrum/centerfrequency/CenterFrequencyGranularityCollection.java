/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.centerfrequency;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CenterFrequencyGranularityCollection implements Collection {

    private final Set<BigDecimal> centerFrequencyGranularitiesHz;

    private final double defaultFrequencyGranularity;

    public CenterFrequencyGranularityCollection(double defaultFrequencyGranularity) {
        this.defaultFrequencyGranularity = defaultFrequencyGranularity;
        centerFrequencyGranularitiesHz = new LinkedHashSet<>();
    }

    @Override
    public boolean add(BigDecimal centerFrequencyGranularityGHz) {
        if (centerFrequencyGranularityGHz != null) {
            return this.centerFrequencyGranularitiesHz.add(
                new BigDecimal(centerFrequencyGranularityGHz
                    .multiply(BigDecimal.valueOf(1000000000)).stripTrailingZeros().toPlainString()
                )
            );
        }
        return false;
    }

    @Override
    public boolean add(float centerFrequencyGranularityGHz) {
        return add(new BigDecimal(centerFrequencyGranularityGHz));
    }

    @Override
    public boolean add(double centerFrequencyGranularityGHz) {
        return add(new BigDecimal(centerFrequencyGranularityGHz));
    }

    @Override
    public Set<BigDecimal> set() {
        return new HashSet<>(centerFrequencyGranularitiesHz)
            .stream().map(frequency -> frequency.divide(BigDecimal.valueOf(1000000000)))
            .collect(Collectors.toSet());
    }

    @Override
    public BigDecimal leastCommonMultiplierInGHz() {

        if (centerFrequencyGranularitiesHz.isEmpty()) {
            return new BigDecimal(BigDecimal.valueOf(defaultFrequencyGranularity).stripTrailingZeros().toPlainString());
        }

        Iterator<BigDecimal> iterator = centerFrequencyGranularitiesHz.iterator();
        if (centerFrequencyGranularitiesHz.size() == 1) {
            return iterator.next().divide(BigDecimal.valueOf(1000000000));
        }

        BigInteger lcm = new BigInteger(String.valueOf(iterator.next()));

        while (iterator.hasNext()) {
            lcm = this.leastCommonMultiplier(lcm, new BigInteger(String.valueOf(iterator.next())));
        }

        return new BigDecimal(lcm.toString()).divide(BigDecimal.valueOf(1000000000));
    }

    private BigInteger leastCommonMultiplier(BigInteger number1, BigInteger number2) {
        BigInteger gcd = number1.gcd(number2);
        BigInteger absProduct = number1.multiply(number2).abs();
        return absProduct.divide(gcd);
    }

    @Override
    public int slots(BigDecimal frequencyGranularityGHz) {

        BigDecimal lcm = leastCommonMultiplierInGHz();

        if (lcm.remainder(frequencyGranularityGHz).compareTo(BigDecimal.ZERO) != 0) {
            throw new LeastCommonMultiplierException(
                String.format(
                    "The LCM center frequency %s is not evenly dividable by frequency granularity %s",
                    lcm,
                    frequencyGranularityGHz
                )
            );
        }

        return lcm.divide(frequencyGranularityGHz).intValue();

    }

    @Override
    public int slots(double frequencyGranularityGHz) {
        return slots(new BigDecimal(frequencyGranularityGHz));
    }
}
