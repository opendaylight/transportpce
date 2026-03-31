/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;

/**
 * Service for building TAPI links and resolving their associated state and CEP data.
 *
 * <p>This interface provides utility methods to:
 * <ul>
 *   <li>create a TAPI {@link Link} between two node edge points,</li>
 *   <li>translate OpenROADM administrative and operational states into TAPI states,</li>
 *   <li>retrieve the effective state of a link from its endpoint NEPs, and</li>
 *   <li>expose the map of generated TAPI connection end points (CEPs).</li>
 * </ul>
 */
public interface TapiLink {

    /**
     * Creates a TAPI link between a source and destination termination point.
     *
     * <p>The link is built from the supplied source/destination node and TP identifiers,
     * link type, qualifiers, administrative/operational state, supported layer protocols,
     * and the UUID of the target TAPI topology.
     *
     * @param srcNodeId source node identifier
     * @param srcTpId source termination point identifier
     * @param dstNodeId destination node identifier
     * @param dstTpId destination termination point identifier
     * @param linkType link type used to determine how the TAPI link is built
     * @param srcNodeQual qualifier associated with the source node
     * @param dstNodeQual qualifier associated with the destination node
     * @param srcTpQual qualifier associated with the source termination point
     * @param dstTpQual qualifier associated with the destination termination point
     * @param adminState administrative state as a string
     * @param operState operational state as a string
     * @param layerProtoNameList set of layer protocols supported by the link
     * @param transLayerNameList set of transition layer protocol names
     * @param tapiTopoUuid UUID of the TAPI topology that will contain the link
     * @return the created TAPI link, or {@code null} if the link type is not recognized
     *         or the link cannot be created
     */
    Link createTapiLink(
            String srcNodeId,
            String srcTpId,
            String dstNodeId,
            String dstTpId,
            String linkType,
            String srcNodeQual,
            String dstNodeQual,
            String srcTpQual,
            String dstTpQual,
            String adminState,
            String operState,
            Set<LayerProtocolName> layerProtoNameList,
            Set<String> transLayerNameList,
            Uuid tapiTopoUuid);

    /**
     * Converts a textual administrative state into the corresponding TAPI administrative state.
     *
     * @param adminState administrative state expressed as a string
     * @return {@link AdministrativeState#UNLOCKED}, {@link AdministrativeState#LOCKED},
     *         or {@code null} if the input is {@code null}
     */
    AdministrativeState setTapiAdminState(String adminState);

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
    AdministrativeState setTapiAdminState(AdminStates adminState1, AdminStates adminState2);

    /**
     * Converts a textual operational state into the corresponding TAPI operational state.
     *
     * @param operState operational state expressed as a string
     * @return {@link OperationalState#ENABLED}, {@link OperationalState#DISABLED},
     *         or {@code null} if the input is {@code null}
     */
    OperationalState setTapiOperationalState(String operState);

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
    OperationalState setTapiOperationalState(State operState1, State operState2);

    /**
     * Retrieves the effective operational state of a link from the corresponding source and destination NEPs.
     *
     * @param srcNodeId source node identifier
     * @param destNodeId destination node identifier
     * @param sourceTpId source termination point identifier
     * @param destTpId destination termination point identifier
     * @return the effective operational state name, or {@code null} if it cannot be resolved
     */
    String getOperState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId);

    /**
     * Retrieves the effective administrative state of a link from the corresponding source and destination NEPs.
     *
     * @param srcNodeId source node identifier
     * @param destNodeId destination node identifier
     * @param sourceTpId source termination point identifier
     * @param destTpId destination termination point identifier
     * @return the effective administrative state name, or {@code null} if it cannot be resolved
     */
    String getAdminState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId);

    /**
     * Returns the map of generated TAPI connection end points indexed by their associated UUID mapping.
     *
     * @return a map containing generated CEPs
     */
    Map<Map<String, String>, ConnectionEndPoint> getCepMap();
}
