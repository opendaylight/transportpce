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
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl22;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion22;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl22;
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
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl22 openRoadmInterfacesImpl22;
    private PortMappingVersion22 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl22 crossConnectImpl22;

    @Before
    public void setUp() {
        this.mountPoint = new MountPointStub(this.getDataBroker());
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.mappingUtils = Mockito.spy(this.mappingUtils);
        this.crossConnectImpl121 = new CrossConnectImpl121(this.deviceTransactionManager);
        this.crossConnectImpl22 = new CrossConnectImpl22(this.deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager, this.mappingUtils,
                this.crossConnectImpl121, this.crossConnectImpl22);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(this.deviceTransactionManager);
        this.openRoadmInterfacesImpl22 = new OpenRoadmInterfacesImpl22(this.deviceTransactionManager);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
                openRoadmInterfacesImpl121, openRoadmInterfacesImpl22);
        this.portMappingVersion22 =
                new PortMappingVersion22(getDataBroker(), this.deviceTransactionManager, this.openRoadmInterfaces);
        this.portMappingVersion121 =
                new PortMappingVersion121(getDataBroker(), this.deviceTransactionManager, this.openRoadmInterfaces);
        this.portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22, this.mappingUtils,
                this.portMappingVersion121);
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
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(this.mappingUtils)
                .getOpenRoadmVersion("node 1");
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
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(this.mappingUtils)
                .getOpenRoadmVersion("node 1");
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
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(this.mappingUtils)
                .getOpenRoadmVersion("node 1");
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
