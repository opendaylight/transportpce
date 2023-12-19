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
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.srg.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.srg.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.WavelengthDuplicationType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacksBuilder;

public class SrgRev181019 implements Rev181019 {

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> srg(
            Map<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                    .rev231110.network.network.nodes.SharedRiskGroupKey,
                    org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                            .rev231110.network.network.nodes.SharedRiskGroup> sharedRiskGroups
    ) {
        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();

        SharedRiskGroupBuilder sharedRiskGroupBuilder = new SharedRiskGroupBuilder();

        for (Map.Entry<org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                .rev231110.network.network.nodes.SharedRiskGroupKey,
                org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                        .rev231110.network.network.nodes.SharedRiskGroup> entries : sharedRiskGroups.entrySet()) {

            org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg
                    .rev231110.network.network.nodes.SharedRiskGroup value = entries.getValue();

            Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                    .rev181019.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.org.openroadm.device
                    .rev181019.srg.CircuitPacks> circuitPacksKeyCircuitPacksMap =
                    this.circuitPacks(Objects.requireNonNull(value.getCircuitPacks()));

            SharedRiskGroup srg = sharedRiskGroupBuilder
                    .setSrgNumber(value.getSrgNumber())
                    .setWavelengthDuplication(
                            WavelengthDuplicationType.valueOf(value.getWavelengthDuplication().getName())
                    )
                    .setMaxAddDropPorts(value.getMaxAddDropPorts())
                    .setCurrentProvisionedAddDropPorts(value.getMaxAddDropPorts())
                    .setCircuitPacks(circuitPacksKeyCircuitPacksMap)
                    .build();

            sharedRiskGroupMap.put(srg.key(), srg);
        }

        return sharedRiskGroupMap;
    }

    @Override
    public Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
            .rev181019.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.org.openroadm.device
            .rev181019.srg.CircuitPacks> circuitPacks(Map<CircuitPacksKey, CircuitPacks> rev231110) {

        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev181019.srg.CircuitPacksKey, org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev181019.srg.CircuitPacks> circuitPacksKeyCircuitPacksMap = new HashMap<>();

        for (Map.Entry<CircuitPacksKey, CircuitPacks> circuitPackEntry :
                rev231110.entrySet()) {
            CircuitPacksBuilder circuitPacksBuilder = new CircuitPacksBuilder();
            CircuitPacks circuitPacks = circuitPackEntry.getValue();
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.srg.CircuitPacks rev181019 =
                    circuitPacksBuilder
                            .setIndex(circuitPacks.getIndex())
                            .setCircuitPackName(circuitPacks.getCircuitPackName().toString())
                            .build();
            circuitPacksKeyCircuitPacksMap.put(rev181019.key(), rev181019);
        }

        return circuitPacksKeyCircuitPacksMap;
    }
}