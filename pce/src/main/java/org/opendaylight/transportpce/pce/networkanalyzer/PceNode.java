/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
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

    boolean checkWL(long index);

    Map<String, List<Uint16>> getAvailableTribPorts();

    Map<String, List<Uint16>> getAvailableTribSlots();
}
