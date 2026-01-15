/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.grid.Available;
import org.opendaylight.transportpce.tapi.frequency.grid.AvailableGrid;
import org.opendaylight.transportpce.tapi.frequency.grid.Numeric;
import org.opendaylight.transportpce.tapi.frequency.range.Range;
import org.opendaylight.transportpce.tapi.frequency.range.RangeFactory;
import org.opendaylight.transportpce.tapi.frequency.range.SortedRange;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev250110.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev250110.degree.used.wavelengths.UsedWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Default implementation of {@link OpenRoadmSpectrumRangeExtractor}.
 *
 * <p>Extracts spectrum ranges from OpenROADM termination points and converts them into
 * numeric frequency ranges expressed as either:
 * <ul>
 *   <li>{@code Map<Frequency, Frequency>} (lowerExclusive -&gt; upperExclusive), or</li>
 *   <li>{@link Range} instances, depending on caller needs.</li>
 * </ul>
 *
 * <p>OpenROADM represents spectrum availability/occupation in different ways depending on the
 * termination point type, e.g. via frequency bitmaps (AvailFreqMaps) or via used-wavelength lists.
 * This component encapsulates those details and provides uniform range outputs for the caller.
 *
 * <p>When required data is absent (missing augmentations/attributes/maps), methods return empty
 * ranges/maps rather than throwing.
 */
public class DefaultOpenRoadmSpectrumRangeExtractor implements OpenRoadmSpectrumRangeExtractor {

    private final Numeric numericFrequency;

    private final Factory frequencyFactory;

    private final RangeFactory rangeFactory;

    /**
     * Creates a spectrum range extractor.
     *
     * @param numericFrequency
     *     decoder for OpenROADM frequency bitmaps (e.g. available/assigned slot maps)
     * @param frequencyFactory
     *     factory used when converting center-frequency/width pairs into lower/upper frequency bounds
     * @param rangeFactory
     *     helper for building {@link Range} instances from center-frequency/width pairs
     * @throws NullPointerException if any argument is {@code null}
     */
    public DefaultOpenRoadmSpectrumRangeExtractor(
            Numeric numericFrequency,
            Factory frequencyFactory,
            RangeFactory rangeFactory) {

        this.numericFrequency = Objects.requireNonNull(numericFrequency);
        this.frequencyFactory = Objects.requireNonNull(frequencyFactory);
        this.rangeFactory = Objects.requireNonNull(rangeFactory);
    }

    @Override
    public Map<Frequency, Frequency> getPP11UsedFrequencies(TerminationPoint1 tp) {
        AvailFreqMapsKey cband = new AvailFreqMapsKey(GridConstant.C_BAND);

        Map<Double, Double> usedRanges = usedRanges(getPpAvailableFreqMaps(tp, cband));
        return new SortedRange(usedRanges).ranges();
    }

    /** {@inheritDoc} */
    @Override
    public Map<Frequency, Frequency> getPP11AvailableFrequencies(TerminationPoint1 tp) {
        AvailFreqMapsKey cband = new AvailFreqMapsKey(GridConstant.C_BAND);

        Map<Double, Double> availableRanges = availableRanges(getPpAvailableFreqMaps(tp, cband));
        return new SortedRange(availableRanges).ranges();
    }

    /** {@inheritDoc} */
    @Override
    public Range getTTP11UsedFreqMap(TerminationPoint1 tp) {
        AvailFreqMapsKey cband = new AvailFreqMapsKey(GridConstant.C_BAND);

        Map<Double, Double> usedRanges = usedRanges(getTxTtpAvailableFreqMaps(tp, cband));
        return new SortedRange(usedRanges);
    }

    /** {@inheritDoc} */
    @Override
    public Range getTTP11AvailableFreqMap(TerminationPoint1 tp) {
        AvailFreqMapsKey cband = new AvailFreqMapsKey(GridConstant.C_BAND);

        Map<Double, Double> availableRanges = availableRanges(getTxTtpAvailableFreqMaps(tp, cband));
        return new SortedRange(availableRanges);
    }

    /** {@inheritDoc} */
    @Override
    public Range getTTPUsedFreqMap(TerminationPoint tp) {
        var termPoint1 = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
                .TerminationPoint1.class);

