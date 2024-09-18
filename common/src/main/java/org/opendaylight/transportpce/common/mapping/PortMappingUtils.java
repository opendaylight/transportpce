/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import com.google.common.collect.ImmutableMap;
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
    public static final String CANNOT_GET_DEV_CONF_LOGMSG =
        "{} : impossible to get device configuration";
    public static final String CANNOT_GET_LLDP_CONF_LOGMSG =
        "- cannot find port config under LLDP";
    public static final String CONMAP_ISSUE_LOGMSG =
        "{} : connection-map analysis for source {} and dest (CP+port) {} - no entry found for source in LCP map";
    public static final String CREATE = "create";
    public static final String CREATE_MAPPING_DATA_LOGMSG =
        "{} : OpenROADM version {} node - Creating Mapping Data";
    public static final String DEVICE_HAS_LOGMSG =
        "{} : device has {} {}";
    public static final String FETCH_CONNECTIONPORT_LOGMSG =
        "{} : fetching connection-port {} at circuit-pack {}";
    public static final String FOUND = "found";
    public static final String GETTING_CONPORT_LOGMSG =
        "{} : getting connection-ports for degree number {}";
    public static final String GETTING_CP_LOGMSG =
        "{} : getting circuit-packs for SRG number {}";
    public static final String GETTING_ETH_LIST_LOGMSG =
        "{} : getting Ethernet interfaces list";
    public static final String GETTING_MC_CAPA_LOGMSG =
        "{} : getting MC capabilities for {}";
    public static final String GOT_INTF_LOGMSG =
        "{} : got interface {} of type {}";
    public static final String INTF_ISSUE_LOGMSG =
        "{} : error while getting interface {} ";
    public static final String MAP_LOOKS_LOGMSG =
        "{} : map looks like this {}";
    public static final String MISSING_CP_LOGMSG =
        "{} : no circuit-pack {} in the device";
    public static final String NO_ASSOC_FOUND_LOGMSG =
        "{} : port {} on {} - no association found {}";
    public static final String NO_CONMAP_LOGMSG =
        "{} : no connection-map found inside device configuration";
    public static final String NO_MC_LOGMSG =
        "{} : no MC profile found on {} {} - assuming fixed grid";
    public static final String NO_PORT_ON_CP_LOGMSG =
        "{} : no port {} on circuit pack {}";
    public static final String NO_VALID_PARTNERPORT_LOGMSG =
        "{} : port {} on {} has no valid partner port";
    public static final String NODE_TYPE_LOGMSG =
        "{} : node-type {}";
    public static final String NOT_CORRECT_CONPORT_LOGMSG =
        "{} : the number of connection-port is not correct for DEG{}";
    public static final String NOT_CORRECT_PARTNERPORT_LOGMSG =
        "{} : port {} on {} is not a correct partner port of {} on {}";
    public static final String PARTNERPORT_CONF_ERROR_LOGMSG =
        " - error with partner port configuration";
    public static final String PARTNERPORT_GET_ERROR_LOGMSG =
        " - error getting partner port";
    public static final String PORT_ALREADY_HANDLED_LOGMSG =
        "{} : port {} on {} has already been handled";
    public static final String PORT_NOT_RDMEXT_LOGMSG =
        "{} : port {} on {} is not roadm-external";
    public static final String PORTDIR_ERROR_LOGMSG =
        "{} : port {} on {} - error in configuration with port-direction";
    public static final String PORTMAPPING_IGNORE_LOGMSG =
        " - ignoring it in port-mapping";
    public static final String PORTMAPPING_POST_FAIL_LOGMSG =
        "{} : port-mapping post-treatment failure for {}";
    public static final String PORTQUAL_LOGMSG =
        "{} : port {} on {} - PortQual {}";
    public static final String PROCESSING_DONE_LOGMSG =
        "{} : processing done {}";
    public static final String UNABLE_MAPPING_LOGMSG =
        "{} : unable to {} mapping for {}";
    public static final String UNABLE_MC_CAPA_LOGMSG =
        "{} : unable to create MC capabilities";
    public static final String UNSUPPORTED_DIR_LOGMSG =
        "{} : port {} on {} - unsupported Direction {}";
    public static final String UPDATE = "update";
    public static final String UPDATE_MAPPING_LOGMSG =
        "{} : Updating old mapping data {} for {} by new mapping data {}";
    public static final String XPDR_LIST_IN_CONF_LOGMSG =
        "{} : list of Xponders {} in configuration";
    public static final ImmutableMap<String, String> INTERFACE_RATE_MAP =
        ImmutableMap.<String, String>builder()
        .put("If1GE", "1")
        .put("If10GE", "10")
        .put("If40GE", "40")
        .put("If100GE", "100")
        .put("If200GE", "200")
        .put("If400GE", "400")
        .put("If1GEODU0", "1")
        .put("If10GEODU2", "10")
        .put("If10GEODU2e", "10")
        .put("If10GEoduflexgfp", "10")
        .put("If25GEoduflexcbr", "25")
        .put("If40GEODU3", "40")
        .put("If100GEODU4", "100")
        .put("If100GEoduflexgfp", "100")
        .put("If200GEoduflexcbr", "200")
        .put("If400GEodufleximp", "400")
        .put("If400GEoduflexcbr", "400")
        .put("IfOCHOTU1ODU1", "1")
        .put("IfOCHOTU2ODU2", "10")
        .put("IfOCHOTU2EODU2E", "10")
        .put("IfOCHOTU3ODU3", "40")
        .put("IfOCHOTU4ODU4", "100")
        .put("IfOCHOTU4ODU4uniregen", "100")
        .put("IfOCHOTU4ODU4regen", "100")
        .put("IfOCHOTUCnODUCn", "400")
        .put("IfOCHOTUCnODUCnregen", "400")
        .put("IfOCHOTUCnODUCnuniregen", "400")
        .put("Ifotsiotucnoducn", "400")
        .put("IfnOTU4ODU4", "100")
        .put("IfOTU1ODU1", "1")
        .put("IfOTU2ODU2", "10")
        .put("IfOTU2eODU2e", "10")
        .put("IfOTU3ODU3", "40")
        .put("IfOTU4ODU4", "100")
        //Add default 100G to ifOCH that was created at the beginning of OR when no other line rate was available
        .put("IfOCH", "100")
        .build();

        //new HashMap<>(Map.of(IfOTU4ODU4.VALUE, "1"));

    /**
     * Implements the FNV-1 64bit algorithm.
     * FNV-1 128bit would be ideal for 16 bytes but we need an overhead for Base64 encoding.
     * Otherwise, the hash cannot be stored in a UTF-8 string.
     * https://www.wikiwand.com/en/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function#/FNV-1_hash
     * https://github.com/pmdamora/fnv-cracker-app/blob/master/src/main/java/passwordcrack/cracking/HashChecker.java
     * @param stringdata the String to be hashed
     * @return the hash string
     */
    public static String fnv1size64(String stringdata) {
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
