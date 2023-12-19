/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.revision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.WavelengthDuplicationType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;


class WaveLengthDuplicationRev181019Test {

    @Test
    void notNull() {
        WaveLengthDuplicationRev181019 wldRev181019 = new WaveLengthDuplicationRev181019(new java.util.HashMap<>());
        assertNotNull(wldRev181019.srg());
    }

    @Test
    void srgOnePerDegree() {
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf("1"))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerDegree)
                .build();

        WaveLengthDuplicationRev181019 wldRev181019 = new WaveLengthDuplicationRev181019(
                Map.of(sharedRiskGroup.key(), sharedRiskGroup)
        );
        Map<SharedRiskGroupKey, org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                .rev250905.shared.risk.group.SharedRiskGroup> srg = wldRev181019.srg();

        assertNotNull(srg);
        assertEquals(1, srg.size());
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                .rev250905.shared.risk.group.SharedRiskGroup firstItem = srg.values().iterator().next();
        assertEquals(Uint16.valueOf("1"), firstItem.getSrgNumber());
        assertEquals("one-per-degree", firstItem.getWavelengthDuplication().getName());
    }
}
