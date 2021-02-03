/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

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

    private TapiTopologyDataUtils() {
    }

}
