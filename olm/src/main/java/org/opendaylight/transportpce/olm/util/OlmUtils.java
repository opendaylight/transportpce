/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.OpenroadmNodeVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OlmUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils.class);
    private static long DATABROKER_READ_TIMEOUT_SECONDS = 120;


    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity. Currently vendorExtentions
     * are excluded but can be added back based on requirement.
     *
     * <p>This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     * @param openRoadmVersion
     *            OpenRoadm version number
     *
     * @return Result of the request list of PM readings
     */
    public static GetPmOutputBuilder pmFetch(GetPmInput input, DeviceTransactionManager deviceTransactionManager,
                                             OpenroadmNodeVersion openRoadmVersion) {
        LOG.info("Getting PM Data for NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
            input.getResourceType(), input.getResourceIdentifier());
        GetPmOutputBuilder pmOutputBuilder;
        switch (openRoadmVersion.getIntValue()) {
            case 1:
                pmOutputBuilder = OlmUtils121.pmFetch(input, deviceTransactionManager);
                break;
            case 2:
                pmOutputBuilder = OlmUtils221.pmFetch(input, deviceTransactionManager);
                break;
            case 3:
                pmOutputBuilder = OlmUtils710.pmFetch(input, deviceTransactionManager);
                break;
            default:
                LOG.error("Unrecognized OpenRoadm version");
                pmOutputBuilder = new GetPmOutputBuilder();
        }
        return pmOutputBuilder;
    }

    private OlmUtils() {
    }

}
