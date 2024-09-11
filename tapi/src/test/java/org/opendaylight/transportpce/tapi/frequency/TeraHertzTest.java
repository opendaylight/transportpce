/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.common.Uint64;

class TeraHertzTest {


    @Test
    void teraHertz() {
        TeraHertz teraHertz = new TeraHertz(193.1);

        assertEquals(193.1, teraHertz.teraHertz().doubleValue());
    }

    @Test
    void hertz() {
        TeraHertz teraHertz = new TeraHertz(193.1);

        assertEquals(Uint64.valueOf("193100000000000"), teraHertz.hertz());

    }

    @Test
    void compare() {
        Frequency lightOne = new TeraHertz(193.1000);
        Frequency lightTwo = new TeraHertz(193.1);

        Assertions.assertTrue(lightTwo.compareTo(lightOne) == 0);
        Assertions.assertTrue(lightOne.equals(lightTwo));
    }
}