/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;

public class ServicehandlerTxRxCheckTest {

    @Test
    public void checkPortShouldBeFalseForNullPort() {
        Assert.assertFalse(ServicehandlerTxRxCheck.checkPort(null));
    }

    @Test
    public void checkLgxShouldBeFalseForNullLgx() {
        Assert.assertFalse(ServicehandlerTxRxCheck.checkLgx(null));
    }

    @Test
    public void checkTxOrRxInfoForNullTx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(null, null);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service TxDirection is not correctly set", result.getMessage());
    }

    @Test
    public void checkTxOrRxInfoForNullTxPort() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck
                .checkTxOrRxInfo(new TxDirectionBuilder().build(), null);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service TxDirection Port is not correctly set", result.getMessage());
    }

    @Test
    public void checkTxOrRxInfoForNullTxLgx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck
            .checkTxOrRxInfo(new TxDirectionBuilder()
                .setPort(new PortBuilder().setPortDeviceName("q")
                    .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build()).build(), null);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service TxDirection Lgx is not correctly set", result.getMessage());
    }

    @Test
    public void checkTxOrRxInfoForNullRx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck
                .checkTxOrRxInfo(new TxDirectionBuilder()
                        .setPort(new PortBuilder().setPortDeviceName("q")
                                .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                        .setLgx(new LgxBuilder().setLgxDeviceName("l")
                                .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build()).build(), null);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service RxDirection is not correctly set", result.getMessage());
    }

    @Test
    public void checkTxOrRxInfoForNullRxPort() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck
            .checkTxOrRxInfo(new TxDirectionBuilder()
                .setPort(new PortBuilder().setPortDeviceName("q")
                .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                .setLgx(new LgxBuilder().setLgxDeviceName("l")
                .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build()).build(),
            new RxDirectionBuilder().build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service RxDirection Port is not correctly set", result.getMessage());
    }

    @Test
    public void checkTxOrRxInfoForNullRxLgx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck
                .checkTxOrRxInfo(new TxDirectionBuilder()
                        .setPort(new PortBuilder().setPortDeviceName("q")
                                .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                        .setLgx(new LgxBuilder().setLgxDeviceName("l")
                                .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build()).build(),
                        new RxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("q")
                                .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build()).build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service RxDirection Lgx is not correctly set", result.getMessage());
    }

    @Test
    public void checkForServiceEndNull() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(null, ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(ServiceEndpointType.SERVICEAEND + " is not set", result.getMessage());
    }

    @Ignore
    @Test
    public void checkForServiceRateNull() {
        ComplianceCheckResult result =
            ServicehandlerTxRxCheck.check(new ServiceAEndBuilder().build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service " + ServiceEndpointType.SERVICEAEND + " rate is not set", result.getMessage());
    }

    @Test
    public void checkForServiceRateEquals0() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(
            new ServiceAEndBuilder().setServiceRate(0L).build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service " + ServiceEndpointType.SERVICEAEND + " rate is not set", result.getMessage());
    }

    @Test
    public void checkForServiceFormatNull() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(
            new ServiceAEndBuilder().setServiceRate(3L).build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service " + ServiceEndpointType.SERVICEAEND + " format is not set", result.getMessage());
    }

    @Test
    public void checkForClliEmpty() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(new ServiceAEndBuilder()
            .setServiceRate(3L).setClli("").setServiceFormat(ServiceFormat.Ethernet).build(),
            ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(
            "Service" + ServiceEndpointType.SERVICEAEND + " clli format is not set", result.getMessage());
    }

    @Test
    public void checkForFailTxOrRx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(new ServiceAEndBuilder()
            .setServiceRate(3L).setClli("cc").setServiceFormat(ServiceFormat.Ethernet).build(),
            ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals("Service TxDirection is not correctly set", result.getMessage());
    }
}
