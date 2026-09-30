/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Arrays;
import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

class EmptyAssignableSpectrumTest {

    @Test
    void availableBitSetIsEmpty() {
        assertTrue(new EmptyAssignableSpectrum().availableBitSet().isEmpty());
    }

    @Test
    void computeBestSpectrumAssignmentReturnsZeroIndices() {
        Subscriber subscriber = mock(Subscriber.class);

        assertEquals(new SpectrumAssignmentBuilder()
                        .setBeginIndex(Uint16.valueOf(0))
                        .setStopIndex(Uint16.valueOf(0))
                        .setFlexGrid(true)
                        .build(),
                new EmptyAssignableSpectrum().computeBestSpectrumAssignment(subscriber));
        verifyNoInteractions(subscriber);
    }

    /**
     * AssignableSpectrum defines an empty spectrum as a BitSet with no bits set. This only holds
     * as long as a frequency map where every slot is used also converts to a BitSet with no bits
     * set. If the slot encoding in GridConstant changes, this test fails and the code converting
     * between frequency maps and BitSets needs to be reviewed.
     */
    @Test
    void usedSlotEncodingMatchesEmptyBitSet() {
        byte[] allUsed = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(allUsed, (byte) GridConstant.USED_SLOT_VALUE);

        assertEquals(new EmptyAssignableSpectrum().availableBitSet(), BitSet.valueOf(allUsed));
    }
}
