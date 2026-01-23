/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;

class OwnedNodeEdgePointNameTest {

    @Test
    void create() {
        OwnedNodeEdgePointName photonicMediaOtsNodeEdgePoint = onepName(
                "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX",
                NepPhotonicSublayer.PHTNC_MEDIA_OTS);

        assertEquals("ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX", photonicMediaOtsNodeEdgePoint.toString());
        assertEquals(NepPhotonicSublayer.PHTNC_MEDIA_OTS, photonicMediaOtsNodeEdgePoint.nepPhotonicSublayer());
    }

    @Test
    void testCreate() {
    }

    @Test
    void testCreate1() {
    }

    @Test
    void testToString() {
    }

    @Test
    void value() {
    }

    @Test
    void nepPhotonicSublayer() {
    }

    private @NonNull OwnedNodeEdgePointName onepName(
            String name,
            String value,
            NepPhotonicSublayer nepPhotonicSublayer) {
        return new OwnedNodeEdgePointName(name(name, value), nepPhotonicSublayer);
    }

    private Name name(String name, String value) {
        return new NameBuilder().setValueName(name).setValue(value).build();
    }
}
