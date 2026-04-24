/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.state;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmLinkStateMapper implements LinkStateMapper {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmLinkStateMapper.class);

    @Override
    public AdministrativeState toTapiAdminState(AdminStates adminState) {
        if (adminState != null) {
            return toTapiAdminState(adminState.getName());
        }

        return toTapiAdminState((String)null);
    }

    @Override
    public AdministrativeState toTapiAdminState(String adminState) {
        if (adminState == null) {
            return null;
        }
        return adminState.equals(AdminStates.InService.getName())
                || adminState.equals(AdministrativeState.UNLOCKED.getName()) ? AdministrativeState.UNLOCKED
                : AdministrativeState.LOCKED;
    }

    @Override
    public AdministrativeState toTapiAdminState(AdminStates adminState1, AdminStates adminState2) {
        if (adminState1 == null || adminState2 == null) {
            return null;
        }
        LOG.info("Admin state 1 = {}, admin state 2 = {}", adminState1.getName(), adminState2.getName());
        return AdminStates.InService.equals(adminState1) && AdminStates.InService.equals(adminState2)
                ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    @Override
    public OperationalState toTapiOperationalState(State operState) {
        if (operState != null) {
            return toTapiOperationalState(operState.getName());
        }

        return toTapiOperationalState((String)null);
    }

    @Override
    public OperationalState toTapiOperationalState(String operState) {
        if (operState == null) {
            return null;
        }
        return operState.equals("inService") || operState.equals(OperationalState.ENABLED.getName())
                ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    @Override
    public OperationalState toTapiOperationalState(State operState1, State operState2) {
        if (operState1 == null || operState2 == null) {
            return null;
        }
        return State.InService.equals(operState1) && State.InService.equals(operState2)
                ? OperationalState.ENABLED : OperationalState.DISABLED;
    }
}
