/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import org.opendaylight.transportpce.common.StringConstants;

public final class PortMappingUtils {

    //FNV1 64 bit hash constants
    private static final BigInteger FNV_PRIME = new BigInteger("100000001b3", 16);
    private static final BigInteger FNV_INIT = new BigInteger("cbf29ce484222325", 16);
    private static final BigInteger FNV_MOD = new BigInteger("2").pow(64);


    public static final String ASSOCIATED_LCP_LOGMSG =
        "{} : port {} on {} - associated Logical Connection Point is {}";
    public static final String CANNOT_AS_LCP_LOGMSG =
        " - cannot assign Logical Connection Point";
    public static final String CANNOT_CREATE_LCP_LOGMSG =
        "{} : port {} on {} - Impossible to create logical connection point";
    public static final String CANNOT_GET_DEV_CONF_LOGMSG =
        "{} : Impossible to get device configuration";
    public static final String CANNOT_GET_LLDP_CONF_LOGMSG =
        "- cannot find port config under LLDP";
    public static final String CREATE_MAPPING_DATA_LOGMSG =
        "{} : OpenROADM version {} node - Creating Mapping Data";
    public static final String DEVICE_HAS_LOGMSG =
        "{} : Device has {} {}";
    public static final String FETCH_CONNECTIONPORT_LOGMSG =
        "{} : Fetching connection-port {} at circuit pack {}";
    public static final String GETTING_ETH_LIST_LOGMSG =
        "{} : getting Ethernet interfaces list";
    public static final String GETTING_MC_CAPA_LOGMSG =
        "{} : Getting the MC capabilities for {}";
    public static final String MISSING_CP_LOGMSG =
        "{} : No Circuit-pack {} in the device";
    public static final String NO_ASSOC_FOUND_LOGMSG =
        "{} : port {} on {} - no association found {}";
    public static final String NO_MC_LOGMSG =
        "{} : No MC profile found on {} {} - assuming fixed grid";
    public static final String NO_PORT_ON_CP_LOGMSG =
        "{} : No port {} on circuit pack {}";
    public static final String NO_VALID_PARTNERPORT_UNIDIR_LOGMSG =
        "{} : port {} on {} is unidirectional but has no valid partnerPort";
    public static final String NOT_CORRECT_PARTNERPORT_LOGMSG =
        "{} : port {} on {} is not a correct partner port of {} on {}";
    public static final String PARTNERPORT_CONF_ERROR_LOGMSG =
        " - Error with partner port configuration";
    public static final String PARTNERPORT_GET_ERROR_LOGMSG =
        " - Error getting partner port";
    public static final String PORTDIR_ERROR_LOGMSG =
        " - Error in configuration with port-direction";
    public static final String PORTMAPPING_IGNORE_LOGMSG =
        " - ignoring it in port-mapping";
    public static final String PORTQUAL_ERROR_LOGMSG =
        " - Error in configuration with port-qual";
    public static final String PROCESSING_DONE_LOGMSG =
        "{} : Processing done {}";
    public static final String UNABLE_MAPPING_LOGMSG =
        "{} : Unable to {} mapping for {}";
    public static final String UNABLE_MC_CAPA_LOGMSG =
        "{} : Unable to create MC capabilities";
    public static final String UNSUPPORTED_DIR_LOGMSG =
        "{} : port {} on {} - unsupported Direction {}";


    /**
     * Implements the FNV-1 64bit algorithm.
     * FNV-1 128bit would be ideal for 16 bytes but we need an overhead for Base64 encoding.
     * Otherwise, the hash cannot be stored in a UTF-8 string.
     * https://www.wikiwand.com/en/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function#/FNV-1_hash
     * https://github.com/pmdamora/fnv-cracker-app/blob/master/src/main/java/passwordcrack/cracking/HashChecker.java
     * @param stringdata the String to be hashed
     * @return the hash string
     */
    protected static String fnv1size64(String stringdata) {
        BigInteger hash = FNV_INIT;
        byte[] data = stringdata.getBytes(StandardCharsets.UTF_8);

        for (byte b : data) {
            hash = hash.multiply(FNV_PRIME).mod(FNV_MOD);
            hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
        }

        return Base64.getEncoder().encodeToString(hash.toByteArray());
    }

    protected static String degreeTtpNodeName(String cpIndex, String direction) {
        ArrayList<String> array = new ArrayList<>();
        array.add("DEG" + cpIndex);
        array.add(StringConstants.TTP_TOKEN);
        if (direction != null) {
            array.add(direction);
        }
        return String.join("-", array);
    }

    protected static String createXpdrLogicalConnectionPort(int xponderNb, int lcpNb, String token) {
        return new StringBuilder("XPDR").append(xponderNb)
                .append("-")
                .append(token).append(lcpNb)
                .toString();
    }

    private PortMappingUtils() {
        //Noop - should not be called
    }
}
