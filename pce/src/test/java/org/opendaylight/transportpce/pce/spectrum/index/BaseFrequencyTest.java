/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.index;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BaseFrequencyTest {

    @Test
    void referenceFrequencySpectrumIndex() {

        Base baseFrequency = new BaseFrequency();
        Assertions.assertEquals(284, baseFrequency.referenceFrequencySpectrumIndex(
            193.1,
            191.325,
            6.25
        ));

    }

}
