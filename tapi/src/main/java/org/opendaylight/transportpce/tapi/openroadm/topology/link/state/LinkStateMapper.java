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

public interface LinkStateMapper {

    /**
     * Converts an OpenROADM administrative state into the corresponding TAPI administrative state.
     *
     * @param adminState the OpenROADM administrative state
     * @return {@link AdministrativeState#UNLOCKED}, {@link AdministrativeState#LOCKED},
     *         or {@code null} if the input is {@code null}
     */
    AdministrativeState toTapiAdminState(AdminStates adminState);

    /**
     * Converts an OpenROADM administrative state into the corresponding TAPI administrative state.
     *
     * @param adminState the OpenROADM administrative state
     * @return {@link AdministrativeState#UNLOCKED}, {@link AdministrativeState#LOCKED},
     *         or {@code null} if the input is {@code null}
     */
    AdministrativeState toTapiAdminState(String adminState);

    /**
     * Converts two OpenROADM administrative states into a single effective TAPI administrative state.
     *
     * <p>The effective state is typically derived from both link endpoints.
     *
     * @param adminState1 administrative state of the first endpoint
     * @param adminState2 administrative state of the second endpoint
     * @return {@link AdministrativeState#UNLOCKED} when both endpoints are in service,
     *         otherwise {@link AdministrativeState#LOCKED}; {@code null} if either input is {@code null}
     */
    AdministrativeState toTapiAdminState(AdminStates adminState1, AdminStates adminState2);

    /**
     * Converts a operational state into the corresponding TAPI operational state.
     *
     * @param operState operational state
     * @return {@link OperationalState#ENABLED}, {@link OperationalState#DISABLED},
     *         or {@code null} if the input is {@code null}
     */
    OperationalState toTapiOperationalState(State operState);

    /**
     * Converts a textual operational state into the corresponding TAPI operational state.
     *
     * @param operState operational state expressed as a string
     * @return {@link OperationalState#ENABLED}, {@link OperationalState#DISABLED},
     *         or {@code null} if the input is {@code null}
     */
    OperationalState toTapiOperationalState(String operState);

    /**
     * Converts two OpenROADM operational states into a single effective TAPI operational state.
     *
     * <p>The effective state is typically derived from both link endpoints.
     *
     * @param operState1 operational state of the first endpoint
     * @param operState2 operational state of the second endpoint
     * @return {@link OperationalState#ENABLED} when both endpoints are in service,
     *         otherwise {@link OperationalState#DISABLED}; {@code null} if either input is {@code null}
     */
    OperationalState toTapiOperationalState(State operState1, State operState2);
}
