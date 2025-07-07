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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.WavelengthDuplicationType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;

/**
 * The primary intent of this class is to adapt the Shared Risk Group (SRG) data model from
 * one revision to another, specifically from Rev181019 to rev250714.
 */
public class SrgRev181019Adapter implements Rev250905 {

    private final Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups;

    public SrgRev181019Adapter(Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups) {
        this.sharedRiskGroups = sharedRiskGroups;
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

        for (Map.Entry<SharedRiskGroupKey, SharedRiskGroup> entries : sharedRiskGroups.entrySet()) {
            SharedRiskGroup srgRev181019 = entries.getValue();

            SharedRiskGroupBuilder rev250905SharedRiskGroupBuilder = new SharedRiskGroupBuilder();
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping
                    .rev250905.shared.risk.group.SharedRiskGroup rev250905Srg = rev250905SharedRiskGroupBuilder
                    .setSrgNumber(srgRev181019.getSrgNumber())
                    .setWavelengthDuplication(
                            WavelengthDuplicationType.forName(srgRev181019.getWavelengthDuplication().getName())
                    ).setSrgNumber(srgRev181019.getSrgNumber())
                    .build();

            rev250905ShareRiskGroups.put(rev250905Srg.key(), rev250905Srg);
        }

        return rev250905ShareRiskGroups;
    }
}
