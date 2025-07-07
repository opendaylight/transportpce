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
                        .rev250702.network.network.nodes.SharedRiskGroup> rev231110ShareRiskGroups = new HashMap<>();

        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> entries : sharedRiskGroups.entrySet()) {
            SharedRiskGroupBuilder sharedRiskGroupBuilder = new SharedRiskGroupBuilder();
            SharedRiskGroup srg = entries.getValue();
            Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.srg.CircuitPacks> rev231110CircuitPacks = new HashMap<>();

            CircuitPacksBuilder circuitPacksBuilder = new CircuitPacksBuilder();
            for (Map.Entry<CircuitPacksKey, CircuitPacks> circuitPackEntry : Objects.requireNonNull(
                    srg.getCircuitPacks()).entrySet()) {
                CircuitPacks circuitPack = circuitPackEntry.getValue();

                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                        .rev250702.srg.CircuitPacks rev231110CircuitPack = circuitPacksBuilder
                        .setCircuitPackName(circuitPack.getCircuitPackName())
                        .setIndex(circuitPack.getIndex())
                        .build();

                rev231110CircuitPacks.put(rev231110CircuitPack.key(), rev231110CircuitPack);
            }

            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
                    .rev250702.network.network.nodes.SharedRiskGroup rev231110Srg = sharedRiskGroupBuilder
                    .setSrgNumber(srg.getSrgNumber())
                    .setWavelengthDuplication(
                            Srg.WavelengthDuplication.forValue(srg.getWavelengthDuplication().getIntValue())
                    ).setSrgNumber(srg.getSrgNumber())
                    .setMaxAddDropPorts(srg.getMaxAddDropPorts())
                    .setCircuitPacks(rev231110CircuitPacks)
                    .build();

            rev231110ShareRiskGroups.put(rev231110Srg.key(), rev231110Srg);
        }

        return rev231110ShareRiskGroups;
    }
}
