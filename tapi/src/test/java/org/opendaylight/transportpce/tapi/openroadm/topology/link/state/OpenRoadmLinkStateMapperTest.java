/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;

class OpenRoadmLinkStateMapperTest {

    private final LinkStateMapper linkStateMapper = new OpenRoadmLinkStateMapper();

    @Test
    void toTapiAdminState_shouldMapStringsCorrectly() {
        assertEquals(AdministrativeState.UNLOCKED, linkStateMapper.toTapiAdminState("UNLOCKED"));
        assertEquals(AdministrativeState.UNLOCKED, linkStateMapper.toTapiAdminState("inService"));
        assertEquals(AdministrativeState.LOCKED, linkStateMapper.toTapiAdminState("LOCKED"));
        assertNull(linkStateMapper.toTapiAdminState((String)null));
    }

    @Test
    void toTapiOperationalState_shouldMapStringsCorrectly() {
        assertEquals(OperationalState.ENABLED, linkStateMapper.toTapiOperationalState("ENABLED"));
        assertEquals(OperationalState.ENABLED, linkStateMapper.toTapiOperationalState("inService"));
        assertEquals(OperationalState.DISABLED, linkStateMapper.toTapiOperationalState("DISABLED"));
        assertNull(linkStateMapper.toTapiOperationalState((String)null));
    }

    @Test
    void toTapiAdminState_shouldReturnUnlockedWhenBothEndpointsInService() {
        assertEquals(
                AdministrativeState.UNLOCKED,
                linkStateMapper.toTapiAdminState(AdminStates.InService, AdminStates.InService));
    }

    @Test
    void toTapiAdminState_shouldReturnLockedWhenBothEndpointsOutOfService() {
        assertEquals(
                AdministrativeState.LOCKED,
                linkStateMapper.toTapiAdminState(AdminStates.OutOfService, AdminStates.OutOfService));
    }

    @Test
    void toTapiAdminState_shouldReturnLockedWhenEndpointStatesDiffer() {
        assertEquals(
                AdministrativeState.LOCKED,
                linkStateMapper.toTapiAdminState(AdminStates.InService, AdminStates.OutOfService));
    }

    @Test
    void toTapiAdminState_shouldReturnNullWhenFirstEndpointStateIsNull() {
        assertNull(linkStateMapper.toTapiAdminState(null, AdminStates.InService));
    }

    @Test
    void toTapiAdminState_shouldReturnNullWhenSecondEndpointStateIsNull() {
        assertNull(linkStateMapper.toTapiAdminState(AdminStates.InService, null));
    }

    @Test
    void toTapiOperationalState_shouldReturnEnabledWhenBothEndpointsInService() {
        assertEquals(
                OperationalState.ENABLED,
                linkStateMapper.toTapiOperationalState(State.InService, State.InService));
    }

    @Test
    void toTapiOperationalState_shouldReturnDisabledWhenBothEndpointsOutOfService() {
        assertEquals(
                OperationalState.DISABLED,
                linkStateMapper.toTapiOperationalState(State.OutOfService, State.OutOfService));
    }

    @Test
    void toTapiOperationalState_shouldReturnDisabledWhenEndpointStatesDiffer() {
        assertEquals(
                OperationalState.DISABLED,
                linkStateMapper.toTapiOperationalState(State.InService, State.OutOfService));
    }

    @Test
    void toTapiOperationalState_shouldReturnNullWhenFirstEndpointStateIsNull() {
        assertNull(linkStateMapper.toTapiOperationalState(null, State.InService));
    }

    @Test
    void toTapiOperationalState_shouldReturnNullWhenSecondEndpointStateIsNull() {
        assertNull(linkStateMapper.toTapiOperationalState(State.InService, null));
    }
}
