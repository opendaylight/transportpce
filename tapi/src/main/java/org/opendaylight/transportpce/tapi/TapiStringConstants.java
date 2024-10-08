/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi;

public final class TapiStringConstants {

    public static final String TRANSITIONAL_LINK = "tapi-transitional-link";
    public static final String OMS_RDM_RDM_LINK = "tapi-rdm-rdm-link";
    public static final String OMS_XPDR_RDM_LINK = "tapi-xpdr-rdm-link";
    public static final String OTN_XPDR_XPDR_LINK = "tapi-otn-xpdr-xpdr-link";
    public static final String VALUE_NAME_OTN_XPDR_XPDR_LINK = "otn link name";
    public static final String VALUE_NAME_OTS_XPDR_RDM_LINK = "Xpdr to roadm link";
    public static final String VALUE_NAME_OMS_RDM_RDM_LINK = "OMS link name";
    public static final String VALUE_NAME_ROADM_NODE = "roadm node name";
    public static final String VALUE_NAME_XPDR_NODE = "dsr/odu node name";
    public static final String T0_MULTILAYER = "T0 - Multi-layer topology";
    public static final String T0_TAPI_MULTILAYER = "T0 - Tapi-Multi-layer Abstracted topology";
    public static final String T0_FULL_MULTILAYER = "T0 - Full Multi-layer topology";
    public static final String SBI_TAPI_TOPOLOGY = "SBI - Multi-layer - TAPI topology";
    public static final String ALIEN_XPDR_TAPI_TOPOLOGY = "Alien-Xponders - TAPI topology";
    // TODO: these hardcoded UUID are here for debugging purpose.
    // They allow to have a reference somewhere of the UUID to be used when testing.
    public static final String T0_MULTILAYER_UUID = "747c670e-7a07-3dab-b379-5b1cd17402a3";
    public static final String T0_TAPI_MULTILAYER_UUID = "a6c5aed1-dc75-333a-b3a3-b6b70534eae8";
    public static final String T0_FULL_MULTILAYER_UUID = "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7";
    public static final String SBI_TAPI_TOPOLOGY_UUID = "a21e4756-4d70-3d40-95b6-f7f630b4a13b";
    public static final String TPDR_100G = "Transponder 100GE";
    public static final String DSR = "DSR";
    public static final String ODU = "ODU";
    public static final String I_ODU = "iODU";
    public static final String E_ODU = "eODU";
    public static final String OTSI = "OTSi";
    public static final String E_OTSI = "eOTSi";
    public static final String I_OTSI = "iOTSi";
    public static final String PHTNC_MEDIA = "PHOTONIC_MEDIA";
    public static final String PHTNC_MEDIA_OTS = "PHOTONIC_MEDIA_OTS";
    public static final String PHTNC_MEDIA_OMS = "PHOTONIC_MEDIA_OMS";
    public static final String MC = "MEDIA_CHANNEL";
    public static final String OTSI_MC = "OTSi_MEDIA_CHANNEL";
    public static final String RDM_INFRA = "ROADM-infra";
    public static final String CLIENT = "-CLIENT";
    public static final String NETWORK = "-NETWORK";
    public static final String XPDR = "XPONDER";
    public static final String XXPDR = "-XPDR";
    public static final String TP = "TerminationPoint";
    public static final String NODE = "Node";
    public static final String LGX_PORT_NAME = "Some lgx-port-name";
    public static final String PORT_TYPE = "some port type";
    public static final String LGX_DEVICE_NAME = "Some lgx-device-name";
    // TODO: static values until they are implemented
    public static final String PORT_RACK_VALUE = "000000.00";
    public static final String FIXED_LATENCY_VALUE = "12345678";
    public static final String COST_HOP_VALUE = "12345678";
    public static final String JITTER_VALUE = "12345678";
    public static final String WANDER_VALUE = "12345678";
    public static final String QUEING_LATENCY_VALUE = "12345678";
    public static final String TAPI_CONNECTION_UPDATE_ERROR = "Could not update TAPI connections";
    public static final String TAPI_CONNECTION_READ_ERROR = "Could not read TAPI connection data";

    private TapiStringConstants() {
        // hiding the default constructor
    }
}
