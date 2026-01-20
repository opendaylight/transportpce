/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.TeraHertzFactory;
import org.opendaylight.transportpce.tapi.frequency.grid.Available;
import org.opendaylight.transportpce.tapi.frequency.grid.AvailableGrid;
import org.opendaylight.transportpce.tapi.frequency.grid.FrequencyMath;
import org.opendaylight.transportpce.tapi.frequency.grid.Numeric;
import org.opendaylight.transportpce.tapi.frequency.grid.NumericFrequency;
import org.opendaylight.transportpce.tapi.frequency.range.FrequencyRangeFactory;
import org.opendaylight.transportpce.tapi.frequency.range.Range;
import org.opendaylight.transportpce.tapi.frequency.range.RangeFactory;
import org.opendaylight.transportpce.tapi.frequency.range.SortedRange;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev250110.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev250110.degree.used.wavelengths.UsedWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Default implementation of {@link OpenRoadmSpectrumRangeExtractor}.
 *
 * <p>This extractor decodes OpenROADM spectrum information from termination points and returns
 * normalized frequency intervals. Depending on TP type, spectrum is derived from:
 * <ul>
 *   <li>frequency bitmaps (AvailFreqMaps), decoded via {@link Numeric}</li>
 *   <li>used-wavelength lists (center frequency + width)</li>
 *   <li>XPDR wavelength attributes (center frequency + width)</li>
 * </ul>
 *
 * <p>Returned intervals are normalized (e.g. overlapping/adjacent segments may be merged by
 * the {@link Range} implementation in use).
 *
 * <p>If required data is absent (missing augmentations/attributes/maps), this implementation
 * returns empty results rather than throwing.
 */
public class DefaultOpenRoadmSpectrumRangeExtractor implements OpenRoadmSpectrumRangeExtractor {

    private final Numeric numericFrequency;

    private final Factory frequencyFactory;

    private final RangeFactory rangeFactory;

    private static final AvailFreqMapsKey C_BAND_KEY = new AvailFreqMapsKey(GridConstant.C_BAND);

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

    /** {@inheritDoc} */
    @Override
    public SpectrumRanges extract(TerminationPointId tpId, TerminationPoint1 tp) {
        if (tpId == null || tp == null) {
            return SpectrumRanges.empty();
        }

        return extractRoadm(tpId.openRoadmTpType(), tp);
    }

