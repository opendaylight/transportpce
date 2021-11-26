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

//    public static HardConstraints buildHardConstraintWithGeneral() {
//        return new HardConstraintsBuilder()
//                .setCoRoutingOrGeneral(new GeneralBuilder()
//                    .setExclude(new ExcludeBuilder()
//                        .setNodeId(
//                            new ArrayList<>(Collections.singletonList(new NodeIdType("Ex-Node-Id-1"))))
//                        .setFiberBundle(
//                            new ArrayList<>(Collections.singletonList("Ex-Fiber-Bundle 1")))
//                        .setSite(
//                            new ArrayList<>(Collections.singletonList("Ex-site 1")))
//                        .setSupportingServiceName(
//                            new ArrayList<>(Collections.singletonList("Ex-supporting-Service 1")))
//                                .build())
//                    .setInclude(new IncludeBuilder()
//                        .setNodeId(
//                            new ArrayList<>(Collections.singletonList(new NodeIdType("Inc-Node-Id-1"))))
//                        .setFiberBundle(
//                            new ArrayList<>(Collections.singletonList("Inc-Fiber-Bundle 1")))
//                        .setSite(new ArrayList<>(Collections.singletonList("Inc-site 1")))
//                        .setSupportingServiceName(
//                            new ArrayList<>(Collections.singletonList("Inc-supporting-Service-name 1")))
//                        .build())
//                    .setDiversity(new DiversityBuilder()
//                        .setExistingService(
//                            new ArrayList<>(Collections.singletonList("div-existing-service 1")))
//                        .setExistingServiceApplicability(new ExistingServiceApplicabilityBuilder()
//                            .setNode(Boolean.TRUE)
//                            .setSite(Boolean.TRUE)
//                            .setSrlg(Boolean.TRUE)
//                            .build())
//                        .build())
//                    .setLatency(new LatencyBuilder().setMaxLatency(new BigDecimal(1)).build())
//                    .build())
//                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
//                .build();
//    }

//    public static HardConstraints buildHardConstraintWithNullGeneral() {
//        return new HardConstraintsBuilder()
//                .setCoRoutingOrGeneral(null)
//                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
//                .build();
//    }

//    public static SoftConstraints buildSoftConstraintWithGeneral() {
//        return new SoftConstraintsBuilder()
//                .setCoRoutingOrGeneral(new GeneralBuilder()
//                    .setExclude(new ExcludeBuilder()
//                        .setNodeId(new ArrayList<>())
//                        .setFiberBundle(new ArrayList<>())
//                        .setSite(new ArrayList<>())
//                        .setSupportingServiceName(new ArrayList<>())
//                        .build())
//                    .setInclude(new IncludeBuilder()
//                        .setNodeId(new ArrayList<>())
//                        .setFiberBundle(new ArrayList<>())
//                        .setSite(new ArrayList<>())
//                        .setSupportingServiceName(new ArrayList<>())
//                        .build())
//                    .setDiversity(new DiversityBuilder()
//                        .setExistingService(new ArrayList<>())
//                        .setExistingServiceApplicability(new ExistingServiceApplicabilityBuilder()
//                            .build())
//                        .build())
//                    .build())
//                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
//                .build();
//
//    }
}
