/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.transportpce.pce.SortPortsByName;

public class SortPortsByNameTest {

    private SortPortsByName sortPortsByName = new SortPortsByName();

    @Test
    public void compareTest() {
        Assert.assertEquals(12, sortPortsByName.compare("value22", "valu10"));
    }

    @Test
    public void compareWithoutNUM() {
        Assert.assertEquals(0, sortPortsByName.compare("value", "value"));
    }

    @Test
    public void compareLessThan() {
        Assert.assertEquals(-11, sortPortsByName.compare("value1", "value12"));
    }


}
