/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.olm.stub.MountPointServiceStub;
import org.opendaylight.transportpce.olm.stub.MountPointStub;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;

public class PowerMgmtTest extends AbstractTest {

    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;

    @Before
    public void setUp() {
        this.mountPoint = new MountPointStub(this.getDataBroker());
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl((this.deviceTransactionManager));
        this.portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        this.powerMgmt = new PowerMgmt(this.getDataBroker(), this.openRoadmInterfaces, this.crossConnect,
            this.deviceTransactionManager);
    }

    @Test
    public void testSetPower() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPower2() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPower3() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPower4() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    @Test
    public void testSetPowerPresentNodes() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes2() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction2(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes3() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes31() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }

    @Test
    public void testSetPowerPresentNodes312() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes32() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes4() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "network");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes41() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }

    @Test
    public void testSetPowerPresentNodes42() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes422() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    @Test
    public void testSetPowerPresentNodes43() throws InterruptedException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

}
