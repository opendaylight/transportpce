/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
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
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.stub.MountPointStub;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.renderer.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.RendererRollbackInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.ServicePathInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.ServicePathOutput;

public class DeviceRendererServiceImplTest extends AbstractTest {

    private MountPointService mountPointService;

    private DeviceTransactionManager deviceTransactionManager;

    private DeviceRendererService deviceRendererService;

    private void setMountPoint(MountPoint mountPoint) {
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(this.mountPointService, 3000);
        OpenRoadmInterfaces openRoadmInterfaces = new OpenRoadmInterfacesImpl(this.deviceTransactionManager);
        PortMapping portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
            openRoadmInterfaces);
        OpenRoadmInterfaceFactory openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(portMapping,
            openRoadmInterfaces);
        CrossConnect crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.deviceRendererService = new DeviceRendererServiceImpl(this.getDataBroker(),
            this.deviceTransactionManager, openRoadmInterfaceFactory, openRoadmInterfaces, crossConnect);
    }

    @Test
    public void testSetupServiceWhenDeviceIsNotMounted() {
        setMountPoint(null);
        ServicePathInput servicePathInput = ServiceDataUtils.buildServicePathInputs();
        for (ServicePathDirection servicePathDirection : ServicePathDirection.values()) {
            ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                servicePathDirection);
            Assert.assertFalse(servicePathOutput.isSuccess());
            Assert.assertEquals("node1 is not mounted on the controller",
                servicePathOutput.getResult());
        }
    }

    @Test
    public void testSetupServiceUsingCrossConnectEmptyPorts() throws ExecutionException, InterruptedException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        testSetupService(true);
    }

    @Test
    public void testSetupServiceUsingCrossConnectWithPorts() throws ExecutionException, InterruptedException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        testSetupService(true);
    }

    @Test
    public void testSetupServiceWithoutCrossConnect() throws ExecutionException, InterruptedException {
        setMountPoint(new MountPointStub(getDataBroker()));

        testSetupService(false);
    }

    private void testSetupService(boolean crossConnect) throws ExecutionException, InterruptedException {
        String [] interfaceTokens = {
            OpenRoadmInterfacesImpl.NETWORK_TOKEN,
            OpenRoadmInterfacesImpl.CLIENT_TOKEN,
            OpenRoadmInterfacesImpl.TTP_TOKEN,
            OpenRoadmInterfacesImpl.PP_TOKEN,
            ""
        };

        String nodeId = "node1";

        for (String srcToken : interfaceTokens) {
            String srcTP = "src-" + srcToken;
            MountPointUtils.writeMapping(nodeId, srcTP, this.deviceTransactionManager);
            for (String dstToken : interfaceTokens) {
                String dstTp = "dst-" + dstToken;
                MountPointUtils.writeMapping(nodeId, dstTp, this.deviceTransactionManager);

                boolean connectingUsingCrossConnect = true;
                if (OpenRoadmInterfacesImpl.NETWORK_TOKEN.equals(srcToken)
                        || OpenRoadmInterfacesImpl.CLIENT_TOKEN.equals(srcToken)) {
                    connectingUsingCrossConnect = false;
                }
                if (OpenRoadmInterfacesImpl.NETWORK_TOKEN.equals(dstToken)
                        || OpenRoadmInterfacesImpl.CLIENT_TOKEN.equals(dstToken)) {
                    connectingUsingCrossConnect = false;
                }

                if (connectingUsingCrossConnect != crossConnect) {
                    continue;
                }

                List<Nodes> nodes = new ArrayList<>();
                nodes.add(ServiceDataUtils.createNode(nodeId, srcTP, dstTp));
                ServicePathInput servicePathInput = ServiceDataUtils.buildServicePathInputs(nodes);

                for (ServicePathDirection servicePathDirection : ServicePathDirection.values()) {
                    ServicePathOutput servicePathOutput = deviceRendererService.setupServicePath(servicePathInput,
                            servicePathDirection);
                    Assert.assertTrue(servicePathOutput.isSuccess());
                    String expectedResult = "Roadm-connection successfully created for nodes: ";
                    if (crossConnect) {
                        expectedResult = expectedResult + nodeId;
                    }
                    Assert.assertEquals(expectedResult, servicePathOutput.getResult());
                    Assert.assertEquals(1, servicePathOutput.getNodeInterface().size());
                    Assert.assertEquals(nodeId, servicePathOutput.getNodeInterface().get(0).getNodeId());
                }
            }
        }
    }

    @Test
    public void testRollbackEmptyInterface() {
        setMountPoint(new MountPointStub(getDataBroker()));
        RendererRollbackInput rendererRollbackInput = ServiceDataUtils.buildRendererRollbackInput();
        RendererRollbackOutput rendererRollbackOutput =
                this.deviceRendererService.rendererRollback(rendererRollbackInput);
        Assert.assertTrue(rendererRollbackOutput.isSuccess());
        Assert.assertTrue(rendererRollbackOutput.getFailedToRollback().isEmpty());
    }

    @Test
    public void testRollbackConnectionIdNotExist() {
        setMountPoint(new MountPointStub(getDataBroker()));

        NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder();
        nodeInterfaceBuilder.setNodeId("node1");
        nodeInterfaceBuilder.setKey(new NodeInterfaceKey("node1"));
        List<String> connectionID = new ArrayList<>();
        connectionID.add("node1-PP");
        nodeInterfaceBuilder.setConnectionId(connectionID);
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        nodeInterfaces.add(nodeInterfaceBuilder.build());
        RendererRollbackInputBuilder rendererRollbackInputBuilder = new RendererRollbackInputBuilder();
        rendererRollbackInputBuilder.setNodeInterface(nodeInterfaces);

        RendererRollbackOutput rendererRollbackOutput =
                this.deviceRendererService.rendererRollback(rendererRollbackInputBuilder.build());
        Assert.assertFalse(rendererRollbackOutput.isSuccess());
        Assert.assertEquals(1, rendererRollbackOutput.getFailedToRollback().size());
        Assert.assertEquals("node1", rendererRollbackOutput.getFailedToRollback().get(0).getNodeId());
    }
}
