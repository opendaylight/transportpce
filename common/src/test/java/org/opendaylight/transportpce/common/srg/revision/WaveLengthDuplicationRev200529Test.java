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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.WavelengthDuplicationType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;

public class WaveLengthDuplicationRev200529Test {
    @Test
    void notNull() {
        WaveLengthDuplicationRev200529 wldRev200529 = new WaveLengthDuplicationRev200529(new java.util.HashMap<>());
        assertNotNull(wldRev200529.srg());
    }

    @Test
    void srgOnePerDegree() {
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf("1"))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerDegree)
                .build();

        WaveLengthDuplicationRev200529 wldRev200529 = new WaveLengthDuplicationRev200529(
                Map.of(sharedRiskGroup.key(), sharedRiskGroup)
        );
        Map<SharedRiskGroupKey, org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                .rev250905.shared.risk.group.SharedRiskGroup> srg = wldRev200529.srg();

        assertNotNull(srg);
        assertEquals(1, srg.size());
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                .rev250905.shared.risk.group.SharedRiskGroup firstItem = srg.values().iterator().next();
        assertEquals(Uint16.valueOf("1"), firstItem.getSrgNumber());
        assertEquals("one-per-degree", firstItem.getWavelengthDuplication().getName());
    }

    @Test
    void instantiateNotNull() {
        WaveLengthDuplication wld = WaveLengthDuplicationRev200529.instantiate(null);
        assertNotNull(wld);
        assertNotNull(wld.srg());
        assertEquals(0, wld.srg().size());
    }

    @Test
    void instantiate() {
        SharedRiskGroup sharedRiskGroup = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf("1"))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerDegree)
                .build();

        WaveLengthDuplication wld = WaveLengthDuplicationRev200529.instantiate(List.of(sharedRiskGroup));
        assertNotNull(wld);
        assertNotNull(wld.srg());
        assertEquals(1, wld.srg().size());
        assertEquals(1, wld.srg().values().iterator().next().getSrgNumber().intValue());
        assertEquals("one-per-degree", wld.srg().values().iterator().next().getWavelengthDuplication().getName());
    }
}
