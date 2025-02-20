/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

class PostAlgoPathValidatorTest {

    @Test
    void computeBestSpectrumAssignmentFixGrid() {
        ClientInput clientInput = mock(ClientInput.class);
        NetworkTransactionService networkTransactionService = mock(NetworkTransactionService.class);
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService, new BitSet(),
                clientInput);
        BitSet available = new BitSet(768);
        available.set(16, 28);
        boolean isFlexGrid = false;
        SpectrumAssignment expectedFixGrid = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(16))
                .setStopIndex(Uint16.valueOf(23))
                .setFlexGrid(isFlexGrid)
                .build();

        SpectrumAssignment fixGrid = postAlgoPathValidator.computeBestSpectrumAssignment(available, 8, isFlexGrid);

        assertEquals(expectedFixGrid, fixGrid);
    }

    @Test
    void computeBestSpectrumAssignmentFlexGrid() {
        ClientInput clientInput = mock(ClientInput.class);
        NetworkTransactionService networkTransactionService = mock(NetworkTransactionService.class);
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService, new BitSet(),
                clientInput);
        BitSet available = new BitSet(768);
        available.set(16, 28);
        boolean isFlexGrid = true;
        SpectrumAssignment expectedFlexGrid = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(20))
                .setStopIndex(Uint16.valueOf(27))
                .setFlexGrid(isFlexGrid)
                .build();

        SpectrumAssignment flexGrid = postAlgoPathValidator.computeBestSpectrumAssignment(available, 8, isFlexGrid);

        assertEquals(expectedFlexGrid, flexGrid);
    }
}
