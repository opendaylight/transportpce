/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;

@ExtendWith(MockitoExtension.class)
class OpenRoadmInterfacesImplTest {

    private static final String NODE = "ROADM-A1";
    private static final String INTERFACE = "DEG1-TTP-TXRX-nmc-761:768";

    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private MappingUtils mappingUtils;
    @Mock
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    @Mock
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;

    private OpenRoadmInterfacesImpl openRoadmInterfaces;

    @BeforeEach
    void setup() {
        openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    @Test
    void readInterfaceReportsAnUnknownVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn("1.2.1");

        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.readInterface(NODE, INTERFACE));
        assertTrue(thrown.getMessage().contains("unknown OpenROADM version 1.2.1"), thrown.getMessage());
        verifyNoInteractions(openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    @Test
    void readInterfaceReportsANodeWithoutAVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn(null);

        assertThrows(OpenRoadmInterfaceException.class, () -> openRoadmInterfaces.readInterface(NODE, INTERFACE));
        verifyNoInteractions(openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    @Test
    void getSupportedInterfaceReportsAnUnknownVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn("1.2.1");

        // An unknown version must not look like an interface without a supporting interface
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.getSupportedInterface(NODE, INTERFACE));
        assertTrue(thrown.getMessage().contains("unknown OpenROADM version 1.2.1"), thrown.getMessage());
        verifyNoInteractions(openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    @Test
    void getSupportedInterfaceReportsANodeWithoutAVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn(null);

        assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.getSupportedInterface(NODE, INTERFACE));
        verifyNoInteractions(openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    @Test
    void deleteInterfaceReportsAnUnknownVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn("1.2.1");

        // Returning normally would report an interface that is still on the device as deleted
        OpenRoadmInterfaceException thrown = assertThrows(OpenRoadmInterfaceException.class,
            () -> openRoadmInterfaces.deleteInterface(NODE, INTERFACE));
        assertTrue(thrown.getMessage().contains("unknown OpenROADM version 1.2.1"), thrown.getMessage());
        verifyNoInteractions(openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    @Test
    void deleteInterfaceReportsANodeWithoutAVersion() {
        when(mappingUtils.getOpenRoadmVersion(NODE)).thenReturn(null);

        assertThrows(OpenRoadmInterfaceException.class, () -> openRoadmInterfaces.deleteInterface(NODE, INTERFACE));
        verifyNoInteractions(openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }
}
