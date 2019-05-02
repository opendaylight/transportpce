/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexImpl;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexInterface;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface121;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.renderer.utils.ServiceImplementationDataUtils;
//import org.opendaylight.transportpce.renderer.utils.TransactionUtils
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathOutput;
/*
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.connection.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.connection.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
*/
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.Nodes;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DeviceRendererServiceImplDeleteTest extends AbstractTest {

    private DeviceRendererService deviceRendererService;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private DeviceTransactionManager deviceTransactionManager;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;

    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(this.deviceTransactionManager);
        this.openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(this.deviceTransactionManager);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221);
        this.openRoadmInterfaces = Mockito.spy(this.openRoadmInterfaces);
        this.portMappingVersion22 = new PortMappingVersion221(getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        this.portMappingVersion121 = new PortMappingVersion121(getDataBroker(), this.deviceTransactionManager,
            this.openRoadmInterfaces);
        PortMapping portMapping = new PortMappingImpl(getDataBroker(), this.portMappingVersion22, this.mappingUtils,
            this.portMappingVersion121);
        FixedFlexInterface fixedFlexInterface = new FixedFlexImpl();
        OpenRoadmInterface121 openRoadmInterface121 = new OpenRoadmInterface121(portMapping,openRoadmInterfaces);
        OpenRoadmInterface221 openRoadmInterface221 = new OpenRoadmInterface221(portMapping,openRoadmInterfaces,
            fixedFlexInterface);
        this.openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(this.mappingUtils,openRoadmInterface121,
            openRoadmInterface221);

        this.crossConnectImpl121 = new CrossConnectImpl121(this.deviceTransactionManager);
        this.crossConnectImpl221 = new CrossConnectImpl221(this.deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager, this.mappingUtils,
            this.crossConnectImpl121, this.crossConnectImpl221);
        this.crossConnect = Mockito.spy(this.crossConnect);
        this.deviceRendererService = new DeviceRendererServiceImpl(this.getDataBroker(),
            this.deviceTransactionManager, this.openRoadmInterfaceFactory, this.openRoadmInterfaces,
            this.crossConnect, portMapping);
    }

    @Test
    public void testSetupServiceWhenDeviceIsNotMounted() {
        setMountPoint(null);
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs();
        ServicePathOutput servicePathOutput = deviceRendererService.deleteServicePath(servicePathInput);
        Assert.assertFalse(servicePathOutput.isSuccess());
        Assert.assertEquals("node1 is not mounted on the controller",
            servicePathOutput.getResult());
    }

    @Test
    public void testDeleteServiceSuccess() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        String [] interfaceTokens = {
            StringConstants.NETWORK_TOKEN,
            StringConstants.CLIENT_TOKEN,
            StringConstants.TTP_TOKEN,
            StringConstants.PP_TOKEN
        };

        String nodeId = "node1";
        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect)
            .deleteCrossConnect(Mockito.eq(nodeId), Mockito.anyString());
        Mockito.doNothing().when(this.openRoadmInterfaces).deleteInterface(Mockito.eq(nodeId), Mockito.anyString());

        for (String srcToken : interfaceTokens) {
            String srcTP = "src-" + srcToken;
            for (String dstToken : interfaceTokens) {
                String dstTp = "dst-" + dstToken;

                List<Nodes> nodes = new ArrayList<>();
                nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
                ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);

                ServicePathOutput servicePathOutput = deviceRendererService.deleteServicePath(servicePathInput);
                Assert.assertTrue(servicePathOutput.isSuccess());
                Assert.assertEquals("Request processed", servicePathOutput.getResult());
            }
        }
    }

