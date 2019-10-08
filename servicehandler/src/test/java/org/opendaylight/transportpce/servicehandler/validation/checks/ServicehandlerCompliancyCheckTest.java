/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;

public class ServicehandlerCompliancyCheckTest {

    public ServicehandlerCompliancyCheckTest(){
    }

    @Test
    public void checkFalseSdncRequest() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1",
            new SdncRequestHeaderBuilder().setRequestId("1").setRequestSystemId("1").setNotificationUrl("1")
            .setRpcAction(RpcActions.ServiceCreate).build(),
            ConnectionType.Service,RpcActions.ServiceCreate, false, false);

        Assert.assertEquals("", result.getMessage());
        Assert.assertTrue(result.hasPassed());
    }

    @Test
    public void checkServiceNameNull() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check(null, null,
                ConnectionType.Service,null, false, false);

        Assert.assertEquals("Service Name (common-id for Temp service) is not set", result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkConTypeFalseAndNull() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1", null,
                null,null, true, false);
        Assert.assertEquals("Service ConnectionType is not set", result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkSdncRequestHeaderNull() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1", null,
                ConnectionType.Service,null, true, true);

        Assert.assertEquals("Service sndc-request-header is not set ", result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkRequestIdEmptyString() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("")
                        .setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service, RpcActions.ServiceCreate, true, true);

        Assert.assertEquals("Service sdncRequestHeader 'request-id' is not set", result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkDifferentAction() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1")
                        .setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service, RpcActions.NetworkReOptimization, true, true);

        Assert.assertEquals("Service sdncRequestHeader rpc-action '" + RpcActions.ServiceCreate.name()
                + "' not equal to '"
                + RpcActions.NetworkReOptimization.name() + "'", result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkServiceActionNull() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1").build(),
                ConnectionType.Service, RpcActions.NetworkReOptimization, true, true);

        Assert.assertEquals("Service sndc-request-header 'rpc-action' is not set ", result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }
}
