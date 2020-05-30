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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelServiceImpl;
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

    @Mock
    private DeviceTransactionManager deviceTransactionManager;

    @Before
    public void setUp() {

    }

    @Test
    public void testOnDataTreeChanged() {

        final DataObjectModification<Node> newNode = mock(DataObjectModification.class);
        when(newNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        final List<AvailableCapability> avCapList = new ArrayList<>();
        avCapList.add(new AvailableCapabilityBuilder()
                .setCapabilityOrigin(AvailableCapability.CapabilityOrigin.UserDefined)
                .setCapability("org-openroadm-device")
                .build());
        final AvailableCapabilities avCaps =
                new AvailableCapabilitiesBuilder().setAvailableCapability(avCapList).build();
        final NodeBuilder nn = new NodeBuilder().setNodeId(new NodeId("test-node"))
                .addAugmentation(NetconfNode.class, new NetconfNodeBuilder()
                        .setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connecting)
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
                        .build());
        Node nnn = nn.build();
        when(newNode.getDataAfter()).thenReturn(nnn);

        final Collection<DataTreeModification<Node>> changes = new HashSet<>();
        final DataTreeModification<Node> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(newNode);

        DataStoreContextImpl dataStoreContext = new DataStoreContextImpl();
        DataBroker dataBroker = dataStoreContext.getDataBroker();
        RequestProcessor requestProcessor = new RequestProcessor(dataBroker);
        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(requestProcessor);
        R2RLinkDiscovery linkDiskovery = new R2RLinkDiscovery(
                dataBroker, deviceTransactionManager, networkTransactionService);
        PortMappingVersion121 p1 = mock(PortMappingVersion121.class);
        PortMappingVersion221 p2 = mock(PortMappingVersion221.class);
        PortMapping portMapping = new PortMappingImpl(dataBroker, p2, p1);
        NetworkModelServiceImpl networkModelServiceImpl = new NetworkModelServiceImpl(networkTransactionService,
                linkDiskovery, portMapping);
        NetConfTopologyListener listener = new NetConfTopologyListener(networkModelServiceImpl, dataBroker,
                deviceTransactionManager);
        listener.onDataTreeChanged(changes);
        verify(ch).getRootNode();
        verify(newNode, times(3)).getDataAfter();
        verify(newNode, times(2)).getModificationType();

    }

}
