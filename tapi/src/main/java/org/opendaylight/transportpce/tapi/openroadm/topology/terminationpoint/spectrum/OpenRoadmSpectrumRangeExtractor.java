/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
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
     * @param tp IETF termination point
     * @return extracted occupied + available spectrum ranges; never {@code null}
     */
    SpectrumRanges extract(TerminationPoint tp);

    /**
     * Extracts spectrum ranges from an OpenROADM {@link TerminationPoint1} topology augmentation
     * for ROADM termination point types (SRG-PP and DEG-TTP).
     *
     * <p>This is a lower-level overload of {@link #extract(TerminationPointId, TerminationPoint1)}
     * intended for cases where the OpenROADM TP type is already known and callers do not have (or
     * do not need) a full {@link TerminationPointId}.
     *
     * <p>Supported extraction strategies are:
     * <ul>
     *   <li>SRG-PP: decode PP attributes frequency bitmap (e.g. C-band)</li>
     *   <li>DEG-TTP: occupied derived from used-wavelength list when present, otherwise bitmap;
     *       available derived from bitmap</li>
     * </ul>
     *
     * <p>For unsupported TP types, {@link SpectrumRanges#empty()} is returned.
     *
     * @param openroadmTpType OpenROADM termination point type
     * @param tp OpenROADM topology augmentation
     * @return extracted occupied + available spectrum ranges; never {@code null}
     */
    SpectrumRanges extractRoadm(OpenroadmTpType openroadmTpType, TerminationPoint1 tp);

}
