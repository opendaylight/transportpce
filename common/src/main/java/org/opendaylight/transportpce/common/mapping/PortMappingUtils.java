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
import java.util.Base64;
import org.opendaylight.transportpce.common.StringConstants;

public final class PortMappingUtils {

    //FNV1 64 bit hash constants
    private static final BigInteger FNV_PRIME = new BigInteger("100000001b3", 16);
    private static final BigInteger FNV_INIT = new BigInteger("cbf29ce484222325", 16);
    private static final BigInteger FNV_MOD = new BigInteger("2").pow(64);

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
        return new StringBuilder("DEG")
                .append(cpIndex)
                .append("-")
                .append(StringConstants.TTP_TOKEN)
                .append("-")
                .append(direction)
                .toString();
    }

    private PortMappingUtils() {
        //Noop - should not be called
    }
}
