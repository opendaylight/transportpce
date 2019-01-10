/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PowerMgmtTest extends AbstractTest {

    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;

    /*
     * initial setup before test cases
     */
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

    /*
     * test setPower function in PowerMgmt
     */
    @Test
    public void testSetPower() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    /*
     * test setPower function in PowerMgmt with different input (DestTp("network"))
     */
    @Test
    public void testSetPower2() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    /*
     * test setPower function in PowerMgmt with different input (DestTp("deg"))
     */
    @Test
    public void testSetPower3() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    /*
     * test setPower function in PowerMgmt with different input (DestTp("srg"))
     */
    @Test
    public void testSetPower4() {

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);

    }

    /*
     * test setPower function in PowerMgmt in case nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes() with null mapping key
     */
    @Test
    public void testSetPowerPresentNodes() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case of different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes2() with null mapping key
     */
    @Test
    public void testSetPowerPresentNodes2() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction2(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes3() with null mapping key
     */
    @Test
    public void testSetPowerPresentNodes3() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes3() with "deg" mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3()
     */
    @Test
    public void testSetPowerPresentNodes31() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes3() with "deg" mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4()
     */
    @Test
    public void testSetPowerPresentNodes312() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes3() with "deg" mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4()
     */
    @Test
    public void testSetPowerPresentNodes312_2() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }

        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            Interface.class, new InterfaceKey("ots"));
        Interface interface0 = new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp
            .InterfaceBuilder()
            .setName("ots")
            .build();

        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(mountPointService, 3000),
            "node 1", LogicalDatastoreType.CONFIGURATION, interfacesIID, interface0);
        Thread.sleep(5000);
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }


    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes3() with null mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3()
     */
    @Test
    public void testSetPowerPresentNodes32() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction3(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes() with "network" mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2()
     */
    @Test
    public void testSetPowerPresentNodes4() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "network");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

//    @Test
//    public void testSetPowerPresentNodes4_1() throws InterruptedException, ExecutionException {
//        List<NodeId> nodes = TransactionUtils.getNodeIds();
//        for (NodeId nodeId : nodes) {
//            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "network");
//        }
//
//        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
//            .child(CircuitPacks.class, new CircuitPacksKey("circuit name"))
//            .child(Ports.class, new PortsKey("port"));
//        Ports ports = new PortsBuilder()
//            .setPortName("port")
//            .setCircuitId("circuit name")
//            .build();
//        TransactionUtils.putAndSubmit(new DeviceTransactionManagerImpl(this.mountPointService, 3000)
//            ,"node 1", LogicalDatastoreType.OPERATIONAL, portIID, ports);
//        Thread.sleep(1000);
//        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
//        boolean output = this.powerMgmt.setPower(input);
//        Assert.assertEquals(true, output);
//    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes() with null mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2()
     */
    @Test
    public void testSetPowerPresentNodes41() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), null);
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput2();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(false, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes() with "deg" mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3()
     */
    @Test
    public void testSetPowerPresentNodes42() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes() with "deg" mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4()
     */
    @Test
    public void testSetPowerPresentNodes422() throws InterruptedException, ExecutionException {
        List<NodeId> nodes = TransactionUtils.getNodeIds();
        for (NodeId nodeId : nodes) {
            TransactionUtils.writeNodeTransaction(nodeId.getValue(), this.getDataBroker(), "deg");
            Thread.sleep(500);
        }
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInput4();
        boolean output = this.powerMgmt.setPower(input);
        Assert.assertEquals(true, output);
    }

    /*
     * test setPower function in PowerMgmt in case different nodes are present in Configuration Datastore
     * from TransactionUtils.getNodes() with null mapping key and input
     * from OlmPowerServiceRpcImplUtil.getServicePowerSetupInput3()
     */
    @Test
    public void testSetPowerPresentNodes43() throws InterruptedException, ExecutionException {
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
