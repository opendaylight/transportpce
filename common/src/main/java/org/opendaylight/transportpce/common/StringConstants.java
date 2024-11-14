/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import java.util.Map;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class StringConstants {

    public static final String OPENROADM_DEVICE_MODEL_NAME = "org-openroadm-device";

    public static final String DEFAULT_NETCONF_NODEID = "controller-config";
    public static final String OPENROADM_DEVICE_VERSION_1_2_1 = "(http://org/openroadm/device?revision=2017-02-06)org-openroadm-device";
    public static final String OPENROADM_DEVICE_VERSION_2_2_1 = "(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device";
    public static final String OPENROADM_DEVICE_VERSION_7_1 = "(http://org/openroadm/device?revision=2020-05-29)org-openroadm-device";

    public static final String NETWORK_TOKEN = "NETWORK";

    public static final String TTP_TOKEN = "TTP";

    public static final String CLIENT_TOKEN = "CLIENT";

    public static final String PP_TOKEN = "PP";

    public static final String SERVICE_TYPE_100GE_T = "100GEt";
    public static final String SERVICE_TYPE_100GE_M = "100GEm";
    public static final String SERVICE_TYPE_100GE_S = "100GEs";

    public static final String SERVICE_TYPE_OTU4 = "OTU4";
    public static final String SERVICE_TYPE_OTUC2 = "OTUC2";
    public static final String SERVICE_TYPE_OTUC3 = "OTUC3";
    public static final String SERVICE_TYPE_OTUC4 = "OTUC4";

    public static final String SERVICE_TYPE_400GE = "400GE";

    public static final String SERVICE_TYPE_10GE = "10GE";

    public static final String SERVICE_TYPE_1GE = "1GE";

    public static final String SERVICE_TYPE_ODU4 = "ODU4";
    public static final String SERVICE_TYPE_ODUC2 = "ODUC2";
    public static final String SERVICE_TYPE_ODUC3 = "ODUC3";
    public static final String SERVICE_TYPE_ODUC4 = "ODUC4";

    public static final String SERVICE_TYPE_OTHER = "other";

    public static final String SERVICE_DIRECTION_AZ = "aToz";
    public static final String SERVICE_DIRECTION_ZA = "zToa";
    public static final String UNKNOWN_MODE = "Unknown Mode";

    public static final Map<String, Uint32> SERVICE_TYPE_RATE = Map.of(
        SERVICE_TYPE_100GE_T, ServiceRateConstant.RATE_100,
        SERVICE_TYPE_OTU4, ServiceRateConstant.RATE_100,
        SERVICE_TYPE_ODUC2, ServiceRateConstant.RATE_200,
        SERVICE_TYPE_ODUC3, ServiceRateConstant.RATE_300,
        SERVICE_TYPE_ODUC4, ServiceRateConstant.RATE_400,
        SERVICE_TYPE_400GE, ServiceRateConstant.RATE_400);


    private StringConstants() {
        // hiding the default constructor
    }

}
