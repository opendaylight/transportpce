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
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraintsBuilder;



public class MappingConstraintsTest {

    private MappingConstraints mappingConstraints;
    private HardConstraints hardConstraints;
    private SoftConstraints softConstraints;


    public MappingConstraintsTest() {
        this.hardConstraints = new HardConstraintsBuilder()
        .setCoRoutingOrGeneral(new CoRoutingBuilder()
        .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
            .constrains.rev161014.constraints.co.routing.or.general.co.routing
            .CoRoutingBuilder().setExistingService(
            Arrays.asList("Some existing-service")).build())
        .build())
        .setCustomerCode(Arrays.asList("Some customer-code"))
        .build();

        this.softConstraints = new SoftConstraintsBuilder()
            .setCoRoutingOrGeneral(new CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                    .constrains.rev161014.constraints.co.routing.or.general.co.routing
                    .CoRoutingBuilder().setExistingService(
                    Arrays.asList("Some existing-service")).build())
                .build())
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .build();

        this.mappingConstraints = new MappingConstraints(hardConstraints, softConstraints);
    }

    @Test
    public void serviceToServicePathConstarintsNullHardConstraints() {
        this.mappingConstraints = new MappingConstraints(null, softConstraints);
        this.mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(null, this.mappingConstraints.getServiceHardConstraints());
        Assert.assertEquals(null, this.mappingConstraints.getServicePathHardConstraints());
    }

    @Test
    public void serviceToServicePathConstarintsNullSoftConstraints() {
        this.mappingConstraints = new MappingConstraints(hardConstraints, null);
        this.mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(null, this.mappingConstraints.getServiceSoftConstraints());
        Assert.assertEquals(null, this.mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstarintsNullConstraints() {
        this.mappingConstraints = new MappingConstraints(hardConstraints, softConstraints);
        this.mappingConstraints.setServiceHardConstraints(null);
        this.mappingConstraints.setServiceSoftConstraints(null);
        this.mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(null, this.mappingConstraints.getServiceHardConstraints());
        Assert.assertEquals(null, this.mappingConstraints.getServicePathHardConstraints());
        Assert.assertEquals(null, this.mappingConstraints.getServiceSoftConstraints());
        Assert.assertEquals(null, this.mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstarintsNotNullConstraints() {
        this.mappingConstraints = new MappingConstraints(hardConstraints, softConstraints);
        this.mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(this.hardConstraints.getCoRoutingOrGeneral(), this.mappingConstraints
            .getServiceHardConstraints().getCoRoutingOrGeneral());
        Assert.assertEquals(softConstraints, this.mappingConstraints.getServiceSoftConstraints());
        Assert.assertEquals(null, this.mappingConstraints.getServicePathSoftConstraints());
    }

    @Test
    public void serviceToServicePathConstarintsParameterizedConstructor() {
        this.mappingConstraints = new MappingConstraints(
            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev171017.routing.constraints.sp.HardConstraintsBuilder()
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .setCoRoutingOrGeneral(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                .constraints.rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                    .constraints.rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                    .setExistingService(Arrays.asList("Some existing-service"))
                    .build())
                .build()).build(),
            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                .routing.constraints.rev171017.routing.constraints.sp.SoftConstraintsBuilder()
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .setCoRoutingOrGeneral(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                    .constraints.rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                        .constraints.rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                        .setExistingService(Arrays.asList("Some existing-service"))
                        .build())
                    .build()).build());

        this.mappingConstraints.serviceToServicePathConstarints();
        Assert.assertEquals(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
            .constraints.rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder()
            .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                .constraints.rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                .setExistingService(Arrays.asList("Some existing-service"))
                .build())
            .build(), this.mappingConstraints.getServicePathHardConstraints().getCoRoutingOrGeneral());
        Assert.assertEquals(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev171017.routing.constraints.sp.SoftConstraintsBuilder()
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .setCoRoutingOrGeneral(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                .constraints.rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing
                    .constraints.rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder()
                    .setExistingService(Arrays.asList("Some existing-service"))
                    .build())
                .build()).build(), this.mappingConstraints.getServicePathSoftConstraints());

    }

}
