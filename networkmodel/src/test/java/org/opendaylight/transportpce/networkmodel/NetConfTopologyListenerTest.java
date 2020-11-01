/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion710;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.networkmodel.stub.MountPointServiceStub;
import org.opendaylight.transportpce.networkmodel.stub.MountPointStub;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.AvailableCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.credentials.credentials.LoginPasswordBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class NetConfTopologyListenerTest {

    @Before
    public void setUp() {

    }

    @Test
    public void testOnDataTreeChanged() {

        final DataObjectModification<Node> newNode = mock(DataObjectModification.class);
        when(newNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        final Collection<DataTreeModification<Node>> changes = new HashSet<>();
        final DataTreeModification<Node> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(newNode);

        DataStoreContextImpl dataStoreContext = new DataStoreContextImpl();
        DataBroker dataBroker = dataStoreContext.getDataBroker();
        RequestProcessor requestProcessor = new RequestProcessor(dataBroker);
        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(requestProcessor);
        MountPoint mountPoint = new MountPointStub(dataBroker);
        MountPointService mountPointService = new MountPointServiceStub(mountPoint);
        DeviceTransactionManager deviceTransactionManager =
                new DeviceTransactionManagerImpl(mountPointService, 3000);
        R2RLinkDiscovery linkDiskovery = new R2RLinkDiscovery(
                dataBroker, deviceTransactionManager, networkTransactionService);
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121 =
                new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221 =
                new OpenRoadmInterfacesImpl221(deviceTransactionManager);
        OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710 =
            new OpenRoadmInterfacesImpl710(deviceTransactionManager);
        MappingUtils mappingUtils = new MappingUtilsImpl(dataBroker);
        OpenRoadmInterfacesImpl openRoadmInterfaces =
                new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils,
                openRoadmInterfacesImpl121, openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
        PortMappingVersion121 p1 = new PortMappingVersion121(dataBroker, deviceTransactionManager, openRoadmInterfaces);
        PortMappingVersion221 p2 = new PortMappingVersion221(dataBroker, deviceTransactionManager, openRoadmInterfaces);
        PortMappingVersion710 p3 = new PortMappingVersion710(dataBroker, deviceTransactionManager, openRoadmInterfaces);
        PortMapping portMapping = new PortMappingImpl(dataBroker,p3, p2, p1);
        NetworkModelService networkModelService = mock(NetworkModelService.class);

        //Start Netconf Topology listener and start adding nodes to the Netconf Topology to verify behaviour
        NetConfTopologyListener listener = new NetConfTopologyListener(networkModelService, dataBroker,
                deviceTransactionManager);

        //A new node appears in Netconf Topology, status is Connecting
        final Node netconfNode = getNetconfNode("test1",
                NetconfNodeConnectionStatus.ConnectionStatus.Connecting, OPENROADM_DEVICE_VERSION_2_2_1);
        when(newNode.getDataAfter()).thenReturn(netconfNode);
        listener.onDataTreeChanged(changes);
        verify(ch).getRootNode();
        verify(newNode, times(3)).getDataAfter();
        verify(newNode, times(2)).getModificationType();

        //A new node appears in Netconf Topology, status is Connected, version is 121
        final Node netconfNode2 = getNetconfNode("test2", NetconfNodeConnectionStatus.ConnectionStatus.Connected,
                OPENROADM_DEVICE_VERSION_1_2_1);
        when(newNode.getDataAfter()).thenReturn(netconfNode2);
        listener.onDataTreeChanged(changes);
        verify(ch, times(2)).getRootNode();
        verify(newNode, times(6)).getDataAfter();
        verify(newNode, times(4)).getModificationType();

        //A new node appears in Netconf Topology, status is Connected, version is 221
        final Node netconfNode3 = getNetconfNode("test3", NetconfNodeConnectionStatus.ConnectionStatus.Connected,
                OPENROADM_DEVICE_VERSION_2_2_1);
        when(newNode.getDataAfter()).thenReturn(netconfNode3);
        listener.onDataTreeChanged(changes);
        verify(ch, times(3)).getRootNode();
        verify(newNode, times(9)).getDataAfter();
        verify(newNode, times(6)).getModificationType();

        //A new node is deleted from Netconf Topology, Data Before was empty
        when(newNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(newNode.getDataBefore()).thenReturn(netconfNode3);
        listener.onDataTreeChanged(changes);
        verify(ch, times(4)).getRootNode();
        verify(newNode, times(10)).getDataAfter();
        verify(newNode, times(7)).getModificationType();

    }

    public Node getNetconfNode(final String nodeId, final NetconfNodeConnectionStatus.ConnectionStatus cs,
                               final String openRoadmVersion) {
        final List<AvailableCapability> avCapList = new ArrayList<>();
        avCapList.add(new AvailableCapabilityBuilder()
                .setCapabilityOrigin(AvailableCapability.CapabilityOrigin.UserDefined)
                .setCapability(openRoadmVersion)
                .build());
        final AvailableCapabilities avCaps =
                new AvailableCapabilitiesBuilder().setAvailableCapability(avCapList).build();
        final NetconfNode netconfNode = new NetconfNodeBuilder()
                .setConnectionStatus(cs)
                .setAvailableCapabilities(avCaps)
                .setHost(new Host(new IpAddress(new Ipv4Address("127.0.0.1"))))
                .setPort(new PortNumber(Uint16.valueOf(9999)))
                .setReconnectOnChangedSchema(true)
                .setDefaultRequestTimeoutMillis(Uint32.valueOf(1000))
                .setBetweenAttemptsTimeoutMillis(Uint16.valueOf(100))
                .setKeepaliveDelay(Uint32.valueOf(1000))
                .setTcpOnly(true)
                .setCredentials(new LoginPasswordBuilder()
                        .setUsername("testuser")
                        .setPassword("testpassword")
                        .build())
                .build();
        final NodeBuilder nn = new NodeBuilder().setNodeId(new NodeId(nodeId))
                .addAugmentation(netconfNode);
        return nn.build();

    }
}
