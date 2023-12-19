/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.revision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.WavelengthDuplicationType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;

public class WaveLengthDuplicationRev200529 implements WaveLengthDuplication {

    private final Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupsRev200529;

    public WaveLengthDuplicationRev200529(Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupsRev200529) {
        this.sharedRiskGroupsRev200529 = sharedRiskGroupsRev200529;
    }

    @Override
    public Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
            .rev250905.shared.risk.group.SharedRiskGroupKey,
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                    .rev250905.shared.risk.group.SharedRiskGroup> srg() {

        Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                .rev250905.shared.risk.group.SharedRiskGroupKey,
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                        .rev250905.shared.risk.group.SharedRiskGroup> rev250905ShareRiskGroups = new HashMap<>();

        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> entries : sharedRiskGroupsRev200529.entrySet()) {
            SharedRiskGroup srgRev200529 = entries.getValue();

            SharedRiskGroupBuilder rev250905SharedRiskGroupBuilder = new SharedRiskGroupBuilder();
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                    .rev250905.shared.risk.group.SharedRiskGroup rev250905Srg = rev250905SharedRiskGroupBuilder
                    .setSrgNumber(srgRev200529.getSrgNumber())
                    .setWavelengthDuplication(
                            WavelengthDuplicationType.forName(srgRev200529.getWavelengthDuplication().getName())
                    ).setSrgNumber(srgRev200529.getSrgNumber())
                    .build();

            rev250905ShareRiskGroups.put(rev250905Srg.key(), rev250905Srg);
        }

        return rev250905ShareRiskGroups;
    }

    public static WaveLengthDuplication instantiate(List<SharedRiskGroup> sharedRiskGroupsRev200529) {
        if (sharedRiskGroupsRev200529 == null) {
            return new WaveLengthDuplicationRev181019(new HashMap<>());
        }

        return new WaveLengthDuplicationRev200529(sharedRiskGroupsRev200529.stream()
                .collect(Collectors.toMap(SharedRiskGroup::key, Function.identity(), (srg1, srg2) -> srg1)));
    }
}
