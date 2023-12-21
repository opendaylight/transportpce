/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl710;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.utils.CreateOtsOmsDataUtils;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.stub.MountPointServiceStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DeviceRendererServiceImplCreateOtsOmsTest extends AbstractTest {

    private DeviceRendererService deviceRendererService;
    private CrossConnect crossConnect;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private DeviceTransactionManager deviceTransactionManager;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;
    private CrossConnectImpl121 crossConnectImpl121;
    private CrossConnectImpl221 crossConnectImpl221;
    private CrossConnectImpl710 crossConnectImpl710;
    private final PortMapping portMapping = mock(PortMapping.class);

    private void setMountPoint(MountPoint mountPoint) {
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.mappingUtils = new MappingUtilsImpl(getDataBroker());
        this.mappingUtils = spy(MappingUtils.class);

        doReturn(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1).when(mappingUtils).getOpenRoadmVersion(anyString());
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
        this.openRoadmInterfaces = spy(this.openRoadmInterfaces);
        this.openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(this.mappingUtils, portMapping,
                openRoadmInterfaces);

        this.crossConnectImpl121 = new CrossConnectImpl121(this.deviceTransactionManager);
        this.crossConnectImpl221 = new CrossConnectImpl221(this.deviceTransactionManager);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager, this.mappingUtils,
            this.crossConnectImpl121, this.crossConnectImpl221, this.crossConnectImpl710);
        this.crossConnect = spy(this.crossConnect);
        this.deviceRendererService = new DeviceRendererServiceImpl(getDataBroker(), this.deviceTransactionManager,
                this.openRoadmInterfaces, this.crossConnect, mappingUtils, portMapping);
    }

    @Test
    void testCreateOtsOmsWhenDeviceIsNotMounted() throws OpenRoadmInterfaceException {
        setMountPoint(null);
        CreateOtsOmsInput input = CreateOtsOmsDataUtils.buildCreateOtsOms();
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        assertFalse(result.getSuccess());
        assertEquals("node 1 is not mounted on the controller", result.getResult());
    }

    @Test
    void testCreateOtsOmsWhenDeviceIsMountedWithNoMapping() throws OpenRoadmInterfaceException {
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        CreateOtsOmsInput input = CreateOtsOmsDataUtils.buildCreateOtsOms();
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        assertFalse(result.getSuccess());
    }

    @Test
    void testCreateOtsOmsWhenDeviceIsMountedWithMapping()
            throws OpenRoadmInterfaceException, InterruptedException, ExecutionException {
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node 1")).child(NodeInfo.class).build();
        InstanceIdentifier<Nodes> nodeIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node 1")).build();
        final NodeInfo nodeInfo = new NodeInfoBuilder().setOpenroadmVersion(OpenroadmNodeVersion._221).build();
        Nodes nodes = new NodesBuilder().setNodeId("node 1").setNodeInfo(nodeInfo).build();
        WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID, nodes);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo);
        wr.commit().get();
        setMountPoint(MountPointUtils.getMountPoint(new ArrayList<>(), getDataBroker()));
        CreateOtsOmsInput input = CreateOtsOmsDataUtils.buildCreateOtsOms();
        Mapping mapping = MountPointUtils.createMapping(input.getNodeId(), input.getLogicalConnectionPoint());
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        CreateOtsOmsOutput result = this.deviceRendererService.createOtsOms(input);
        assertTrue(result.getSuccess());
    }
}