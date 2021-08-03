/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.service;

import java.util.Map;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.PortQual;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceTypes {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceTypes.class);

    private ServiceTypes() {
    }

    public static String getServiceType(String serviceFormat, Uint32 serviceRate, Mapping mapping) {

        switch (serviceFormat) {
            case "OC":
                if (Uint32.valueOf(100).equals(serviceRate)) {
                    return StringConstants.SERVICE_TYPE_100GE_T;
                }
                LOG.warn("Invalid service-rate {}", serviceRate);
                return null;

            case "Ethernet":
                if (Uint32.valueOf(400).equals(serviceRate)) {
                    return StringConstants.SERVICE_TYPE_400GE;
                }
                if (Uint32.valueOf(100).equals(serviceRate)
                        && (mapping == null || !PortQual.SwitchClient.getName().equals(mapping.getPortQual()))) {
                    return StringConstants.SERVICE_TYPE_100GE_T;
                }
                return getOtnServiceType(serviceFormat, serviceRate);

            case "ODU":
            case "OTU":
                return getOtnServiceType(serviceFormat, serviceRate);

            default:
                LOG.warn("Invalid service-format {}", serviceFormat);
                return null;
        }
    }

    public static String getOtnServiceType(String serviceFormat, Uint32 serviceRate) {

        Map<Integer, String> ethernetMap = Map.of(
            1, StringConstants.SERVICE_TYPE_1GE,
            10, StringConstants.SERVICE_TYPE_10GE,
            100, StringConstants.SERVICE_TYPE_100GE_M);

        Map<Integer, String> otuMap = Map.of(
            100, StringConstants.SERVICE_TYPE_OTU4,
            400, StringConstants.SERVICE_TYPE_OTUC4);

        Map<Integer, String> oduMap = Map.of(
            100, StringConstants.SERVICE_TYPE_ODU4,
            400, StringConstants.SERVICE_TYPE_ODUC4);

        Map<String, Map<Integer, String>> otnMap = Map.of(
            "Ethernet", ethernetMap,
            "OTU", otuMap,
            "ODU", oduMap);

        if (!otnMap.containsKey(serviceFormat)) {
            LOG.warn("Invalid service-format {}", serviceFormat);
            return null;
        }

        if (!otnMap.get(serviceFormat).containsKey(serviceRate)) {
            LOG.warn("Invalid service-rate {}", serviceRate);
            return null;
        }

        return otnMap.get(serviceFormat).get(serviceRate);

    }
}
