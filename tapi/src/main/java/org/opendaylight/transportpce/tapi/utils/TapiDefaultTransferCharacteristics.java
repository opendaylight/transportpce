/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import java.util.Set;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;

/**
 * The Cost/Latency/Risk transfer characteristics every NRG/IRG/Node/Link builder across the tapi
 * module sets. These are fixed, TAPI-schema-mandated fields populated with constant placeholder
 * values rather than anything derived from device data.
 *
 * @param cost cost characteristic
 * @param latency latency characteristic
 * @param risk risk characteristic
 */
public record TapiDefaultTransferCharacteristics(
        CostCharacteristic cost, LatencyCharacteristic latency, RiskCharacteristic risk) {

    private static final TapiDefaultTransferCharacteristics DEFAULT = build();

    /**
     * Returns the shared instance: these values never vary, so every call site reuses the same one
     * instead of rebuilding the same three immutable objects.
     */
    public static TapiDefaultTransferCharacteristics create() {
        return DEFAULT;
    }

    private static TapiDefaultTransferCharacteristics build() {
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiConstants.COST_HOP_VALUE)
                .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
                .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                .build();
        return new TapiDefaultTransferCharacteristics(costCharacteristic, latencyCharacteristic, riskCharacteristic);
    }
}
