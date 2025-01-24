/*
 * Copyright © 2025 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;

public class TapiStringConstantTest {

    public static final String T0_MULTILAYER_UUID = "747c670e-7a07-3dab-b379-5b1cd17402a3";
    public static final String T0_TAPI_MULTILAYER_UUID = "a6c5aed1-dc75-333a-b3a3-b6b70534eae8";
    public static final String T0_FULL_MULTILAYER_UUID = "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7";
    public static final String SBI_TAPI_TOPOLOGY_UUID = "a21e4756-4d70-3d40-95b6-f7f630b4a13b";
    public static final String ALIEN_XPDR_TAPI_TOPOLOGY_UUID = "4aedacb6-f830-3b3d-983a-a2de06bc373b";


    @Test
    void testTopologyUUID() {
        assertEquals(
                T0_MULTILAYER_UUID,
                new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_MULTILAYER
                        .getBytes(Charset.forName("UTF-8"))).toString()).getValue());
        assertEquals(
                T0_TAPI_MULTILAYER_UUID,
                new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_TAPI_MULTILAYER
                        .getBytes(Charset.forName("UTF-8"))).toString()).getValue());
        assertEquals(
                T0_FULL_MULTILAYER_UUID,
                new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
                        .getBytes(Charset.forName("UTF-8"))).toString()).getValue());
        assertEquals(
                SBI_TAPI_TOPOLOGY_UUID,
                new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.SBI_TAPI_TOPOLOGY
                        .getBytes(Charset.forName("UTF-8"))).toString()).getValue());
        assertEquals(
                ALIEN_XPDR_TAPI_TOPOLOGY_UUID,
                new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.ALIEN_XPDR_TAPI_TOPOLOGY
                        .getBytes(Charset.forName("UTF-8"))).toString()).getValue());

    }

}