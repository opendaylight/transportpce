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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;

public class ServicehandlerCompliancyCheckTest {

    public ServicehandlerCompliancyCheckTest(){
    }

    @Test
    public void checkFalseSdncRequest() {
        ComplianceCheckResult result = ServicehandlerCompliancyCheck.check("service 1",
            new SdncRequestHeaderBuilder().setRequestId("1").setRequestSystemId("1").setNotificationUrl("1")
            .setRpcAction(RpcActions.ServiceCreate).build(),
            ConnectionType.Service,RpcActions.ServiceCreate, true, false);

        Assert.assertEquals("", result.getMessage());
        Assert.assertEquals(true, result.hasPassed());
    }

    @Test
    public void constructServicehandlerCompliancyCheck() {
        ServicehandlerCompliancyCheck servicehandlerCompliancyCheck = new ServicehandlerCompliancyCheck();
        Assert.assertEquals(ServicehandlerCompliancyCheck.class, servicehandlerCompliancyCheck.getClass());
    }
}
