/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.MappingUtils;

class DeviceVersionResolverTest {

    private static final String NODE = "node1";

    private final MappingUtils mappingUtils = mock(MappingUtils.class);

    private final DeviceVersionResolver resolver = new DeviceVersionResolver(mappingUtils);

    @Test
    void testResolvesVersion221() throws Exception {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1);

        assertEquals(DeviceVersion.V2_2_1, resolver.resolve(NODE, "read interface X"));
    }

    @Test
    void testResolvesVersion71() throws Exception {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn(StringConstants.OPENROADM_DEVICE_VERSION_7_1);

        assertEquals(DeviceVersion.V7_1, resolver.resolve(NODE, "read interface X"));
    }

    @Test
    void testUnsupportedVersionNamesTheOperationNodeAndVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn("1.2.1");

        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> resolver.resolve(NODE, "delete interface X"));
        assertEquals("Could not delete interface X on node node1: unknown OpenROADM version 1.2.1",
            thrown.getMessage());
    }

    @Test
    void testNodeWithoutAVersionIsReported() {
        // For example a node missing from the port mapping
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn(null);

        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> resolver.resolve(NODE, "delete cross connect X"));
        assertEquals("Could not delete cross connect X on node node1: unknown OpenROADM version null",
            thrown.getMessage());
    }
}