//    @Test
//    public void testDeleteServiceFailure() throws OpenRoadmInterfaceException {
//        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
//        String [] interfaceTokens = {
//            StringConstants.NETWORK_TOKEN,
//            StringConstants.CLIENT_TOKEN,
//            StringConstants.TTP_TOKEN,
//            StringConstants.PP_TOKEN };
//        String nodeId = "node1";
//        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect).deleteCrossConnect(Mockito.eq(nodeId),
//            Mockito.anyString());
//        Mockito.doThrow(OpenRoadmInterfaceException.class).when(this.openRoadmInterfaces)
//            .deleteInterface(Mockito.eq(nodeId), Mockito.anyString());
//
//        for (String srcToken : interfaceTokens) {
//            String srcTP = "src-" + srcToken;
//            for (String dstToken : interfaceTokens) {
//                String dstTp = "dst-" + dstToken;
//
//                List<Nodes> nodes = new ArrayList<>();
//                nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
//                ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
//
//                ServicePathOutput servicePathOutput = deviceRendererService.deleteServicePath(servicePathInput);
//                Assert.assertFalse(servicePathOutput.isSuccess());
//                Assert.assertNotEquals("Request processed", servicePathOutput.getResult());
//            }
//        }
//    }

    @Test
    public void testDeleteServiceNulls() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));

        String nodeId = "node1";
        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect)
            .deleteCrossConnect(Mockito.eq(nodeId), Mockito.anyString());
        Mockito.doThrow(OpenRoadmInterfaceException.class).when(this.openRoadmInterfaces)
            .deleteInterface(Mockito.eq(nodeId), Mockito.anyString());

        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, null, null));
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, "src-" + StringConstants.PP_TOKEN, null));
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);

        ServicePathOutput servicePathOutput = deviceRendererService.deleteServicePath(servicePathInput);
        Assert.assertTrue(servicePathOutput.isSuccess());
        Assert.assertEquals("Request processed", servicePathOutput.getResult());
    }

    @Test
    public void testDeleteServiceFailedCrossConnect() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));

        String nodeId = "node1";

        Mockito.doReturn(Collections.emptyList()).when(this.crossConnect)
            .deleteCrossConnect(Mockito.eq(nodeId), Mockito.anyString());
        Mockito.doNothing().when(this.openRoadmInterfaces).deleteInterface(Mockito.eq(nodeId), Mockito.anyString());

        String srcTP = "src-" + StringConstants.TTP_TOKEN;
        String dstTp = "dst-" + StringConstants.PP_TOKEN;

        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTP, dstTp));
        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);

        ServicePathOutput servicePathOutput = deviceRendererService.deleteServicePath(servicePathInput);
        Assert.assertTrue(servicePathOutput.isSuccess());
        Assert.assertEquals("Request processed", servicePathOutput.getResult());
    }

//    @Test
//    public void testDeleteServiceInterfacesUsedByXc() throws OpenRoadmInterfaceException, ExecutionException,
//        InterruptedException {
//        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
//
//        String nodeId = "node1";
//
//        Mockito.doReturn(true).when(this.crossConnect).deleteCrossConnect(Mockito.eq(nodeId), Mockito.anyString());
//        Mockito.doThrow(OpenRoadmInterfaceException.class).when(this.openRoadmInterfaces)
//            .deleteInterface(Mockito.eq(nodeId), Mockito.anyString());
//
//        String srcTp = "src-" + StringConstants.PP_TOKEN;
//        String dstTp = "dst-" + StringConstants.TTP_TOKEN;
//        Long waveNumber = 20L;
//
//        String connectionNumber = dstTp + "-" + srcTp + "-" + waveNumber;
//        RoadmConnectionsBuilder roadmConnectionsBuilder = new RoadmConnectionsBuilder();
//        roadmConnectionsBuilder.setConnectionNumber(connectionNumber)
//            .withKey(new RoadmConnectionsKey(connectionNumber));
//        String interfaceName = this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaceName(srcTp, waveNumber);
//        roadmConnectionsBuilder.setSource((new SourceBuilder()).setSrcIf(interfaceName).build());
//        interfaceName = this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaceName(dstTp, waveNumber);
//        roadmConnectionsBuilder.setWavelengthNumber(20L);
//        roadmConnectionsBuilder.setDestination((new DestinationBuilder()).setDstIf(interfaceName).build());
//        InstanceIdentifier<RoadmConnections> xciid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
//            .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber));
//        TransactionUtils.writeTransaction(this.deviceTransactionManager, nodeId, LogicalDatastoreType.CONFIGURATION,
//            xciid, roadmConnectionsBuilder.build());
//
//        List<Nodes> nodes = new ArrayList<>();
//        nodes.add(ServiceImplementationDataUtils.createNode(nodeId, srcTp, dstTp));
//        ServicePathInput servicePathInput = ServiceImplementationDataUtils.buildServicePathInputs(nodes);
//
//        ServicePathOutput servicePathOutput = deviceRendererService.deleteServicePath(servicePathInput);
//        Assert.assertTrue(servicePathOutput.isSuccess());
//        Assert.assertEquals("Request processed", servicePathOutput.getResult());
//    }
}
