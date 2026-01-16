/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
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
 * <p>Ranges are stored in a normalized form: any overlapping or adjacent ranges are merged.
 * For example, adding these will result in one single stored range:
 *    191.35 - 191.45
 *    191.45 - 191.55
 */
public class SortedRange implements Range {

    private final NavigableMap<Frequency, Frequency> frequencyRanges = new TreeMap<>();

    public SortedRange() {
    }

    public SortedRange(Map<Double, Double> ranges) {
        for (Map.Entry<Double, Double> range : ranges.entrySet()) {
            this.add(range.getKey(), range.getValue());
        }
    }

    @Override
    public boolean add(Frequency lowerBound, Frequency upperBound) {

        if (lowerBound.compareTo(upperBound) > 0) {
            throw new InvalidFrequencyRangeException(
                    String.format("Invalid frequency range: %s > %s", lowerBound, upperBound));
        }

        // If the exact same single range already exists, adding changes nothing.
        Frequency exact = frequencyRanges.get(lowerBound);
        if (exact != null && exact.compareTo(upperBound) == 0) {
            return false;
        }

        Map.Entry<Frequency, Frequency> covering = frequencyRanges.floorEntry(lowerBound);
        if (covering != null && covering.getValue().compareTo(upperBound) >= 0) {
            // New range is fully contained in an existing range -> no change.
            return false;
        }

        Frequency mergedLower = lowerBound;
        Frequency mergedUpper = upperBound;

        // Merge with the previous range if it overlaps or touches [mergedLower, mergedUpper].
        Map.Entry<Frequency, Frequency> floor = frequencyRanges.floorEntry(mergedLower);
        if (floor != null && floor.getValue().compareTo(mergedLower) >= 0) { // touches/overlaps
            mergedLower = floor.getKey();
            if (floor.getValue().compareTo(mergedUpper) > 0) {
                mergedUpper = floor.getValue();
            }
            frequencyRanges.remove(floor.getKey());
        }

        // Merge any following ranges that overlap or touch the merged interval.
        Map.Entry<Frequency, Frequency> next = frequencyRanges.ceilingEntry(mergedLower);
        while (next != null && next.getKey().compareTo(mergedUpper) <= 0) { // touches/overlaps
            if (next.getValue().compareTo(mergedUpper) > 0) {
                mergedUpper = next.getValue();
            }
            frequencyRanges.remove(next.getKey());
            next = frequencyRanges.ceilingEntry(mergedLower);
        }

        frequencyRanges.put(mergedLower, mergedUpper);

        return true;
    }

    @Override
    public boolean add(Double lowerBound, Double upperBound) {
        return this.add(new TeraHertz(lowerBound), new TeraHertz(upperBound));
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
    public boolean add(Double centerFrequencyTHz, Double widthGHz, Factory factory) {
        return add(factory.lower(centerFrequencyTHz, widthGHz), factory.upper(centerFrequencyTHz, widthGHz));
    }

    @Override
    public Map<Frequency, Frequency> ranges() {
        Map<Frequency, Frequency> range = new TreeMap<>();

        range.putAll(frequencyRanges);

        return range;
    }

    @Override
    public Map<Double, Double> asTeraHertz() {
        Map<Double, Double> ranges = new TreeMap<>();

        for (Map.Entry<Frequency, Frequency> range : frequencyRanges.entrySet()) {
            ranges.put(range.getKey().teraHertz().doubleValue(), range.getValue().teraHertz().doubleValue());
        }

        return ranges;
    }

    @Override
    public Map<Uint64, Uint64> asHertz() {
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
