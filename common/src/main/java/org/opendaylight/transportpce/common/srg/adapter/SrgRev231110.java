/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.Srg;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.srg.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacksKey;

public class SrgRev231110 implements Rev231110 {

    @Override
    public Map<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
            .rev231110.network.network.nodes.SharedRiskGroupKey,
            org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                    .rev231110.network.network.nodes.SharedRiskGroup> srg(
            Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups
    ) {

        Map<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                .rev231110.network.network.nodes.SharedRiskGroupKey,
                org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                        .rev231110.network.network.nodes.SharedRiskGroup> rev231110ShareRiskGroups = new HashMap<>();

        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> entries : sharedRiskGroups.entrySet()) {

            SharedRiskGroupBuilder sharedRiskGroupBuilder = new SharedRiskGroupBuilder();

            SharedRiskGroup srg = entries.getValue();

            Map<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                    .rev231110.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                    .rev231110.srg.CircuitPacks> rev231110CircuitPacks = new HashMap<>();

            CircuitPacksBuilder circuitPacksBuilder = new CircuitPacksBuilder();
            for (Map.Entry<CircuitPacksKey, CircuitPacks> circuitPackEntry : Objects.requireNonNull(
                    srg.getCircuitPacks()
            ).entrySet()
            ) {

                CircuitPacks circuitPack = circuitPackEntry.getValue();

                org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                        .rev231110.srg.CircuitPacks rev231110CircuitPack = circuitPacksBuilder
                        .setCircuitPackName(circuitPack.getCircuitPackName())
                        .setIndex(circuitPack.getIndex())
                        .build();

                rev231110CircuitPacks.put(rev231110CircuitPack.key(), rev231110CircuitPack);
            }

            org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                    .rev231110.network.network.nodes.SharedRiskGroup rev231110Srg = sharedRiskGroupBuilder
                    .setSrgNumber(srg.getSrgNumber())
                    .setWavelengthDuplication(
                            Srg.WavelengthDuplication.forValue(srg.getWavelengthDuplication().getIntValue())
                    )
                    .setSrgNumber(srg.getSrgNumber())
                    .setMaxAddDropPorts(srg.getMaxAddDropPorts())
                    .setCircuitPacks(rev231110CircuitPacks)
                    .build();

            rev231110ShareRiskGroups.put(rev231110Srg.key(), rev231110Srg);
        }

        return rev231110ShareRiskGroups;
    }

}