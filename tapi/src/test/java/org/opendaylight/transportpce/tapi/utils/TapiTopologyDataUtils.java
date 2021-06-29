/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInputBuilder;

public final class TapiTopologyDataUtils {
    public static final String OPENROADM_TOPOLOGY_FILE = "src/test/resources/openroadm-topology.xml";
    public static final String OPENROADM_NETWORK_FILE = "src/test/resources/openroadm-network.xml";
    public static final String OTN_TOPOLOGY_FILE = "src/test/resources/otn-topology.xml";
    public static final String PORTMAPPING_FILE = "src/test/resources/portmapping.xml";

    public static GetTopologyDetailsInput buildGetTopologyDetailsInput(String topoName) {
        GetTopologyDetailsInputBuilder builtInput = new GetTopologyDetailsInputBuilder();
        builtInput.setTopologyIdOrName(topoName);
        return builtInput.build();
    }

    public static GetNodeDetailsInput buildGetNodeDetailsInput(String topoName, String nodeName) {
        GetNodeDetailsInputBuilder builtInput = new GetNodeDetailsInputBuilder();
        builtInput.setTopologyIdOrName(topoName);
        builtInput.setNodeIdOrName(nodeName);
        return builtInput.build();
    }

    public static GetLinkDetailsInput buildGetLinkDetailsInput(String topoName, String linkName) {
        GetLinkDetailsInputBuilder builtInput = new GetLinkDetailsInputBuilder();
        builtInput.setTopologyIdOrName(topoName);
        builtInput.setLinkIdOrName(linkName);
        return builtInput.build();
    }

    public static GetServiceInterfacePointListInput buildServiceInterfacePointListInput() {
        return new GetServiceInterfacePointListInputBuilder().build();
    }

    public static GetServiceInterfacePointDetailsInput buildGetServiceInterfacePointDetailsInput(Uuid sipUuid) {
        GetServiceInterfacePointDetailsInputBuilder builtInput = new GetServiceInterfacePointDetailsInputBuilder();
        builtInput.setSipIdOrName(sipUuid.getValue());
        return builtInput.build();
    }

    public static GetNodeEdgePointDetailsInput buildGetNodeEdgePointDetailsInput(String topoName,
                                                                                 String nodeName, String onepName) {
        GetNodeEdgePointDetailsInputBuilder builtInput = new GetNodeEdgePointDetailsInputBuilder();
        builtInput.setTopologyIdOrName(topoName);
        builtInput.setNodeIdOrName(nodeName);
        builtInput.setEpIdOrName(onepName);
        return builtInput.build();
    }

    private TapiTopologyDataUtils() {
    }
}
