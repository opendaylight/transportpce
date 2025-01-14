/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CenterFrequencyGranularityCollection implements Collection {

    private static final Logger LOG = LoggerFactory.getLogger(CenterFrequencyGranularityCollection.class);

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
                            .multiply(BigDecimal.valueOf(1000000000))
                            .stripTrailingZeros()
                            .toPlainString()));
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
        try {
            return new HashSet<>(centerFrequencyGranularitiesHz)
                    .stream().map(frequency -> frequency.divide(BigDecimal.valueOf(1000000000)))
                    .collect(Collectors.toSet());
        } catch (ArithmeticException e) {
            throw new LeastCommonMultipleException("Failed copying center frequency granularities to a new object", e);
        }
    }

    @Override
    public BigDecimal leastCommonMultipleInGHz() {
        try {
            return leastCommonMultipleInGHz(centerFrequencyGranularitiesHz);
        } catch (ArithmeticException e) {
            throw new LeastCommonMultipleException("No least common multiple found", e);
        }
    }

    private BigDecimal leastCommonMultipleInGHz(Set<BigDecimal> centerFreqGranularitiesHz) {
        if (centerFreqGranularitiesHz.isEmpty()) {
            return new BigDecimal(BigDecimal.valueOf(defaultFrequencyGranularity).stripTrailingZeros().toPlainString());
        }

        Iterator<BigDecimal> iterator = centerFreqGranularitiesHz.iterator();
        if (centerFreqGranularitiesHz.size() == 1) {
            return iterator.next().divide(BigDecimal.valueOf(1000000000));
        }

        BigInteger lcm = new BigInteger(String.valueOf(iterator.next()));

        while (iterator.hasNext()) {
            lcm = this.leastCommonMultiple(lcm, new BigInteger(String.valueOf(iterator.next())));
        }

        BigDecimal lcmTHz = new BigDecimal(lcm.toString()).divide(BigDecimal.valueOf(1000000000));
        LOG.info("Least common multiple for center frequency granularities: {}GHz", lcmTHz);
        return lcmTHz;
    }

    private BigInteger leastCommonMultiple(BigInteger numberOne, BigInteger numberTwo) {
        BigInteger greatestCommonDivisor = numberOne.gcd(numberTwo);
        BigInteger absProduct = numberOne.multiply(numberTwo).abs();
        return absProduct.divide(greatestCommonDivisor);
    }

    @Override
    public int slots(BigDecimal frequencyGranularityGHz) {
        //A temporary set ensuring we're not messing with the internal state of this object.
        Set<BigDecimal> temp = new LinkedHashSet<>(this.centerFrequencyGranularitiesHz);

        if (this.centerFrequencyGranularitiesHz.isEmpty()) {
            temp.add(new BigDecimal(BigDecimal.valueOf(defaultFrequencyGranularity)
                    .multiply(BigDecimal.valueOf(1000000000)).stripTrailingZeros().toPlainString()));
        }

        // By adding frequencyGranularityGHz, we'll add support for center frequency granularities
        // that doesn't fit the formula frequencyGranularityGHz x m (e.g. 6.25 x m).
        temp.add(new BigDecimal(frequencyGranularityGHz
                        .multiply(BigDecimal.valueOf(1000000000)).stripTrailingZeros().toPlainString())
        );

        return leastCommonMultipleInGHz(temp).divide(frequencyGranularityGHz).intValue();
    }

    @Override
    public int slots(double frequencyGranularityGHz) {
        int slots = slots(new BigDecimal(frequencyGranularityGHz));
        LOG.info("Center frequency granularity slot width: {} (frequency granularity: {}GHz)",
                slots, frequencyGranularityGHz);
        return slots;
    }
}
