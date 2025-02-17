/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;

class DeviceInterfaceTest {

    @Test
    void rollback() {
        Delete delete = mock(Delete.class);
        when(delete.deleteInterface("ROADM-A", "DEG1")).thenReturn(true);

        DeviceInterface n1 = new DeviceInterface("ROADM-A", "DEG1");
        assertTrue(n1.rollback(delete));

        verify(delete, times(1)).deleteInterface("ROADM-A", "DEG1");
    }

    @Test
    void testTwoInterfacesAreEqual() {
        DeviceInterface n1 = new DeviceInterface("ROADM-A", "DEG1");
        DeviceInterface n2 = new DeviceInterface("ROADM-A", "DEG1");

        assertTrue(n1.equals(n2));
    }

    @Test
    void testTwoInterfacesAreNotEqual() {
        DeviceInterface n1 = new DeviceInterface("ROADM-A", "DEG1");
        DeviceInterface n2 = new DeviceInterface("ROADM-B", "DEG1");

        assertFalse(n1.equals(n2));
    }
}