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
    public static final String T0_MULTILAYER = "T0 - Multi-layer topology";
    public static final String T0_FULL_MULTILAYER = "T0 - Full Multi-layer topology";
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
