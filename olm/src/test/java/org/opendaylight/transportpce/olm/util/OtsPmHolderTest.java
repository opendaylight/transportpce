/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OtsPmHolderTest {

    @Test
    void test() {
        OtsPmHolder otspmholder0 = new OtsPmHolder("name", Double.valueOf(12), "interface");

        assertEquals("name", otspmholder0.getOtsParameterName());
        assertEquals(Double.valueOf(12), otspmholder0.getOtsParameterVal());
        assertEquals("interface", otspmholder0.getOtsInterfaceName());

        otspmholder0.setOtsParameterName("name 2");
        otspmholder0.setOtsParameterVal(Double.valueOf(120));
        otspmholder0.setOtsInterfaceName("interface 2");

        assertEquals("name 2", otspmholder0.getOtsParameterName());
        assertEquals(Double.valueOf(120), otspmholder0.getOtsParameterVal());
        assertEquals("interface 2", otspmholder0.getOtsInterfaceName());
    }
}
