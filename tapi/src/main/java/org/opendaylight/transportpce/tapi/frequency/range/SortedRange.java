/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * This implementation of Range interface is used to store frequency ranges in a sorted order.
 * This implementation does not allow overlapping ranges or a range where
 * the lower frequency is greater than the upper frequency.
 *
 * <p>
 * The frequency ranges are treated as exclusive, i.e. the lower and upper frequency is not included in the range.
 * These ranges are therefore treated as valid:
 *    191.35 - 191.45
 *    191.45 - 191.55
 * These ranges will not be added, given the existing range 191.35 - 191.45:
 *    191.35 - 191.45
 *    191.35 - 191.39
 */
public class SortedRange implements Range {

    private final SortedMap<Frequency, Frequency> frequencyRanges = new TreeMap<>();

    public SortedRange() {
    }

    public SortedRange(Map<Double, Double> ranges) {
        for (Map.Entry<Double, Double> range : ranges.entrySet()) {
            this.add(range.getKey(), range.getValue());
        }
    }

    @Override
    public boolean add(Frequency lowerExclusive, Frequency upperExclusive) {

        if (lowerExclusive.compareTo(upperExclusive) > 0) {
            throw new InvalidFrequencyRangeException(
                    String.format("Invalid frequency range: %s > %s", lowerExclusive, upperExclusive));
        }

        for (Map.Entry<Frequency, Frequency> range : frequencyRanges.entrySet()) {
            if (range.getKey().compareTo(upperExclusive) >= 0) {
                break;
            }
            if (range.getValue().compareTo(lowerExclusive) > 0) {
                return false;
            }
        }

        frequencyRanges.put(lowerExclusive, upperExclusive);

        return true;
    }

    @Override
    public boolean add(Double lower, Double upper) {
        return this.add(new TeraHertz(lower), new TeraHertz(upper));
    }

    @Override
    public boolean add(Range range) {
        boolean added = false;

        for (Map.Entry<Frequency, Frequency> entry : range.ranges().entrySet()) {
            added = this.add(entry.getKey(), entry.getValue()) || added;
        }

        return added;
    }

    @Override
    public boolean addCenterFrequency(Double centerFrequencyTHz, Double widthGHz, Factory factory) {
        return add(factory.lower(centerFrequencyTHz, widthGHz), factory.upper(centerFrequencyTHz, widthGHz));
    }

    @Override
    public Map<Frequency, Frequency> ranges() {
        Map<Frequency, Frequency> range = new TreeMap<>();

        range.putAll(frequencyRanges);

        return range;
    }

    @Override
    public Map<Double, Double> rangesAsDouble() {
        Map<Double, Double> ranges = new TreeMap<>();

        for (Map.Entry<Frequency, Frequency> range : frequencyRanges.entrySet()) {
            ranges.put(range.getKey().teraHertz().doubleValue(), range.getValue().teraHertz().doubleValue());
        }

        return ranges;
    }

    @Override
    public Map<Uint64, Uint64> rangesAsUint64() {
        Map<Uint64, Uint64> ranges = new TreeMap<>();

        for (Map.Entry<Frequency, Frequency> range : frequencyRanges.entrySet()) {
            ranges.put(range.getKey().hertz(), range.getValue().hertz());
        }

        return ranges;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof SortedRange that)) {
            return false;
        }

        return Objects.equals(frequencyRanges, that.frequencyRanges);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(frequencyRanges);
    }
}
