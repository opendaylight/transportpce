/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.validation.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck.LogMessages;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeaderBuilder;

public class ServicehandlerComplianceCheckTest {

    @Test
    void checkFalseSdncRequest() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1").setRequestSystemId("1").setNotificationUrl("1")
                    .setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service,RpcActions.ServiceCreate, false, false);

        assertEquals("", result.getMessage());
        assertTrue(result.hasPassed());
    }

    @Test
    void checkServiceNameNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck
            .check(null, null, ConnectionType.Service, null, false, false);

        assertEquals(LogMessages.SERVICENAME_NOT_SET, result.getMessage());
        assertFalse(result.hasPassed());
    }

    @Test
    void checkConTypeFalseAndNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck
            .check("service 1", null, null, null, true, false);
        assertEquals(LogMessages.CONNECTIONTYPE_NOT_SET, result.getMessage());
        assertFalse(result.hasPassed());
    }

    @Test
    void checkSdncRequestHeaderNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck
            .check("service 1", null, ConnectionType.Service, null, true, true);

        assertEquals(LogMessages.HEADER_NOT_SET, result.getMessage());
        assertFalse(result.hasPassed());
    }

    @Test
    void checkRequestIdEmptyString() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("").setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service, RpcActions.ServiceCreate, true, true);

        assertEquals(LogMessages.REQUESTID_NOT_SET, result.getMessage());
        assertFalse(result.hasPassed());
    }

    @Test
    void checkDifferentAction() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1").setRpcAction(RpcActions.ServiceCreate).build(),
                ConnectionType.Service, RpcActions.NetworkReOptimization, true, true);

        assertEquals(
            LogMessages.rpcactionsDiffers(RpcActions.ServiceCreate, RpcActions.NetworkReOptimization),
            result.getMessage());
        assertFalse(result.hasPassed());
    }

    @Test
    void checkServiceActionNull() {
        ComplianceCheckResult result = ServicehandlerComplianceCheck.check("service 1",
                new SdncRequestHeaderBuilder().setRequestId("1").build(),
                ConnectionType.Service, RpcActions.NetworkReOptimization, true, true);

        assertEquals(LogMessages.RPCACTION_NOT_SET, result.getMessage());
        assertFalse(result.hasPassed());
    }
}
