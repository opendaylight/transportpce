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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.yang.common.Uint16;

public interface PceNode {
    String getPceNodeType();

    String getSupNetworkNodeId();

    String getSupClliNodeId();

    void addOutgoingLink(PceLink outLink);

    String getRdmSrgClient(String tp);

    String getXpdrClient(String tp);

    boolean checkTP(String tp);

    List<PceLink> getOutgoingLinks();

    AdminStates getAdminStates();

    State getState();

    NodeId getNodeId();

    Map<String, List<Uint16>> getAvailableTribPorts();

    Map<String, List<Uint16>> getAvailableTribSlots();

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
     * @return BigDecimal.
     */
    BigDecimal getSlotWidthGranularity();

    /**
     * For optical node the central-frequency granularity from mc capabilities.
     * @return BigDecimal.
     */
    BigDecimal getCentralFreqGranularity();
}
