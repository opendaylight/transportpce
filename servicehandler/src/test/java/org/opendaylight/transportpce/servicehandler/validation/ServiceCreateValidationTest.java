/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev210924.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev210924.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev210924.ServiceCreateInputBuilder;


public class ServiceCreateValidationTest {

    @Test
    public void validateServiceCreateRequestIfCommonIdNull() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setCommonId(null).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(true, result.isSuccess());
    }

    @Test
    public void validateServiceCreateRequestIfConstraintsNotNull() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setHardConstraints(new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924
                        .constraints.co.routing.or.general.co.routing.CoRoutingBuilder().setExistingService(
                        Arrays.asList("Some existing-service")).build())
                    .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build()).setSoftConstraints(new SoftConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924
                        .constraints.co.routing.or.general.co.routing.CoRoutingBuilder().setExistingService(
                        Arrays.asList("Some existing-service")).build())
                    .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build()).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(false, result.isSuccess());
    }

    @Test
    public void validateServiceCreateRequestIfConstraintsNull() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setSoftConstraints(null).setHardConstraints(null).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(true, result.isSuccess());
    }

    @Test
    public void validateServiceCreateRequestIfHardConstraintsNull() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setSoftConstraints(new SoftConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924
                        .constraints.co.routing.or.general.co.routing.CoRoutingBuilder().setExistingService(
                        Arrays.asList("Some existing-service")).build())
                    .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build()).setHardConstraints(null).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(true, result.isSuccess());
    }

    @Test
    public void validateServiceCreateRequestIfSoftConstraintsNull() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setSoftConstraints(null).setHardConstraints(new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev210924
                        .constraints.co.routing.or.general.co.routing.CoRoutingBuilder().setExistingService(
                        Arrays.asList("Some existing-service")).build())
                    .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build()).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(true, result.isSuccess());
    }
}
