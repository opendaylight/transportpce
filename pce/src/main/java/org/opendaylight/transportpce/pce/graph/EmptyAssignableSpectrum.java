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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * Empty assignable spectrum used when path validation determines that no frequency slots can be assigned.
 */
public class EmptyAssignableSpectrum implements AssignableSpectrum {

    @Override
    public BitSet availableBitSet() {
        return new BitSet();
    }

    @Override
    public SpectrumAssignment computeBestSpectrumAssignment(Subscriber subscriber) {

        return new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(0))
                .setStopIndex(Uint16.valueOf(0))
                .setFlexGrid(true)
                .build();
    }
}
