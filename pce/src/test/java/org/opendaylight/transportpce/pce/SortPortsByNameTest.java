/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SortPortsByNameTest {

    private SortPortsByName sortPortsByName = new SortPortsByName();

    @Test
    void compareTest() {
        assertEquals(12, sortPortsByName.compare("value22", "valu10"));
    }

    @Test
    void compareWithoutNUM() {
        assertEquals(0, sortPortsByName.compare("value", "value"));
    }

    @Test
    void compareLessThan() {
        assertEquals(-11, sortPortsByName.compare("value1", "value12"));
    }
}