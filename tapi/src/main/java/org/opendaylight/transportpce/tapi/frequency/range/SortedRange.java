/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency.range;

import static com.google.common.collect.Range.closed;

import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * A {@link Range} implementation that stores frequency ranges in sorted,
 * normalized form.
 *
 * <p>Ranges added to this implementation may overlap or be adjacent.
 * Internally, all ranges are automatically merged so that the stored
 * representation consists of non-overlapping, connected ranges.
 *
 * <p>For example, adding the following ranges:
 * <pre>
 *   191.35 - 191.45
 *   191.45 - 191.55
 * </pre>
 * results in a single stored range:
 * <pre>
 *   191.35 - 191.55
 * </pre>
 *
 * <p>If a newly added range is fully enclosed by an existing range,
 * the operation is a no-op and {@code false} is returned.
 *
 * <p>All ranges are treated as closed intervals, i.e. both lower and
 * upper bounds are inclusive.
 */
public class SortedRange implements Range {

    private final RangeSet<Frequency> frequencyRanges = TreeRangeSet.create();

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

        if (frequencyRanges.encloses(closed(lowerBound, upperBound))) {
            // New range is fully contained in an existing range -> no change.
            return false;
        }

        frequencyRanges.add(closed(lowerBound, upperBound));

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

        frequencyRanges.asRanges().forEach(r -> range.put(r.lowerEndpoint(), r.upperEndpoint()));

        return range;
    }

    @Override
    public Map<Double, Double> asTeraHertz() {
        Map<Double, Double> ranges = new TreeMap<>();

        frequencyRanges.asRanges().forEach(r -> ranges.put(
                r.lowerEndpoint().teraHertz().doubleValue(),
                r.upperEndpoint().teraHertz().doubleValue())
        );

        return ranges;
    }

    @Override
    public Map<Uint64, Uint64> asHertz() {
        Map<Uint64, Uint64> ranges = new TreeMap<>();

        frequencyRanges.asRanges().forEach(r -> ranges.put(
                r.lowerEndpoint().hertz(),
                r.upperEndpoint().hertz())
        );

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
