/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import org.junit.Assert;
import org.junit.Test;

public class ServiceEndpointTypeTest {

    @Test
    public void testgetIntValue() {
        Assert.assertEquals(1, ServiceEndpointType.SERVICEAEND.getIntValue());
        Assert.assertEquals(2, ServiceEndpointType.SERVICEZEND.getIntValue());
    }

    @Test
    public void testForValue() {
        Assert.assertEquals(ServiceEndpointType.SERVICEAEND, ServiceEndpointType.forValue(1));
        Assert.assertEquals(ServiceEndpointType.SERVICEZEND, ServiceEndpointType.forValue(2));
    }

}
