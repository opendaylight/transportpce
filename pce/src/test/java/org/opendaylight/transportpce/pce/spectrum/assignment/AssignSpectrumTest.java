/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment;

import java.util.BitSet;
import junit.framework.Assert;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;

class AssignSpectrumTest {


    @Test
    void testEmptyRange() {

        Assign assignSpectrum = new AssignSpectrum(new SpectrumIndex());
        Assert.assertEquals(new IndexRange(0, 0), assignSpectrum.range(
            768,
            284,
            new BitSet(),
            8,
            8
        ));

    }

    @Test
    void testLastBitsAreAvailable() {

        Assign assignSpectrum = new AssignSpectrum(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(760, 768);

        Assert.assertEquals(new IndexRange(760, 767), assignSpectrum.range(
            768,
            284,
            available,
            8,
            8
        ));

    }

    @Test
    void testSecondToLastBitsAreAvailable() {

        Assign assignSpectrum = new AssignSpectrum(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(752, 760);

        Assert.assertEquals(new IndexRange(752, 759), assignSpectrum.range(
            768,
            284,
            available,
            8,
            8
        ));

    }

    @Test
    void testFirstBitsAreAvailable() {

        Assign assignSpectrum = new AssignSpectrum(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 8);

        Assert.assertEquals(new IndexRange(0, 7), assignSpectrum.range(
            768,
            284,
            available,
            8,
            8
        ));

    }

    @Test
    void testFirstBitsAreAvailableTwiceCenterFrequencyGranularity() {

        Assign assignSpectrum = new AssignSpectrum(new SpectrumIndex());
        BitSet available = new BitSet();
        available.set(0, 32);

        Assert.assertEquals(new IndexRange(4, 19), assignSpectrum.range(
            768,
            284,
            available,
            16,
            16
        ));

    }
}
