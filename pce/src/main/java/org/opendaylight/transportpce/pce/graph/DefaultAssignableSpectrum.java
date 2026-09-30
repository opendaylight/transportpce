/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.BitSet;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.pce.spectrum.assignment.Assign;
import org.opendaylight.transportpce.pce.spectrum.assignment.AssignSpectrumHighToLow;
import org.opendaylight.transportpce.pce.spectrum.assignment.Range;
import org.opendaylight.transportpce.pce.spectrum.index.Base;
import org.opendaylight.transportpce.pce.spectrum.index.BaseFrequency;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.event.Level;

/**
 * Default {@link AssignableSpectrum} implementation backed by a filtered spectrum bitset and the standard
 * high-to-low spectrum assignment strategy.
 */
public class DefaultAssignableSpectrum implements AssignableSpectrum {

    private final BitSet availableSlots;

    private final int spectralWidthSlotNumber;

    private final int nrOfSlotsSeparatingCenterFrequencies;

    private final boolean isFlexGrid;

    private final Base baseFrequency;

    private final Assign assignSpectrum;

    /**
     * Create assignable spectrum using the default base frequency and high-to-low assignment strategy.
     *
     * @param availableSlots bitset containing slots that remain assignable for the path.
     * @param spectralWidthSlotNumber number of slots required by the service spectral width.
     * @param nrOfSlotsSeparatingCenterFrequencies number of slots between valid center frequencies.
     * @param isFlexGrid true when the resulting assignment belongs to a flex-grid service.
     */
    public DefaultAssignableSpectrum(
            BitSet availableSlots,
            int spectralWidthSlotNumber,
            int nrOfSlotsSeparatingCenterFrequencies,
            boolean isFlexGrid) {

        this(
                availableSlots,
                spectralWidthSlotNumber,
                nrOfSlotsSeparatingCenterFrequencies,
                isFlexGrid,
                new BaseFrequency(),
                new AssignSpectrumHighToLow(new SpectrumIndex()));
    }

    /**
     * Create assignable spectrum using explicit spectrum-index and assignment collaborators.
     *
     * @param availableSlots bitset containing slots that remain assignable for the path.
     * @param spectralWidthSlotNumber number of slots required by the service spectral width.
     * @param nrOfSlotsSeparatingCenterFrequencies number of slots between valid center frequencies.
     * @param isFlexGrid true when the resulting assignment belongs to a flex-grid service.
     * @param baseFrequency base frequency used to align spectrum indices with the fixed-grid reference frequency.
     * @param assignSpectrum assignment strategy used to select the lower and upper spectrum indices.
     */
    public DefaultAssignableSpectrum(
            BitSet availableSlots,
            int spectralWidthSlotNumber,
            int nrOfSlotsSeparatingCenterFrequencies,
            boolean isFlexGrid,
            Base baseFrequency,
            Assign assignSpectrum) {

        this.availableSlots = (BitSet) availableSlots.clone();
        this.spectralWidthSlotNumber = spectralWidthSlotNumber;
        this.nrOfSlotsSeparatingCenterFrequencies = nrOfSlotsSeparatingCenterFrequencies;
        this.isFlexGrid = isFlexGrid;
        this.baseFrequency = baseFrequency;
        this.assignSpectrum = assignSpectrum;
    }

    @Override
    public BitSet availableBitSet() {
        return (BitSet) availableSlots.clone();
    }

    /**
     * Compute spectrum assignment from the available slots for the spectral width.
     *
     * @param subscriber will be notified about errors.
     * @return a spectrum assignment object which contains begin and stop index. If
     *         no spectrum assignment found, beginIndex = stopIndex = 0
     */
    @Override
    public SpectrumAssignment computeBestSpectrumAssignment(Subscriber subscriber) {

        Range range = assignSpectrum.range(
                GridConstant.EFFECTIVE_BITS,
                baseFrequency.referenceFrequencySpectrumIndex(
                        GridConstant.CENTRAL_FREQUENCY_THZ,
                        GridConstant.START_EDGE_FREQUENCY_THZ,
                        GridConstant.GRANULARITY
                ),
                availableSlots,
                nrOfSlotsSeparatingCenterFrequencies,
                spectralWidthSlotNumber
        );

        if (range.lower() == 0 && range.upper() == 0) {
            subscriber.event(Level.ERROR, "No frequencies available.");
        }

        return new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(range.lower()))
                .setStopIndex(Uint16.valueOf(range.upper()))
                .setFlexGrid(isFlexGrid)
                .build();
    }
}
