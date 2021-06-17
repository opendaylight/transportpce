/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.service;

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
        String serviceType = null;
        if ("Ethernet".equals(serviceFormat)) {
            switch (serviceRate.intValue()) {
                case 1:
                    serviceType = StringConstants.SERVICE_TYPE_1GE;
                    break;
                case 10:
                    serviceType = StringConstants.SERVICE_TYPE_10GE;
                    break;
                case 100:
                    serviceType = StringConstants.SERVICE_TYPE_100GE_T;
                    if (mapping != null && PortQual.SwitchClient.getName().equals(mapping.getPortQual())) {
                        serviceType = StringConstants.SERVICE_TYPE_100GE_M;
                    }
                    break;
                case 400:
                    serviceType = StringConstants.SERVICE_TYPE_400GE;
                    break;
                default:
                    LOG.warn("Invalid service-rate {}", serviceRate);
                    break;
            }
        }
        if ("OC".equals(serviceFormat) && Uint32.valueOf(100).equals(serviceRate)) {
            serviceType = StringConstants.SERVICE_TYPE_100GE_T;
        }
        if ("OTU".equals(serviceFormat)) {
            switch (serviceRate.intValue()) {
                case 100:
                    serviceType = StringConstants.SERVICE_TYPE_OTU4;
                    break;
                case 400:
                    serviceType = StringConstants.SERVICE_TYPE_OTUC4;
                    break;
                default:
                    LOG.warn("Invalid service-rate {}", serviceRate);
                    break;
            }
        }
        if ("ODU".equals(serviceFormat)) {
            switch (serviceRate.intValue()) {
                case 100:
                    serviceType = StringConstants.SERVICE_TYPE_ODU4;
                    break;
                case 400:
                    serviceType = StringConstants.SERVICE_TYPE_ODUC4;
                    break;
                default:
                    LOG.warn("Invalid service-rate {}", serviceRate);
                    break;
            }
        }
        return serviceType;
    }

    public static String getOtnServiceType(String serviceFormat, Uint32 serviceRate) {
        String serviceType = null;
        if ("Ethernet".equals(serviceFormat)) {
            switch (serviceRate.intValue()) {
                case 1:
                    serviceType = StringConstants.SERVICE_TYPE_1GE;
                    break;
                case 10:
                    serviceType = StringConstants.SERVICE_TYPE_10GE;
                    break;
                case 100:
                    serviceType = StringConstants.SERVICE_TYPE_100GE_M;
                    break;
                default:
                    LOG.warn("Invalid service-rate {}", serviceRate);
                    break;
            }
        }
        if ("OTU".equals(serviceFormat)) {
            switch (serviceRate.intValue()) {
                case 100:
                    serviceType = StringConstants.SERVICE_TYPE_OTU4;
                    break;
                case 400:
                    serviceType = StringConstants.SERVICE_TYPE_OTUC4;
                    break;
                default:
                    LOG.warn("Invalid service-rate {}", serviceRate);
                    break;
            }
        }
        if ("ODU".equals(serviceFormat)) {
            switch (serviceRate.intValue()) {
                case 100:
                    serviceType = StringConstants.SERVICE_TYPE_ODU4;
                    break;
                case 400:
                    serviceType = StringConstants.SERVICE_TYPE_ODUC4;
                    break;
                default:
                    LOG.warn("Invalid service-rate {}", serviceRate);
                    break;
            }
        }
        return serviceType;
    }
}
