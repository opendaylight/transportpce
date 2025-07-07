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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.shared.risk.group.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;

/**
 * The primary intent of is to adapt the Shared Risk Group (SRG) data model from
 * one revision to another, specifically from Rev181019 to rev250714.
 */
public class SrgRev181019Adapter implements Rev250325 {

    private final Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups;

    public SrgRev181019Adapter(Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups) {
        this.sharedRiskGroups = sharedRiskGroups;
    }

    @Override
    public Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
            .rev250714.shared.risk.group.SharedRiskGroupKey,
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                    .rev250714.shared.risk.group.SharedRiskGroup> srg() {

        Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                .rev250714.shared.risk.group.SharedRiskGroupKey,
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                        .rev250714.shared.risk.group.SharedRiskGroup> rev250714ShareRiskGroups = new HashMap<>();

        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> entries : sharedRiskGroups.entrySet()) {
            SharedRiskGroup srgRev181019 = entries.getValue();

            SharedRiskGroupBuilder rev250714SharedRiskGroupBuilder = new SharedRiskGroupBuilder();
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                    .rev250714.shared.risk.group.SharedRiskGroup rev250714Srg = rev250714SharedRiskGroupBuilder
                    .setSrgNumber(srgRev181019.getSrgNumber())
                    .setWavelengthDuplication(
                            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                                    .rev250714.shared.risk.group.SharedRiskGroup.WavelengthDuplication.forValue(
                                            srgRev181019.getWavelengthDuplication().getIntValue())
                    ).setSrgNumber(srgRev181019.getSrgNumber())
                    .build();

            rev250714ShareRiskGroups.put(rev250714Srg.key(), rev250714Srg);
        }

        return rev250714ShareRiskGroups;
    }
}
