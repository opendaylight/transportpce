/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.GeneralBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraintsBuilder;



public class MappingConstraintsTest {

    private HardConstraints buildHardConstraintWithCoRouting() {
        return new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                        .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                                .constrains.rev190329.constraints.co.routing.or.general.co.routing
                                .CoRoutingBuilder().setExistingService(
                                Arrays.asList("Some existing-service")).build())
                        .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build();
    }

    private HardConstraints buildHardConstraintWithGeneral() {
        return new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new GeneralBuilder().build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build();
    }

    private SoftConstraints buildSoftConstraintWithCoRouting() {
        return new SoftConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                        .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                                .constrains.rev190329.constraints.co.routing.or.general.co.routing
                                .CoRoutingBuilder().setExistingService(
                                Arrays.asList("Some existing-service")).build())
                        .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build();
    }

    @Test
    public void serviceToServicePathConstraintsNullHardConstraints() {
        MappingConstraints mappingConstraints = new MappingConstraints(null, buildSoftConstraintWithCoRouting());
        mappingConstraints.serviceToServicePathConstarints();
        Assert.assertNull(mappingConstraints.getServiceHardConstraints());
        Assert.assertNull(mappingConstraints.getServicePathHardConstraints());
    }

    @Test
    public void serviceToServicePathConstraintsNullSoftConstraints() {
        MappingConstraints mappingConstraints = new MappingConstraints(buildHardConstraintWithGeneral(), null);
        mappingConstraints.serviceToServicePathConstarints();
        Assert.assertNull(mappingConstraints.getServiceSoftConstraints());
        Assert.assertNull(mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstraintsNullSoftConstraintsGeneral() {
        MappingConstraints mappingConstraints = new MappingConstraints(buildHardConstraintWithCoRouting(), null);
        mappingConstraints.serviceToServicePathConstarints();
        Assert.assertNull(mappingConstraints.getServiceSoftConstraints());
        Assert.assertNull(mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstraintsNullConstraints() {
        MappingConstraints mappingConstraints =
            new MappingConstraints(buildHardConstraintWithCoRouting(), buildSoftConstraintWithCoRouting());
        mappingConstraints.setServiceHardConstraints(null);
        mappingConstraints.setServiceSoftConstraints(null);
        mappingConstraints.serviceToServicePathConstarints();
        Assert.assertNull(mappingConstraints.getServiceHardConstraints());
        Assert.assertNull(mappingConstraints.getServicePathHardConstraints());
        Assert.assertNull(mappingConstraints.getServiceSoftConstraints());
        Assert.assertNull(mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstraintsNotNullConstraints() {
        HardConstraints hardConstraints = buildHardConstraintWithCoRouting();
        SoftConstraints softConstraints = buildSoftConstraintWithCoRouting();
        MappingConstraints mappingConstraints = new MappingConstraints(hardConstraints, softConstraints);
        mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(hardConstraints.getCoRoutingOrGeneral(), mappingConstraints
            .getServiceHardConstraints().getCoRoutingOrGeneral());
        Assert.assertEquals(softConstraints, mappingConstraints.getServiceSoftConstraints());
        Assert.assertNull(mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstraintsParameterizedConstructor() {
        MappingConstraints mappingConstraints = new MappingConstraints(
            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev220118.routing.constraints.sp.HardConstraintsBuilder()
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .setCoRoutingOrGeneral(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                .constraints.rev220118.constraints.sp.co.routing.or.general.CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                    .constraints.rev220118.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                    .setExistingService(Arrays.asList("Some existing-service"))
                    .build())
                .build()).build(),
            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                .routing.constraints.rev220118.routing.constraints.sp.SoftConstraintsBuilder()
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .setCoRoutingOrGeneral(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                    .constraints.rev220118.constraints.sp.co.routing.or.general.CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                        .constraints.rev220118.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                        .setExistingService(Arrays.asList("Some existing-service"))
                        .build())
                    .build()).build());

        mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
            .constraints.rev220118.constraints.sp.co.routing.or.general.CoRoutingBuilder()
            .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                .constraints.rev220118.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                .setExistingService(Arrays.asList("Some existing-service"))
                .build())
            .build(), mappingConstraints.getServicePathHardConstraints().getCoRoutingOrGeneral());
        Assert.assertEquals(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev220118.routing.constraints.sp.SoftConstraintsBuilder()
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .setCoRoutingOrGeneral(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                .constraints.rev220118.constraints.sp.co.routing.or.general.CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                    .constraints.rev220118.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                    .setExistingService(Arrays.asList("Some existing-service"))
                    .build())
                .build()).build(), mappingConstraints.getServicePathSoftConstraints());
    }
}
