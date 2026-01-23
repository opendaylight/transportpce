/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumKey;

/**
 * Default implementation of {@link TapiSpectrumCapabilityPacFactory}.
 *
 * <p>Builds a {@link SpectrumCapabilityPac} and always sets {@code supportableSpectrum}.
 * If neither used nor available spectrum is provided, a default {@code availableSpectrum}
 * range is generated based on {@link GridConstant}.
 */
public class DefaultTapiSpectrumCapabilityPacFactory implements TapiSpectrumCapabilityPacFactory {

    private final Factory frequencyFactory;
    private final TapiSpectrumGridConfig grid;

    public DefaultTapiSpectrumCapabilityPacFactory(Factory frequencyFactory, TapiSpectrumGridConfig grid) {
        this.frequencyFactory = Objects.requireNonNull(frequencyFactory, "frequencyFactory");
        this.grid = Objects.requireNonNull(grid, "grid");
    }

    public DefaultTapiSpectrumCapabilityPacFactory(Factory frequencyFactory) {
        this(frequencyFactory, new TapiSpectrumGridConfig(
                GridConstant.START_EDGE_FREQUENCY_THZ,
                GridConstant.GRANULARITY,
                GridConstant.EFFECTIVE_BITS));
    }

    /**
     * Supportable lower edge expressed on the same grid as {@link Frequency}.
     * Index 0 represents the start edge.
     */
    private Frequency supportableLowerFrequency() {
        return frequencyFactory.frequency(grid.startEdgeFrequencyThz(), grid.granularity(), 0);
    }

    /**
     * Supportable upper edge expressed on the same grid as {@link Frequency}.
     */
    private Frequency supportableUpperFrequency() {
        return frequencyFactory.frequency(grid.startEdgeFrequencyThz(), grid.granularity(), grid.effectiveBits());
    }

    @Override
    public SpectrumCapabilityPac create(
            Map<Frequency, Frequency> usedFreqMap,
            Map<Frequency, Frequency> availableFreqMap) {

        SpectrumCapabilityPacBuilder spectrumPac = new SpectrumCapabilityPacBuilder()
                .setAvailableSpectrum(new HashMap<>());

        // Always set supportable spectrum first (we'll use it for derived occupied as well)
        Frequency supportableLower = supportableLowerFrequency();
        Frequency supportableUpper = supportableUpperFrequency();

        SupportableSpectrum supportable = new SupportableSpectrumBuilder()
                .setLowerFrequency(supportableLower.hertz())
                .setUpperFrequency(supportableUpper.hertz())
                .build();

        spectrumPac.setSupportableSpectrum(Map.of(
                new SupportableSpectrumKey(
                        supportable.getLowerFrequency(),
                        supportable.getUpperFrequency()),
                supportable
        ));

        // If neither used nor available is present -> set default available spectrum, leave occupied UNSET
        if (isNullOrEmpty(usedFreqMap) && isNullOrEmpty(availableFreqMap)) {

            AvailableSpectrum defaultAvailable = new AvailableSpectrumBuilder()
                    .setLowerFrequency(supportableLower.hertz())
                    .setUpperFrequency(supportableUpper.hertz())
                    .build();

            spectrumPac.setAvailableSpectrum(Map.of(
                    new AvailableSpectrumKey(
                            defaultAvailable.getLowerFrequency(),
                            defaultAvailable.getUpperFrequency()),
                    defaultAvailable
            ));

            return spectrumPac.build();
        }

        // available
        if (!isNullOrEmpty(availableFreqMap)) {
            spectrumPac.setAvailableSpectrum(toAvailableSpectrumMap(availableFreqMap));
        }

        // occupied
        if (!isNullOrEmpty(usedFreqMap)) {
            spectrumPac.setOccupiedSpectrum(toOccupiedSpectrumMap(usedFreqMap));
        } else if (!isNullOrEmpty(availableFreqMap)) {
            Map<Frequency, Frequency> derivedOccupied =
                    complementWithinSupportable(availableFreqMap, supportableLower, supportableUpper);

            // Preserve "absent vs empty" semantics: only set if there is something to report.
            if (!derivedOccupied.isEmpty()) {
                spectrumPac.setOccupiedSpectrum(toOccupiedSpectrumMap(derivedOccupied));
            }
        }

        return spectrumPac.build();
    }

    @Override
    public SpectrumCapabilityPac create(
            OpenRoadmSpectrumRangeExtractor openRoadmSpectrumRangeExtractor,
            TerminationPoint terminationPoint) {

        Objects.requireNonNull(openRoadmSpectrumRangeExtractor, "openRoadmSpectrumRangeExtractor");
        Objects.requireNonNull(terminationPoint, "terminationPoint");

        SpectrumRanges ranges = openRoadmSpectrumRangeExtractor.extract(terminationPoint);
        return create(ranges.occupied(), ranges.available());
    }