    /** {@inheritDoc} */
    @Override
    public SpectrumRanges extract(TerminationPoint tp) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1 commonTp1 =
                tp.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1.class);

        if (commonTp1 == null || commonTp1.getTpType() == null) {
            return SpectrumRanges.empty();
        }

        return switch (commonTp1.getTpType()) {
            case SRGRXPP, SRGTXPP, SRGTXRXPP -> {
                TerminationPoint1 topologyTp1 = tp.augmentation(TerminationPoint1.class);
                yield topologyTp1 == null
                        ? SpectrumRanges.empty()
                        : fromRanges(ppOccupied(topologyTp1), ppAvailable(topologyTp1));
            }
            case DEGREERXTTP, DEGREETXTTP, DEGREETXRXTTP -> {
                TerminationPoint1 topologyTp1 = tp.augmentation(TerminationPoint1.class);
                yield topologyTp1 == null
                        ? SpectrumRanges.empty()
                        : fromRanges(
                        ttpOccupiedFromUsedWavelengthsOrBitmap(topologyTp1),
                        ttpAvailable(topologyTp1));
            }
            case XPONDERCLIENT, XPONDERNETWORK, XPONDERPORT ->
                xpdrRanges(tp);

            default -> SpectrumRanges.empty();
        };
    }

    /** {@inheritDoc} */
    @Override
    public SpectrumRanges extractRoadm(OpenroadmTpType openroadmTpType, TerminationPoint1 tp) {
        if (openroadmTpType == null || tp == null) {
            return SpectrumRanges.empty();
        }

        return switch (openroadmTpType) {
            case SRGRXPP, SRGTXPP, SRGTXRXPP ->
                fromRanges(ppOccupied(tp), ppAvailable(tp));
            case DEGREERXTTP, DEGREETXTTP, DEGREETXRXTTP ->
                fromRanges(ttpOccupiedFromUsedWavelengthsOrBitmap(tp), ttpAvailable(tp));
            default -> SpectrumRanges.empty();
        };
    }

    /**
     * Decodes an OpenROADM frequency bitmap into numeric frequency intervals.
     *
     * <p>The bitmap is copied/padded to {@link GridConstant#NB_OCTECTS} before decoding.
     * The provided {@code frequencyRange} function selects which interpretation to apply
     * (e.g. available vs assigned).
     *
     * @param afm OpenROADM bitmap container
     * @param frequencyRange decoding function (e.g. {@link Numeric#availableFrequency} or
     *        {@link Numeric#assignedFrequency})
     * @return decoded numeric intervals as {@code lowerBound -> upperBound}; never {@code null}
     */
    private Map<Double, Double> ranges(AvailFreqMaps afm, Function<Available, Map<Double, Double>> frequencyRange) {
        byte[] freqByteSet = Arrays.copyOf(afm.getFreqMap(), GridConstant.NB_OCTECTS);
        return frequencyRange.apply(new AvailableGrid(freqByteSet));
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
    private static AvailFreqMaps emptyFreqMap() {
        return new AvailFreqMapsBuilder()
                .setFreqMap(new byte[0])
                .setMapName("emptymap")
                .build();
    }

    /**
     * Computes occupied DEG-TTP spectrum.
     *
     * <p>If TxTtp {@code usedWavelengths} is present and non-empty, each entry (center + width)
     * is converted into an interval and added to the occupied set. Otherwise, occupied spectrum
     * is derived from the TxTtp bitmap (assigned frequency decoding).
     */
    private Range ttpOccupiedFromUsedWavelengthsOrBitmap(TerminationPoint1 tp) {
        if (!hasTxTtpUsedWavelengths(tp)) {
            return ttpOccupied(tp);
        }

        Range occupied = rangeFactory.empty();
        for (UsedWavelengths wl : getTxTtpUsedWavelengths(tp).values()) {
            Double centFreq = wl.getFrequency().getValue().doubleValue();
            Double width = wl.getWidth().getValue().doubleValue();
            occupied.add(centFreq, width, frequencyFactory);
        }
        return occupied;
    }

    /**
     * Computes XPDR spectrum ranges from the XPDR wavelength attributes.
     *
     * <p>XPDRs provide an occupied wavelength (center + width). They typically do not expose an
     * "available spectrum" bitmap in the same way, so available is returned empty.
     */
    private SpectrumRanges xpdrRanges(TerminationPoint tp) {
        var tpAug = tp.augmentation(TerminationPoint1.class);
        if (tpAug == null) {
            return SpectrumRanges.empty();
        }
        XpdrNetworkAttributes xnatt = tpAug.getXpdrNetworkAttributes();
        if (xnatt == null || xnatt.getWavelength() == null
                || xnatt.getWavelength().getFrequency() == null
                || xnatt.getWavelength().getWidth() == null) {
            return SpectrumRanges.empty();
        }

        Double centerFrequencyTHz = xnatt.getWavelength().getFrequency().getValue().doubleValue();
        Double widthGHz = xnatt.getWavelength().getWidth().getValue().doubleValue();

        Range occupied = rangeFactory.range(centerFrequencyTHz, widthGHz);

        return fromRanges(occupied, rangeFactory.empty());
    }

    private static SpectrumRanges fromRanges(Range occupied, Range available) {
        return new SpectrumRanges(occupied.ranges(), available.ranges());
    }

    private Range decodeBitmap(
            TerminationPoint1 tp,
            Function<TerminationPoint1, AvailFreqMaps> selectMap,
            Function<Available, Map<Double, Double>> decode) {

        AvailFreqMaps afm = selectMap.apply(tp);
        Map<Double, Double> decoded = ranges(afm, decode);
        return new SortedRange(decoded);
    }

    private Range ppAvailable(TerminationPoint1 tp) {
        return decodeBitmap(tp,
                t -> getPpAvailableFreqMaps(t, C_BAND_KEY),
                numericFrequency::availableFrequency);
    }

    private Range ppOccupied(TerminationPoint1 tp) {
        return decodeBitmap(tp,
                t -> getPpAvailableFreqMaps(t, C_BAND_KEY),
                numericFrequency::assignedFrequency);
    }

    private Range ttpAvailable(TerminationPoint1 tp) {
        return decodeBitmap(tp,
                t -> getTxTtpAvailableFreqMaps(t, C_BAND_KEY),
                numericFrequency::availableFrequency);
    }

    private Range ttpOccupied(TerminationPoint1 tp) {
        return decodeBitmap(tp,
                t -> getTxTtpAvailableFreqMaps(t, C_BAND_KEY),
                numericFrequency::assignedFrequency);
    }

    public static OpenRoadmSpectrumRangeExtractor defaultInstance(double startEdgeFrequencyThz, int effectiveBits) {
        return new DefaultOpenRoadmSpectrumRangeExtractor(
                new NumericFrequency(
                        startEdgeFrequencyThz,
                        effectiveBits,
                        new FrequencyMath()
                ),
                new TeraHertzFactory(),
                new FrequencyRangeFactory()
        );
    }

    public static OpenRoadmSpectrumRangeExtractor defaultInstance() {
        return DefaultOpenRoadmSpectrumRangeExtractor.defaultInstance(
                GridConstant.START_EDGE_FREQUENCY_THZ,
                GridConstant.EFFECTIVE_BITS
        );
    }
}
