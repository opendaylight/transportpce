/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.equipment.EquipmentBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.service.applicability.g.ServiceApplicabilityBuilder;

/**
 * Utility Class to Build Hard Constraints and Soft Constraints.
 *
 * @author Ahmed Helmy ( ahmad.helmy@orange.com )
 *
 */
public final class ConstraintsUtils {

    private ConstraintsUtils() {

    }

    public static SoftConstraints buildSoftConstraintWithCoRouting() {
        ServiceIdentifierList sil = new ServiceIdentifierListBuilder()
            .setServiceIdentifier("service-id-soft")
            .setServiceApplicability(new ServiceApplicabilityBuilder()
                .setEquipment(new EquipmentBuilder()
                    .setRoadmSrg(true)
                    .build())
                .build())
            .build();
        return new SoftConstraintsBuilder()
            .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
            .setCoRouting(new CoRoutingBuilder()
                .setServiceIdentifierList(Map.of(sil.key(), sil))
                .build())
            .build();
    }

    public static HardConstraints buildHardConstraintWithCoRouting() {
        ServiceIdentifierList sil = new ServiceIdentifierListBuilder()
            .setServiceIdentifier("service-id-hard")
            .setServiceApplicability(new ServiceApplicabilityBuilder()
                .setEquipment(new EquipmentBuilder()
                    .setRoadmSrg(true)
                    .build())
                .build())
            .build();
        return new HardConstraintsBuilder()
            .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
            .setCoRouting(new CoRoutingBuilder()
                .setServiceIdentifierList(Map.of(sil.key(), sil))
                .build())
            .build();
    }
}
