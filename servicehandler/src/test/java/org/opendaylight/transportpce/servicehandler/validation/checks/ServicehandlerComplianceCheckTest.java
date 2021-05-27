/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import static org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck.LogMessages;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;

public class ServicehandlerComplianceCheckTest {

    public ServicehandlerComplianceCheckTest() {
    }

    @Test
    public void checkFalseSdncRequest() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
            new SdncRequestHeaderBuilder().setRequestId("1").setRequestSystemId("1").setNotificationUrl("1")
            .setRpcAction(RpcActions.ServiceCreate).build(),
            ConnectionType.Service,RpcActions.ServiceCreate, false, false);

        Assert.assertEquals("", result.getMessage());
        Assert.assertTrue(result.hasPassed());
    }

    @Test
    public void checkServiceNameNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check(null, null,
                ConnectionType.Service, null, false, false);

        Assert.assertEquals(LogMessages.SERVICENAME_NOT_SET, result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkConTypeFalseAndNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1", null,
                null, null, true, false);
        Assert.assertEquals(LogMessages.CONNECTIONTYPE_NOT_SET, result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkSdncRequestHeaderNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1", null,
                ConnectionType.Service, null, true, true);

        Assert.assertEquals(LogMessages.HEADER_NOT_SET, result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkRequestIdEmptyString() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("")
                        .setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service, RpcActions.ServiceCreate, true, true);

        Assert.assertEquals(LogMessages.REQUESTID_NOT_SET, result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkDifferentAction() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1")
                        .setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service, RpcActions.NetworkReOptimization, true, true);

        Assert.assertEquals(
                LogMessages.rpcactionsDiffers(RpcActions.ServiceCreate, RpcActions.NetworkReOptimization),
                result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }

    @Test
    public void checkServiceActionNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1").build(),
                ConnectionType.Service, RpcActions.NetworkReOptimization, true, true);

        Assert.assertEquals(LogMessages.RPCACTION_NOT_SET, result.getMessage());
        Assert.assertFalse(result.hasPassed());
    }
}
