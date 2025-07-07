/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.revision;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.Srg;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.srg.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacksKey;

/**
 * The primary intent of is to adapt the Shared Risk Group (SRG) data model from
 * one revision to another, specifically from Rev181019 to Rev250702.
 */
public class SrgRev181019Adapter implements Rev250702 {

    private final Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups;

    public SrgRev181019Adapter(Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups) {
        this.sharedRiskGroups = sharedRiskGroups;
    }

    @Override
    public Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
            .rev250702.network.network.nodes.SharedRiskGroupKey,
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.network.network.nodes.SharedRiskGroup> srg() {

        Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                .rev250702.network.network.nodes.SharedRiskGroupKey,
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                        .rev250702.network.network.nodes.SharedRiskGroup> rev250702ShareRiskGroups = new HashMap<>();

        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> entries : sharedRiskGroups.entrySet()) {
            SharedRiskGroup srgRev181019 = entries.getValue();
            Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.srg.CircuitPacks> rev250702CircuitPacks = new HashMap<>();

            CircuitPacksBuilder rev250702CircuitPacksBuilder = new CircuitPacksBuilder();
            for (Map.Entry<CircuitPacksKey, CircuitPacks> rev181019CircuitPackEntry : Objects.requireNonNull(
                    srgRev181019.getCircuitPacks()).entrySet()) {
                CircuitPacks rev181019CircuitPack = rev181019CircuitPackEntry.getValue();

                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                        .rev250702.srg.CircuitPacks rev250702CircuitPack = rev250702CircuitPacksBuilder
                        .setCircuitPackName(rev181019CircuitPack.getCircuitPackName())
                        .setIndex(rev181019CircuitPack.getIndex())
                        .build();

                rev250702CircuitPacks.put(rev250702CircuitPack.key(), rev250702CircuitPack);
            }

            SharedRiskGroupBuilder rev250702SharedRiskGroupBuilder = new SharedRiskGroupBuilder();
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.network.network.nodes.SharedRiskGroup rev250702Srg = rev250702SharedRiskGroupBuilder
                    .setSrgNumber(srgRev181019.getSrgNumber())
                    .setWavelengthDuplication(
                            Srg.WavelengthDuplication.forValue(srgRev181019.getWavelengthDuplication().getIntValue())
                    ).setSrgNumber(srgRev181019.getSrgNumber())
                    .setMaxAddDropPorts(srgRev181019.getMaxAddDropPorts())
                    .setCircuitPacks(rev250702CircuitPacks)
                    .build();

            rev250702ShareRiskGroups.put(rev250702Srg.key(), rev250702Srg);
        }

        return rev250702ShareRiskGroups;
    }
}
