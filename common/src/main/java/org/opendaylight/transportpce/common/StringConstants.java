/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import static java.util.Map.entry;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yangtools.yang.common.Uint64;

public final class StringConstants {

    public static final String OPENROADM_DEVICE_MODEL_NAME = "org-openroadm-device";

    public static final String OPENCONFIG_XPDR_DEVICE_MODEL = "openconfig-terminal-device";

    public static final String DEFAULT_NETCONF_NODEID = "controller-config";
    public static final String OPENROADM_DEVICE_VERSION_1_2_1 = "(http://org/openroadm/device?revision=2017-02-06)org-openroadm-device";
    public static final String OPENROADM_DEVICE_VERSION_2_2_1 = "(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device";
    public static final String OPENROADM_DEVICE_VERSION_7_1 = "(http://org/openroadm/device?revision=2020-05-29)org-openroadm-device";

    public static final String OPENCONFIG_DEVICE_VERSION_1_9_0 = "(http://openconfig.net/yang/terminal-device?revision=2021-07-29)openconfig-terminal-device";

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

    /**
     * Defining string constants required for supporting Openconfig XPDRS.
     */
    public static final String CHASSIS = "CHASSIS";
    public static final String OPERATINGSYSTEM = "OPERATING_SYSTEM";
    public static final String LINECARD = "LINECARD";
    public static final String PORT = "PORT";
    public static final String TERMINALCLIENT = "TERMINAL_CLIENT";
    public static final String TERMINALLINE = "TERMINAL_LINE";
    public static final String TRANSCEIVER = "TRANSCEIVER";
    public static final String BIDIRECTIONAL = "bidirectional";
    public static final String XPDR_MCPROFILE = "XPDR-mcprofile";
    public static final String SWITCH = "switch";

    public static final Map<String, Uint64> SERVICE_TYPE_RATE = Map.of(

        StringConstants.SERVICE_TYPE_1GE, Uint64.valueOf(1),
        StringConstants.SERVICE_TYPE_10GE, Uint64.valueOf(10),
        StringConstants.SERVICE_TYPE_100GE_T, Uint64.valueOf(100),
        StringConstants.SERVICE_TYPE_100GE_S, Uint64.valueOf(100),
        StringConstants.SERVICE_TYPE_100GE_M, Uint64.valueOf(100),
        StringConstants.SERVICE_TYPE_OTU4, Uint64.valueOf(100),
        StringConstants.SERVICE_TYPE_400GE, Uint64.valueOf(400),
        StringConstants.SERVICE_TYPE_OTUC4, Uint64.valueOf(400),
        StringConstants.SERVICE_TYPE_OTUC3, Uint64.valueOf(300),
        StringConstants.SERVICE_TYPE_OTUC2, Uint64.valueOf(200));

    public static final Map<String, FiberType> FIBER_TYPES_TABLE = Map.ofEntries(
        entry("SMF", FiberType.Smf),
        entry("smf", FiberType.Smf),
        entry("Smf", FiberType.Smf),
        entry("G652", FiberType.Smf),
        entry("G.652", FiberType.Smf),
        entry("G-652", FiberType.Smf),
        entry("Standard", FiberType.Smf),
        entry("G.653", FiberType.Dsf),
        entry("G653", FiberType.Dsf),
        entry("G-653", FiberType.Dsf),
        entry("dsf", FiberType.Dsf),
        entry("Dsf", FiberType.Dsf),
        entry("DSF", FiberType.Dsf),
        entry("G655", FiberType.NzDsf),
        entry("G.655", FiberType.NzDsf),
        entry("G-655", FiberType.NzDsf),
        entry("ELEAF", FiberType.Eleaf),
        entry("Eleaf", FiberType.Eleaf),
        entry("eleaf", FiberType.Eleaf),
        entry("Oleaf", FiberType.Oleaf),
        entry("oLeaf", FiberType.Oleaf),
        entry("OLEAF", FiberType.Oleaf),
        entry("TW", FiberType.Truewave),
        entry("tw", FiberType.Truewave),
        entry("TrueWave", FiberType.Truewave),
        entry("Truewave", FiberType.Truewave),
        entry("truewave", FiberType.Truewave),
        entry("TrueWaveClassic", FiberType.Truewavec),
        entry("Truewaveclassic", FiberType.Truewavec),
        entry("truewaveclassic", FiberType.Truewavec),
        entry("twc", FiberType.Truewavec),
        entry("TWC", FiberType.Truewavec),
        entry("G.654", FiberType.Ull),
        entry("G654", FiberType.Ull),
        entry("G-654", FiberType.Ull),
        entry("ull", FiberType.Ull),
        entry("Ull", FiberType.Ull));

    private StringConstants() {
        // hiding the default constructor
    }

}
