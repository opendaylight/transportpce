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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Extracts spectrum ranges from OpenROADM termination points.
 *
 * <p>This interface provides helpers to convert OpenROADM representations
 * (e.g. frequency bitmaps, used-wavelength lists, XPDR wavelength attributes)
 * into numeric spectrum ranges.
 *
 * <p>Returned ranges are expressed as {@code Map<Frequency, Frequency>} where each entry maps:
 * {@code lowerExclusive -> upperExclusive}. Implementations may return empty maps/ranges when
 * the relevant OpenROADM data is absent.
 */
public interface OpenRoadmSpectrumRangeExtractor {

    /**
     * Extracts occupied (used) spectrum intervals for an SRG-PP termination point.
     *
     * <p>The OpenROADM SRG-PP spectrum is derived from the PP attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return occupied spectrum intervals ({@code lowerBound -> upperBound}), never {@code null}
     */
    Map<Frequency, Frequency> getPP11UsedFrequencies(TerminationPoint1 tp);

    /**
     * Extracts available spectrum intervals for an SRG-PP termination point.
     *
     * <p>The OpenROADM SRG-PP spectrum is derived from the PP attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return available spectrum intervals ({@code lowerBound -> upperBound}), never {@code null}
     */
    Map<Frequency, Frequency> getPP11AvailableFrequencies(TerminationPoint1 tp);

    /**
     * Extracts occupied (assigned) spectrum from a DEG-TTP termination point.
     *
     * <p>The OpenROADM DEG-TTP spectrum is derived from the TxTtp attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return occupied spectrum as a {@link Range}, never {@code null}
     */
    Range getTTP11UsedFreqMap(TerminationPoint1 tp);

    /**
     * Extracts available spectrum from a DEG-TTP termination point.
     *
     * <p>The OpenROADM DEG-TTP spectrum is derived from the TxTtp attributes frequency bitmap (C-band).
     *
     * @param tp OpenROADM topology {@code TerminationPoint1} augmentation
     * @return available spectrum as a {@link Range}, never {@code null}
     */
    Range getTTP11AvailableFreqMap(TerminationPoint1 tp);

    /**
     * Extracts occupied (assigned) spectrum from a DEG-TTP termination point.
     *
     * <p>The OpenROADM DEG-TTP spectrum is derived from the TxTtp attributes (for example the frequency bitmap)
     * and converted into a {@link Range}.
     *
     * @param tp OpenROADM termination point (IETF topology termination point)
     * @return occupied spectrum as a {@link Range}, never {@code null}
     */
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
     */
    Map<Frequency, Frequency> getXpdrUsedWavelength(TerminationPoint tp);
}
