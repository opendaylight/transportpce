/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yangtools.yang.common.Uint16;

public interface PceNode {
    String getPceNodeType();

    String getSupNetworkNodeId();

    String getSupClliNodeId();

    void addOutgoingLink(PceLink outLink);

    String getRdmSrgClient(String tp, String direction);

    String getXpdrClient(String tp);

    boolean checkTP(String tp);

    List<PceLink> getOutgoingLinks();

    AdminStates getAdminStates();

    AdministrativeState getAdminState();

    State getState();

    OperationalState getOperationalState();

    NodeId getNodeId();

    OpenroadmNodeType getORNodeType();

    String getOperationalMode();

    String getXponderOperationalMode(XpdrNetworkAttributes tp);

    String getXpdrOperationalMode(Uuid nepUuid);

    Uuid getNodeUuid();

    Map<String, List<Uint16>> getAvailableTribPorts();

    Map<String, List<Uint16>> getAvailableTribSlots();

    List<BasePceNep> getListOfNep();

    /**
     * Get the version of node.
     * @return the OpenROADM yang release supported by the node.
     */
    String getVersion();

    /**
     * For optical node, the spectrumOccupation of the node.
     * @return BitSet.
     */
    BitSet getBitSetData();

    /**
     * For optical node the slot width granularity from mc capabilities.
     * @return Decimal64.
     */
    BigDecimal getSlotWidthGranularity();

    /**
     * For optical node the central-frequency granularity from mc capabilities.
     * @return Decimal64.
     */
    BigDecimal getCentralFreqGranularity();
}
