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

/**
 * Assignable spectrum calculated for a path after node frequency maps, media-channel capabilities, and service
 * constraints have been applied.
 */
public interface AssignableSpectrum {

    /**
     * Return the slots that remain assignable for the path.
     *
     * @return a bitset where set bits represent slots that can be used by the service.
     */
    BitSet availableBitSet();

    /**
     * Compute the best spectrum assignment from the assignable slots.
     *
     * @param subscriber observer notified when no suitable spectrum assignment can be found.
     * @return a spectrum assignment containing begin and stop indices, or beginIndex = stopIndex = 0 when no
     *         assignment is available.
     */
    SpectrumAssignment computeBestSpectrumAssignment(Subscriber subscriber);

}
