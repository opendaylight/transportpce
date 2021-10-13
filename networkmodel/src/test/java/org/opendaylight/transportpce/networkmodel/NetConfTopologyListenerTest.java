/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class NetConfTopologyListenerTest {

    @Mock
    private NetworkModelService networkModelService;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private PortMapping portMapping;
    @Mock
    private Map<String, NodeRegistration> registrations;

    @Test
    public void testOnDataTreeChangedWhenDeleteNode() {
        @SuppressWarnings("unchecked") final DataObjectModification<Node> node = mock(DataObjectModification.class);
        final Collection<DataTreeModification<Node>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Node> ch = mock(DataTreeModification.class);
        final NodeRegistration nodeRegistration = mock(NodeRegistration.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(node);

        final Node netconfNode = getNetconfNode("netconfNode1", NetconfNodeConnectionStatus.ConnectionStatus.Connecting,
            OPENROADM_DEVICE_VERSION_2_2_1);
        when(node.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(node.getDataBefore()).thenReturn(netconfNode);

        NetConfTopologyListener listener = new NetConfTopologyListener(networkModelService, dataBroker,
            deviceTransactionManager, portMapping, registrations);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(node, times(1)).getModificationType();
        verify(node, times(3)).getDataBefore();
        verify(networkModelService, times(1)).deleteOpenRoadmnode(anyString());
        verify(nodeRegistration, times(0)).unregisterListeners();
    }

    @Test
    public void testOnDataTreeChangedWhenAddNode() {
        @SuppressWarnings("unchecked") final DataObjectModification<Node> node = mock(DataObjectModification.class);
        final Collection<DataTreeModification<Node>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Node> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(node);

        final Node netconfNodeBefore = getNetconfNode("netconfNode1",
            NetconfNodeConnectionStatus.ConnectionStatus.Connecting, OPENROADM_DEVICE_VERSION_2_2_1);
        final Node netconfNodeAfter = getNetconfNode("netconfNode1",
            NetconfNodeConnectionStatus.ConnectionStatus.Connected, OPENROADM_DEVICE_VERSION_2_2_1);
        when(node.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(node.getDataBefore()).thenReturn(netconfNodeBefore);
        when(node.getDataAfter()).thenReturn(netconfNodeAfter);

        NetConfTopologyListener listener = new NetConfTopologyListener(networkModelService, dataBroker,
            deviceTransactionManager, portMapping);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(node, times(1)).getModificationType();
        verify(node, times(3)).getDataBefore();
        verify(node, times(1)).getDataAfter();
        verify(networkModelService, times(1)).createOpenRoadmNode(anyString(), anyString());
    }

    @Test
    public void testOnDataTreeChangedWhenDisconnectingNode() {
        @SuppressWarnings("unchecked") final DataObjectModification<Node> node = mock(DataObjectModification.class);
        final Collection<DataTreeModification<Node>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Node> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(node);

        final Node netconfNodeBefore = getNetconfNode("netconfNode1",
            NetconfNodeConnectionStatus.ConnectionStatus.Connected, OPENROADM_DEVICE_VERSION_2_2_1);
        final Node netconfNodeAfter = getNetconfNode("netconfNode1",
            NetconfNodeConnectionStatus.ConnectionStatus.Connecting, OPENROADM_DEVICE_VERSION_2_2_1);
        when(node.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(node.getDataBefore()).thenReturn(netconfNodeBefore);
        when(node.getDataAfter()).thenReturn(netconfNodeAfter);

        NetConfTopologyListener listener = new NetConfTopologyListener(networkModelService, dataBroker,
            deviceTransactionManager, portMapping);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(node, times(1)).getModificationType();
        verify(node, times(3)).getDataBefore();
        verify(node, times(1)).getDataAfter();
        verify(networkModelService, never()).createOpenRoadmNode(anyString(), anyString());
        verify(networkModelService, never()).deleteOpenRoadmnode(anyString());
    }

    @Test
    public void testOnDataTreeChangedWhenShouldNeverHappen() {
        @SuppressWarnings("unchecked") final DataObjectModification<Node> node = mock(DataObjectModification.class);
        final Collection<DataTreeModification<Node>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Node> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(node);

        final Node netconfNodeBefore = getNetconfNode("netconfNode1",
            NetconfNodeConnectionStatus.ConnectionStatus.Connected, OPENROADM_DEVICE_VERSION_2_2_1);
        when(node.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(node.getDataBefore()).thenReturn(netconfNodeBefore);

        NetConfTopologyListener listener = new NetConfTopologyListener(networkModelService, dataBroker,
            deviceTransactionManager, portMapping);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(node, times(2)).getModificationType();
        verify(node, times(3)).getDataBefore();
        verify(node, never()).getDataAfter();
        verify(networkModelService, never()).createOpenRoadmNode(anyString(), anyString());
        verify(networkModelService, never()).deleteOpenRoadmnode(anyString());
    }

    private Node getNetconfNode(final String nodeId, final NetconfNodeConnectionStatus.ConnectionStatus cs,
        final String openRoadmVersion) {
        final List<AvailableCapability> avCapList = new ArrayList<>();
        avCapList.add(new AvailableCapabilityBuilder()
            .setCapabilityOrigin(AvailableCapability.CapabilityOrigin.UserDefined)
            .setCapability(openRoadmVersion)
            .build());
        final AvailableCapabilities avCaps = new AvailableCapabilitiesBuilder().setAvailableCapability(avCapList)
            .build();
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
        return new NodeBuilder()
            .withKey(new NodeKey(new NodeId(nodeId)))
            .setNodeId(new NodeId(nodeId))
            .addAugmentation(netconfNode)
            .build();
    }
}
