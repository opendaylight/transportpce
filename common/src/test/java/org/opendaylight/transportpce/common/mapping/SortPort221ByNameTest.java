/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.Port;

public class SortPort221ByNameTest {

    @Test
    public void compareTest() {
        Port port1 = mock(Port.class);
        Port port2 = mock(Port.class);
        when(port1.getPortName()).thenReturn("port1");
        when(port2.getPortName()).thenReturn("port2");
        SortPort221ByName sortPort221ByName = new SortPort221ByName();
        assertEquals(sortPort221ByName.compare(port2, port1), 1);
    }
}