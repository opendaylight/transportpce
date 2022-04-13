/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation;

import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInputBuilder;


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
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service")
                            .build()))
                    .build())
                .setCustomerCode(Set.of("Some customer-code"))
                .build())
            .setSoftConstraints(new SoftConstraintsBuilder()
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service")
                            .build()))
                    .build())
                .setCustomerCode(Set.of("Some customer-code"))
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
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service")
                            .build()))
                    .build())
                .setCustomerCode(Set.of("Some customer-code"))
                .build()).setHardConstraints(null).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(true, result.isSuccess());
    }

    @Test
    public void validateServiceCreateRequestIfSoftConstraintsNull() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setSoftConstraints(null).setHardConstraints(new HardConstraintsBuilder()
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210
                            .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service")
                            .build()))
                    .build())
                .setCustomerCode(Set.of("Some customer-code"))
                .build()).build();
        OperationResult result =
                ServiceCreateValidation.validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceCreate);
        Assert.assertEquals(true, result.isSuccess());
    }
}