    private static Map<AvailableSpectrumKey, AvailableSpectrum> toAvailableSpectrumMap(
            Map<Frequency, Frequency> availableFreqMap) {

        Map<AvailableSpectrumKey, AvailableSpectrum> aspecMap = new HashMap<>();
        for (Map.Entry<Frequency, Frequency> e : availableFreqMap.entrySet()) {
            AvailableSpectrum aspec = new AvailableSpectrumBuilder()
                    .setLowerFrequency(e.getKey().hertz())
                    .setUpperFrequency(e.getValue().hertz())
                    .build();

            aspecMap.put(new AvailableSpectrumKey(aspec.getLowerFrequency(), aspec.getUpperFrequency()), aspec);
        }
        return aspecMap;
    }

    private static Map<OccupiedSpectrumKey, OccupiedSpectrum> toOccupiedSpectrumMap(
            Map<Frequency, Frequency> usedFreqMap) {

        Map<OccupiedSpectrumKey, OccupiedSpectrum> ospecMap = new HashMap<>();
        for (Map.Entry<Frequency, Frequency> e : usedFreqMap.entrySet()) {
            OccupiedSpectrum ospec = new OccupiedSpectrumBuilder()
                    .setLowerFrequency(e.getKey().hertz())
                    .setUpperFrequency(e.getValue().hertz())
                    .build();

            ospecMap.put(new OccupiedSpectrumKey(ospec.getLowerFrequency(), ospec.getUpperFrequency()), ospec);
        }
        return ospecMap;
    }

    private Map<Frequency, Frequency> complementWithinSupportable(
            Map<Frequency, Frequency> available,
            Frequency supportableLower,
            Frequency supportableUpper) {

        // sort + clamp + merge available ranges
        var sorted = available.entrySet().stream()
                .map(e -> new Range(e.getKey(), e.getValue()))
                .sorted((a, b) -> a.lower().hertz().compareTo(b.lower().hertz()))
                .toList();

        var merged = new java.util.ArrayList<Range>();

        for (Range r : sorted) {
            Range clamped = r.clamp(supportableLower, supportableUpper);
            if (clamped == null) {
                continue; // fully outside or invalid after clamping
            }

            if (merged.isEmpty()) {
                merged.add(clamped);
                continue;
            }

            Range last = merged.get(merged.size() - 1);

            // overlap-or-touch: last.upper >= next.lower
            if (last.overlapsOrTouches(clamped)) {
                merged.set(merged.size() - 1, last.merge(clamped));
            } else {
                merged.add(clamped);
            }
        }

        // compute complement gaps
        Map<Frequency, Frequency> out = new HashMap<>();
        Frequency cursor = supportableLower;

        for (Range r : merged) {
            if (cursor.hertz().compareTo(r.lower().hertz()) < 0) {
                out.put(cursor, r.lower());
            }
            if (cursor.hertz().compareTo(r.upper().hertz()) < 0) {
                cursor = r.upper();
            }
        }

        if (cursor.hertz().compareTo(supportableUpper.hertz()) < 0) {
            out.put(cursor, supportableUpper);
        }

        return out;
    }

    private record Range(Frequency lower, Frequency upper) {

        Range {
            Objects.requireNonNull(lower, "lower");
            Objects.requireNonNull(upper, "upper");
            // optionally validate lower <= upper
        }

        Range clamp(Frequency start, Frequency end) {
            // if [lower, upper] outside [s, e], drop
            if (upper.hertz().compareTo(start.hertz()) <= 0) {
                return null;
            }
            if (lower.hertz().compareTo(end.hertz()) >= 0) {
                return null;
            }
            Frequency nl = (lower.hertz().compareTo(start.hertz()) < 0) ? start : lower;
            Frequency nu = (upper.hertz().compareTo(end.hertz()) > 0) ? end : upper;
            if (nu.hertz().compareTo(nl.hertz()) <= 0) {
                return null;
            }
            return new Range(nl, nu);
        }

        boolean overlapsOrTouches(Range other) {
            // treat touching as mergeable: last.upper >= other.lower
            return this.upper.hertz().compareTo(other.lower.hertz()) >= 0;
        }

        Range merge(Range other) {
            Frequency nl = this.lower.hertz().compareTo(other.lower.hertz()) <= 0 ? this.lower : other.lower;
            Frequency nu = this.upper.hertz().compareTo(other.upper.hertz()) >= 0 ? this.upper : other.upper;
            return new Range(nl, nu);
        }
    }

    private static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
