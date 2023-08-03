/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsInputBuilder;

public final class TapiTopologyDataUtils {
    public static final String OPENROADM_TOPOLOGY_FILE = "src/test/resources/openroadm-topology.xml";
    public static final String OPENROADM_NETWORK_FILE = "src/test/resources/openroadm-network.xml";
    public static final String OTN_TOPOLOGY_FILE = "src/test/resources/otn-topology.xml";
    public static final String PORTMAPPING_FILE = "src/test/resources/portmapping.xml";

    public static GetTopologyDetailsInput buildGetTopologyDetailsInput(Uuid topoId) {
        return new GetTopologyDetailsInputBuilder()
            .setTopologyId(topoId)
            .build();
    }

    public static GetNodeDetailsInput buildGetNodeDetailsInput(Uuid topoId, Uuid nodeId) {
        return new GetNodeDetailsInputBuilder()
            .setTopologyId(topoId)
            .setNodeId(nodeId)
            .build();
    }

    public static GetLinkDetailsInput buildGetLinkDetailsInput(Uuid topoId, Uuid linkId) {
        return new GetLinkDetailsInputBuilder()
            .setTopologyId(topoId)
            .setLinkId(linkId)
            .build();
    }

    public static GetServiceInterfacePointListInput buildServiceInterfacePointListInput() {
        return new GetServiceInterfacePointListInputBuilder()
            .build();
    }

    public static GetServiceInterfacePointDetailsInput buildGetServiceInterfacePointDetailsInput(Uuid sipUuid) {
        return new GetServiceInterfacePointDetailsInputBuilder()
            .setUuid(sipUuid)
            .build();
    }

    public static GetNodeEdgePointDetailsInput buildGetNodeEdgePointDetailsInput(Uuid topoId, Uuid nodeId,
            Uuid onepId) {
        return new GetNodeEdgePointDetailsInputBuilder()
            .setTopologyId(topoId)
            .setNodeId(nodeId)
            .setNodeEdgePointId(onepId)
            .build();
    }

    private TapiTopologyDataUtils() {
    }
}