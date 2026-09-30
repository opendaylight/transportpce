/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.BitSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.pce.spectrum.assignment.AssignSpectrumHighToLow;
import org.opendaylight.transportpce.pce.spectrum.index.BaseFrequency;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

class DefaultAssignableSpectrumTest {

    @Test
    @DisplayName("Computes fixed-grid assignment with default 50 GHz center-frequency granularity")
    void computesFixedGridAssignmentWithDefaultStrategy() {
        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = false;
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(16))
                .setStopIndex(Uint16.valueOf(23))
                .setFlexGrid(isFlexGrid)
                .build();

        AssignableSpectrum assignableSpectrum = new DefaultAssignableSpectrum(available, 8, 8, isFlexGrid);

        SpectrumAssignment fixGrid = assignableSpectrum.computeBestSpectrumAssignment(mock(Subscriber.class));

        assertEquals(expected, fixGrid);
    }

    @Test
    @DisplayName("Computes fixed-grid assignment with injected 50 GHz center-frequency granularity")
    void computesFixedGridAssignmentWithInjectedStrategy() {
        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = false;
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(16))
                .setStopIndex(Uint16.valueOf(23))
                .setFlexGrid(isFlexGrid)
                .build();

        AssignableSpectrum assignableSpectrum = new DefaultAssignableSpectrum(
                available,
                8,
                8,
                isFlexGrid,
                new BaseFrequency(),
                new AssignSpectrumHighToLow(new SpectrumIndex()));

        SpectrumAssignment fixGrid = assignableSpectrum.computeBestSpectrumAssignment(mock(Subscriber.class));

        assertEquals(expected, fixGrid);
    }

    @Test
    @DisplayName("Computes flex-grid assignment with default 6.25 GHz center-frequency granularity")
    void computesFlexGridAssignmentWithDefaultStrategy() {
        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = true;
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(20))
                .setStopIndex(Uint16.valueOf(27))
                .setFlexGrid(isFlexGrid)
                .build();

        AssignableSpectrum assignableSpectrum = new DefaultAssignableSpectrum(available, 8, 1, isFlexGrid);

        SpectrumAssignment flexGrid = assignableSpectrum.computeBestSpectrumAssignment(mock(Subscriber.class));

        assertEquals(expected, flexGrid);
    }

    @Test
    @DisplayName("Computes flex-grid assignment with injected 6.25 GHz center-frequency granularity")
    void computesFlexGridAssignmentWithInjectedStrategy() {
        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = true;
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(20))
                .setStopIndex(Uint16.valueOf(27))
                .setFlexGrid(isFlexGrid)
                .build();

        AssignableSpectrum assignableSpectrum = new DefaultAssignableSpectrum(
                available,
                8,
                1,
                isFlexGrid,
                new BaseFrequency(),
                new AssignSpectrumHighToLow(new SpectrumIndex()));

        SpectrumAssignment flexGrid = assignableSpectrum.computeBestSpectrumAssignment(mock(Subscriber.class));

        assertEquals(expected, flexGrid);
    }
}
