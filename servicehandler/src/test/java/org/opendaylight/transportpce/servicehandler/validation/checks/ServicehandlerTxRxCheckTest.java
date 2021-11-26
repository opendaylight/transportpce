/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import static org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerTxRxCheck.LogMessages;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ServicehandlerTxRxCheckTest {

    @Test
    public void checkForServiceEndNull() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(null, ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.endpointTypeNotSet(ServiceEndpointType.SERVICEAEND), result.getMessage());
    }

    @Test
    public void checkForServiceRateNull() {
        ComplianceCheckResult result =
            ServicehandlerTxRxCheck.check(new ServiceAEndBuilder().build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.rateNull(ServiceEndpointType.SERVICEAEND), result.getMessage());
    }

    @Test
    public void checkForServiceRateEquals0() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(
            new ServiceAEndBuilder().setServiceRate(Uint32.valueOf(0)).build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.rateNotSet(ServiceEndpointType.SERVICEAEND), result.getMessage());
    }

    @Test
    public void checkForServiceFormatNull() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(
            new ServiceAEndBuilder().setServiceRate(Uint32.valueOf(3)).build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.formatNotSet(ServiceEndpointType.SERVICEAEND), result.getMessage());
    }

    @Test
    public void checkForClliEmpty() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(new ServiceAEndBuilder()
            .setServiceRate(Uint32.valueOf(3)).setClli("").setServiceFormat(ServiceFormat.Ethernet).build(),
            ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.clliNotSet(ServiceEndpointType.SERVICEAEND), result.getMessage());
    }
}
