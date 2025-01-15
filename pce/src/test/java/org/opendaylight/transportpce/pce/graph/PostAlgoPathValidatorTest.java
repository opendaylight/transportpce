/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

class PostAlgoPathValidatorTest {

    @Test
    void computeBestSpectrumAssignmentFixGrid() {

        ClientInput clientInput = Mockito.mock(ClientInput.class);

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
                networkTransactionService,
                new BitSet(),
                clientInput
        );

        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = false;
        SpectrumAssignment expectedFixGrid = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(16))
                .setStopIndex(Uint16.valueOf(23))
                .setFlexGrid(isFlexGrid)
                .build();

        SpectrumAssignment fixGrid = postAlgoPathValidator.computeBestSpectrumAssignment(available, 8, 8, isFlexGrid);

        Assertions.assertEquals(expectedFixGrid, fixGrid);


    }

    @Test
    void computeBestSpectrumAssignmentFlexGrid() {
        ClientInput clientInput = Mockito.mock(ClientInput.class);

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
                networkTransactionService,
                new BitSet(),
                clientInput
        );

        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = true;
        SpectrumAssignment expectedFlexGrid = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(16))
                .setStopIndex(Uint16.valueOf(23))
                .setFlexGrid(isFlexGrid)
                .build();

        SpectrumAssignment flexGrid = postAlgoPathValidator.computeBestSpectrumAssignment(available, 8, 8, isFlexGrid);

        Assertions.assertEquals(expectedFlexGrid, flexGrid);


    }
}
