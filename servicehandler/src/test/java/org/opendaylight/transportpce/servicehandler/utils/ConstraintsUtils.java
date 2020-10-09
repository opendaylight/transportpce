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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.GeneralBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.LatencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.diversity.existing.service.constraints.ExistingServiceApplicabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

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
        return new SoftConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                        .constrains.rev190329.constraints.co.routing.or.general.co.routing
                        .CoRoutingBuilder().setExistingService(
                            new ArrayList<>(Collections.singletonList("existing-service 1"))).build())
                    .build())
                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
                .build();
    }

    public static HardConstraints buildHardConstraintWithCoRouting() {
        return new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                        .constrains.rev190329.constraints.co.routing.or.general.co.routing
                        .CoRoutingBuilder().setExistingService(
                            new ArrayList<>(Collections.singletonList("existing-service 1"))).build())
                    .build())
                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
                .build();
    }

    public static HardConstraints buildHardConstraintWithGeneral() {
        return new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new GeneralBuilder()
                    .setExclude(new ExcludeBuilder()
                        .setNodeId(
                            new ArrayList<>(Collections.singletonList(new NodeIdType("Ex-Node-Id-1"))))
                        .setFiberBundle(
                            new ArrayList<>(Collections.singletonList("Ex-Fiber-Bundle 1")))
                        .setSite(
                            new ArrayList<>(Collections.singletonList("Ex-site 1")))
                        .setSupportingServiceName(
                            new ArrayList<>(Collections.singletonList("Ex-supporting-Service 1")))
                                .build())
                    .setInclude(new IncludeBuilder()
                        .setNodeId(
                            new ArrayList<>(Collections.singletonList(new NodeIdType("Inc-Node-Id-1"))))
                        .setFiberBundle(
                            new ArrayList<>(Collections.singletonList("Inc-Fiber-Bundle 1")))
                        .setSite(new ArrayList<>(Collections.singletonList("Inc-site 1")))
                        .setSupportingServiceName(
                            new ArrayList<>(Collections.singletonList("Inc-supporting-Service-name 1")))
                        .build())
                    .setDiversity(new DiversityBuilder()
                        .setExistingService(
                            new ArrayList<>(Collections.singletonList("div-existing-service 1")))
                        .setExistingServiceApplicability(new ExistingServiceApplicabilityBuilder()
                            .setNode(Boolean.TRUE)
                            .setSite(Boolean.TRUE)
                            .setSrlg(Boolean.TRUE)
                            .build())
                        .build())
                    .setLatency(new LatencyBuilder().setMaxLatency(Uint32.valueOf(1)).build())
                    .build())
                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
                .build();
    }

    public static HardConstraints buildHardConstraintWithNullGeneral() {
        return new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(null)
                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
                .build();
    }

    public static SoftConstraints buildSoftConstraintWithGeneral() {
        return new SoftConstraintsBuilder()
                .setCoRoutingOrGeneral(new GeneralBuilder()
                    .setExclude(new ExcludeBuilder()
                        .setNodeId(new ArrayList<>())
                        .setFiberBundle(new ArrayList<>())
                        .setSite(new ArrayList<>())
                        .setSupportingServiceName(new ArrayList<>())
                        .build())
                    .setInclude(new IncludeBuilder()
                        .setNodeId(new ArrayList<>())
                        .setFiberBundle(new ArrayList<>())
                        .setSite(new ArrayList<>())
                        .setSupportingServiceName(new ArrayList<>())
                        .build())
                    .setDiversity(new DiversityBuilder()
                        .setExistingService(new ArrayList<>())
                        .setExistingServiceApplicability(new ExistingServiceApplicabilityBuilder()
                            .build())
                        .build())
                    .build())
                .setCustomerCode(new ArrayList<>(Collections.singletonList("customer-code 1")))
                .build();

    }
}
