/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ServiceEndpointTypeTest {

    @Test
    void testgetIntValue() {
        assertEquals(1, ServiceEndpointType.SERVICEAEND.getIntValue());
        assertEquals(2, ServiceEndpointType.SERVICEZEND.getIntValue());
    }

    @Test
    void testForValue() {
        assertEquals(ServiceEndpointType.SERVICEAEND, ServiceEndpointType.forValue(1));
        assertEquals(ServiceEndpointType.SERVICEZEND, ServiceEndpointType.forValue(2));
    }
}