        if (!hasTxTtpUsedWavelengths(termPoint1)) {
            AvailFreqMapsKey cband = new AvailFreqMapsKey(GridConstant.C_BAND);
            AvailFreqMaps availFreqMaps = getTxTtpAvailableFreqMaps(termPoint1, cband);
            Map<Double, Double> usedRanges = usedRanges(availFreqMaps);
            return new SortedRange(usedRanges);
        }
        Range range = new SortedRange();
        Map<UsedWavelengthsKey, UsedWavelengths> waveLengths = getTxTtpUsedWavelengths(termPoint1);
        for (Map.Entry<UsedWavelengthsKey, UsedWavelengths> usedLambdas : waveLengths.entrySet()) {
            Double centFreq = usedLambdas.getValue().getFrequency().getValue().doubleValue();
            Double width = usedLambdas.getValue().getWidth().getValue().doubleValue();
            range.add(centFreq, width, frequencyFactory);
        }
        return range;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Frequency, Frequency> getXpdrUsedWavelength(TerminationPoint tp) {
        var tpAug = tp.augmentation(TerminationPoint1.class);
        if (tpAug == null) {
            return new HashMap<>();
        }
        XpdrNetworkAttributes xnatt = tpAug.getXpdrNetworkAttributes();
        if (xnatt == null) {
            return new HashMap<>();
        }
        var xnattWvlgth = xnatt.getWavelength();
        if (xnattWvlgth == null) {
            return new HashMap<>();
        }
        var freq = xnattWvlgth.getFrequency();
        if (freq == null) {
            return new HashMap<>();
        }
        var width = xnattWvlgth.getWidth();
        if (width == null) {
            return new HashMap<>();
        }
        Double centerFrequencyTHz = freq.getValue().doubleValue();
        Double widthGHz = width.getValue().doubleValue();
        return rangeFactory.range(centerFrequencyTHz, widthGHz).ranges();
    }

    /**
     * Decodes an OpenROADM frequency bitmap into numeric frequency ranges.
     *
     * <p>The bitmap is copied to a fixed length of {@link GridConstant#NB_OCTECTS} and decoded using the supplied
     * {@code frequencyRange} function (e.g. available vs assigned).
     */
    private Map<Double, Double> ranges(AvailFreqMaps afm, Function<Available, Map<Double, Double>> frequencyRange) {
        byte[] freqByteSet = Arrays.copyOf(afm.getFreqMap(), GridConstant.NB_OCTECTS);
        return frequencyRange.apply(new AvailableGrid(freqByteSet));
    }

    /**
     * Returns numeric ranges representing AVAILABLE spectrum decoded from a frequency bitmap.
     *
     * @param avlFreqMaps OpenROADM available frequency map container
     * @return available spectrum ranges (lower -&gt; upper), never {@code null}
     */
    private Map<Double, Double> availableRanges(AvailFreqMaps avlFreqMaps) {
        return ranges(avlFreqMaps, numericFrequency::availableFrequency);
    }

    /**
     * Returns numeric ranges representing ASSIGNED/OCCUPIED spectrum decoded from a frequency bitmap.
     *
     * @param avlFreqMaps OpenROADM available frequency map container
     * @return occupied spectrum ranges (lower -&gt; upper), never {@code null}
     */
    private Map<Double, Double> usedRanges(AvailFreqMaps avlFreqMaps) {
        return ranges(avlFreqMaps, numericFrequency::assignedFrequency);
    }

    /**
     * Checks whether the provided termination point contains a non-null and non-empty frequency map
     * for the supplied bandKey argument in its PpAttributes object.
     *
     * @param tp a termination point
     * @param bandKey the key representing the band to validate
     * @return {@code true} if it has, {@code false} otherwise
     */
    private boolean hasPpBand(TerminationPoint1 tp, AvailFreqMapsKey bandKey) {
        return hasPpAttributes(tp)
                && tp.getPpAttributes().getAvailFreqMaps() != null
                && tp.getPpAttributes().getAvailFreqMaps().containsKey(bandKey)
                && tp.getPpAttributes().getAvailFreqMaps().get(bandKey) != null
                && tp.getPpAttributes().getAvailFreqMaps().get(bandKey).getFreqMap() != null
                && tp.getPpAttributes().getAvailFreqMaps().get(bandKey).getFreqMap().length > 0;
    }

    /**
     * Checks if the supplied termination point has a non-null PpAttributes object.
     *
     * @param tp a TerminationPoint1 object
     * @return {@code true} if the supplied TerminationPoint1 object contains a non-null PpAttributes object
     */
    private boolean hasPpAttributes(TerminationPoint1 tp) {
        return tp != null && tp.getPpAttributes() != null;
    }

