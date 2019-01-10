/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import org.junit.Assert;
import org.junit.Test;

public class OtsPmHolderTest {

    /*
     * test OtsPmHolder creation
     */
    @Test
    public void test() {
        OtsPmHolder otspmholder0 = new OtsPmHolder("name", Double.valueOf(12), "interface");

        Assert.assertEquals("name", otspmholder0.getOtsParameterName());
        Assert.assertEquals(Double.valueOf(12), otspmholder0.getOtsParameterVal());
        Assert.assertEquals("interface", otspmholder0.getOtsInterfaceName());

        otspmholder0.setOtsParameterName("name 2");
        otspmholder0.setOtsParameterVal(Double.valueOf(120));
        otspmholder0.setOtsInterfaceName("interface 2");

        Assert.assertEquals("name 2", otspmholder0.getOtsParameterName());
        Assert.assertEquals(Double.valueOf(120), otspmholder0.getOtsParameterVal());
        Assert.assertEquals("interface 2", otspmholder0.getOtsInterfaceName());
    }
}
