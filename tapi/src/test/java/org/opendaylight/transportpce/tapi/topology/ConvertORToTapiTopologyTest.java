/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.topology;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;

class ConvertORToTapiTopologyTest {

    @Test
    void getFreqMapFromBitSet() {
        ConvertORToTapiTopology convertORToTapiTopology = new ConvertORToTapiTopology(
                new Uuid("00000000-0000-0000-0000-000000000000")
        );

        byte[] bytes = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0};

        byte[] bytes2 = new byte[bytes.length * 8];

        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bytes2[i * 8 + j] = (byte) (((bytes[i] >> j) & 1) * -1) ;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= bytes2.length; i++) {
            byte i1 = bytes2[i - 1];
            if (i1 == 0) {
                sb.append(" ");
            }
            sb.append(i1);
            if (i % 32 == 0) {
                sb = new StringBuilder();
            }
        }

        System.out.println(sb.toString());
        //System.out.println(convertORToTapiTopology.getFreqMapFromBitSet(bytes));
        System.out.println(convertORToTapiTopology.getFreqMapFromBitSet(bytes2));
    }
}