    /**
     * Retrieves the {@link AvailFreqMaps} entry for the given band from PP attributes.
     *
     * @param tp supplied termination point augmentation
     * @param freqMapsKey band key to look up (e.g. C-band)
     * @return the {@link AvailFreqMaps} entry, or an empty map container if not present
     */
    private AvailFreqMaps getPpAvailableFreqMaps(TerminationPoint1 tp, AvailFreqMapsKey freqMapsKey) {
        if (hasPpBand(tp, freqMapsKey)) {
            return tp.getPpAttributes().getAvailFreqMaps().get(freqMapsKey);
        }
        else {
            return emptyFreqMap();
        }
    }

    /**
     * Checks if the provided termination point contains a non-null and non-empty frequency map
     * for the supplied bandKey argument in its TxTtpAttributes object.
     *
     * @param tp a termination point
     * @param bandKey the key representing the band to validate
     * @return {@code true}
     */
    private boolean hasTxTtpBand(TerminationPoint1 tp, AvailFreqMapsKey bandKey) {
        return hasTxTtpAttributes(tp)
                && tp.getTxTtpAttributes().getAvailFreqMaps() != null
                && tp.getTxTtpAttributes().getAvailFreqMaps().containsKey(bandKey)
                && tp.getTxTtpAttributes().getAvailFreqMaps().get(bandKey) != null
                && tp.getTxTtpAttributes().getAvailFreqMaps().get(bandKey).getFreqMap() != null
                && tp.getTxTtpAttributes().getAvailFreqMaps().get(bandKey).getFreqMap().length > 0;
    }

    /**
     * Checks whether TxTtp attributes contain a non-empty {@code usedWavelengths} list.
     *
     * @param tp termination point augmentation
     * @return {@code true} if {@code usedWavelengths} is present and non-empty
     */
    private boolean hasTxTtpUsedWavelengths(TerminationPoint1 tp) {
        return hasTxTtpAttributes(tp)
                && tp.getTxTtpAttributes().getUsedWavelengths() != null
                && !tp.getTxTtpAttributes().getUsedWavelengths().isEmpty();
    }

    /**
     * Fetches the used wave lengths map from the TxTtpAttributes of the supplied termination point.
     *
     * @param tp the supplied TerminationPoint1
     * @return {@code Map<UsedWavelengthsKey, UsedWavelengths>} for the given object,
     *     or an empty map if the object did not contain usedWaveLengths.
     */
    @NonNull
    private Map<UsedWavelengthsKey, UsedWavelengths> getTxTtpUsedWavelengths(TerminationPoint1 tp) {
        if (hasTxTtpUsedWavelengths(tp)) {
            return tp.getTxTtpAttributes().getUsedWavelengths();
        }
        else {
            return Map.of();
        }
    }


    /**
     * Checks if the supplied termination point contains a non-null TxTtpAttributes object.
     *
     * @param tp the supplied TerminationPoint1 object
     * @return {@code true} if it did, {@code false} if it did not.
     */
    private boolean hasTxTtpAttributes(TerminationPoint1 tp) {
        return tp != null && tp.getTxTtpAttributes() != null;
    }

    /**
     * Fetches the AvailFreqMaps object from the supplied termination points TxTtpAttributes object.
     *
     * @param tp the supplied TerminationPoint1 object
     * @param freqMapsKey the key for the frequency map to fetch.
     * @return {@code AvailFreqMaps} fetched from the TxTtpAttributes object, or an empty object if it didn't exist.
     */
    private AvailFreqMaps getTxTtpAvailableFreqMaps(TerminationPoint1 tp, AvailFreqMapsKey freqMapsKey) {
        if (hasTxTtpBand(tp, freqMapsKey)) {
            return tp.getTxTtpAttributes().getAvailFreqMaps().get(freqMapsKey);
        }
        else {
            return emptyFreqMap();
        }
    }

    /**
     * Creates an empty {@link AvailFreqMaps} instance.
     *
     * <p>The returned object contains an empty {@code freqMap} byte array and a fixed map name.
     * Intended for simplifying callers that unconditionally decode a bitmap.
     *
     * @return an empty {@link AvailFreqMaps}
     */
    @VisibleForTesting
    public AvailFreqMaps emptyFreqMap() {
        return new AvailFreqMapsBuilder()
                .setFreqMap(new byte[0])
                .setMapName("emptymap")
                .build();
    }
}
