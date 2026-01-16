/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import java.util.Map;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.range.Range;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Extracts spectrum ranges from OpenROADM termination points.
 *
 * <p>This interface converts OpenROADM spectrum representations (e.g. frequency bitmaps,
 * used-wavelength lists, and XPDR wavelength attributes) into normalized frequency intervals.
 *
 * <p>The returned {@link SpectrumRanges} contains two sets of intervals:
 * <ul>
 *   <li><b>occupied</b> — spectrum currently assigned/used</li>
 *   <li><b>available</b> — spectrum currently free/available</li>
 * </ul>
 *
 * <p>Intervals are expressed as {@code Map<Frequency, Frequency>} where each entry maps
 * {@code lowerBound -> upperBound}. The interpretation of whether bounds are inclusive/exclusive
 * is a domain concern; callers should treat them as boundary markers for a continuous interval.
 *
 * <p>When relevant OpenROADM data is absent (missing augmentations/attributes/maps),
 * implementations return {@link SpectrumRanges#empty()} rather than throwing.
 */
public interface OpenRoadmSpectrumRangeExtractor {

    /**
     * Extracts spectrum ranges for a given OpenROADM {@link TerminationPoint1} augmentation,
     * using {@code tpId} to select an extraction strategy.
     *
     * <p>Typical strategies include:
     * <ul>
     *   <li>SRG-PP: decode PP attributes frequency bitmap (e.g. C-band)</li>
     *   <li>DEG-TTP: decode TxTtp attributes frequency bitmap and/or used-wavelength list</li>
     * </ul>
     *
     * @param tpId termination-point id/type metadata used to choose extraction strategy
     * @param tp termination point augmentation (OpenROADM)
     * @return extracted occupied + available spectrum ranges; never {@code null}
     */
    SpectrumRanges extract(TerminationPointId tpId, TerminationPoint1 tp);

    /**
     * Convenience overload for extracting spectrum ranges from an IETF {@link TerminationPoint}.
     *
     * <p>This overload is typically used for termination points where spectrum is represented
     * directly on the IETF TP via OpenROADM augmentations (e.g. XPDR wavelength attributes).
     * For unsupported TP types or missing data, {@link SpectrumRanges#empty()} is returned.
     *
     * @param tpId termination-point id/type metadata used to choose extraction strategy
     * @param tp IETF termination point
     * @return extracted occupied + available spectrum ranges; never {@code null}
     */
    SpectrumRanges extract(TerminationPointId tpId, TerminationPoint tp);

    /**
     * Extracts occupied (used) spectrum intervals for an SRG-PP termination point.
     *
     * <p>The OpenROADM SRG-PP spectrum is derived from the PP attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return occupied spectrum intervals ({@code lowerBound -> upperBound}), never {@code null}
     * @deprecated use {@link #extract(TerminationPointId, TerminationPoint1)} instead
     */
    @Deprecated(forRemoval = true)
    Map<Frequency, Frequency> getPP11UsedFrequencies(TerminationPoint1 tp);

    /**
     * Extracts available spectrum intervals for an SRG-PP termination point.
     *
     * <p>The OpenROADM SRG-PP spectrum is derived from the PP attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return available spectrum intervals ({@code lowerBound -> upperBound}), never {@code null}
     * @deprecated use {@link #extract(TerminationPointId, TerminationPoint1)} instead
     */
    @Deprecated(forRemoval = true)
    Map<Frequency, Frequency> getPP11AvailableFrequencies(TerminationPoint1 tp);

    /**
     * Extracts occupied (assigned) spectrum from a DEG-TTP termination point.
     *
     * <p>The OpenROADM DEG-TTP spectrum is derived from the TxTtp attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return occupied spectrum as a {@link Range}, never {@code null}
     * @deprecated use {@link #extract(TerminationPointId, TerminationPoint1)} instead
     */
    @Deprecated(forRemoval = true)
    Range getTTP11UsedFreqMap(TerminationPoint1 tp);

    /**
     * Extracts available spectrum from a DEG-TTP termination point.
     *
     * <p>The OpenROADM DEG-TTP spectrum is derived from the TxTtp attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return available spectrum as a {@link Range}, never {@code null}
     * @deprecated use {@link #extract(TerminationPointId, TerminationPoint1)} instead
     */
    @Deprecated(forRemoval = true)
    Range getTTP11AvailableFreqMap(TerminationPoint1 tp);

    /**
     * Extracts occupied (assigned) spectrum from a DEG-TTP termination point.
     *
     * <p>The OpenROADM DEG-TTP spectrum is derived from the TxTtp attributes (for example the frequency bitmap)
     * and converted into a {@link Range}.
     *
     * @param tp OpenROADM termination point (IETF topology termination point)
     * @return occupied spectrum as a {@link Range}, never {@code null}
     * @deprecated use {@link #extract(TerminationPointId, TerminationPoint)} instead
     */
    @Deprecated(forRemoval = true)
    Range getTTPUsedFreqMap(TerminationPoint tp);

    /**
     * Extracts the provisioned/used wavelength for an XPDR termination point.
     *
     * <p>Reads the XPDR wavelength (center frequency + width) from the OpenROADM termination point
     * augmentation and converts it into a frequency interval.
     *
     * <p>If the required augmentation or wavelength attributes are missing, an empty map is returned.
     *
     * @param tp OpenROADM termination point (IETF topology termination point)
     * @return occupied wavelength interval ({@code lowerBound -> upperBound}), or empty
     * @deprecated use {@link #extract(TerminationPointId, TerminationPoint)} instead
     */
    @Deprecated(forRemoval = true)
    Map<Frequency, Frequency> getXpdrUsedWavelength(TerminationPoint tp);
}
