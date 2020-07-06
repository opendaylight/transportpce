/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FixedFlexImplTest {

    FixedFlexImpl fixedFlex = null;

    @Before
    public void setUp() {
        fixedFlex = new FixedFlexImpl();
    }

    @Test
    public void getFixedFlexWaveMappingTest() {
        FixedFlexImpl res = fixedFlex.getFixedFlexWaveMapping(10L);

        Assert.assertEquals(res.getIndex(),10);
        Assert.assertEquals(res.getCenterFrequency(),195.65,.5);
        Assert.assertEquals(res.getStart(),195.625,.5);
        Assert.assertEquals(res.getStop(),195.675,.5);
        Assert.assertEquals(res.getWavelength(),1532.37,.5);
    }

    @Test
    public void fixedFlexImpl1() {
        FixedFlexImpl res = new FixedFlexImpl(10L);

        Assert.assertEquals(res.getIndex(),0);
        Assert.assertEquals(res.getCenterFrequency(),195.65,.5);
        Assert.assertEquals(res.getStart(),195.625,.5);
        Assert.assertEquals(res.getStop(),195.675,.5);
        Assert.assertEquals(res.getWavelength(),1532.37,.5);
    }

    @Test
    public void fixedFlexImpl2() {
        FixedFlexImpl res = new FixedFlexImpl(1L, 19, 19, 19, 12);

        Assert.assertEquals(res.getIndex(),1L);
        Assert.assertEquals(res.getCenterFrequency(),19,.5);
        Assert.assertEquals(res.getStart(),19,.5);
        Assert.assertEquals(res.getStop(),19,.5);
        Assert.assertEquals(res.getWavelength(),12,.5);
    }
}
