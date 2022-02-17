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
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yangtools.yang.common.Uint32;

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
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(
                null,
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.TXDIR_NOT_SET, result.getMessage());
    }

    @Ignore
    @Test
    public void checkTxOrRxInfoForNullTxPort() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(
                new TxDirectionBuilder()
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build(),
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.TXDIR_PORT_NOT_SET, result.getMessage());
    }

    @Ignore
    @Test
    public void checkTxOrRxInfoForNullTxLgx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build()).build(),
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.TXDIR_LGX_NOT_SET, result.getMessage());
    }

    @Test
    public void checkTxOrRxInfoForNullRx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build(),
                null);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.RXDIR_NOT_SET, result.getMessage());
    }

    @Ignore
    @Test
    public void checkTxOrRxInfoForNullRxPort() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build(),
                new RxDirectionBuilder()
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.RXDIR_PORT_NOT_SET, result.getMessage());
    }

    @Ignore
    @Test
    public void checkTxOrRxInfoForNullRxLgx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.checkTxOrRxInfo(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("l")
                            .setLgxPortName("p").setLgxPortRack("r").setLgxPortShelf("s").build())
                    .build(),
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("q")
                            .setPortName("n").setPortRack("r").setPortShelf("s").setPortType("t").build())
                    .build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.RXDIR_LGX_NOT_SET, result.getMessage());
    }

    @Test
    public void checkForServiceEndNull() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(null, ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.endpointTypeNotSet(ServiceEndpointType.SERVICEAEND), result.getMessage());
    }

    @Ignore
    @Test
    public void checkForServiceRateNull() {
        ComplianceCheckResult result =
            ServicehandlerTxRxCheck.check(new ServiceAEndBuilder().build(), ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.rateNotSet(ServiceEndpointType.SERVICEAEND), result.getMessage());
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

    @Test
    public void checkForFailTxOrRx() {
        ComplianceCheckResult result = ServicehandlerTxRxCheck.check(new ServiceAEndBuilder()
            .setServiceRate(Uint32.valueOf(3)).setClli("cc").setServiceFormat(ServiceFormat.Ethernet).build(),
            ServiceEndpointType.SERVICEAEND);

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(LogMessages.TXDIR_NOT_SET, result.getMessage());
    }
}
