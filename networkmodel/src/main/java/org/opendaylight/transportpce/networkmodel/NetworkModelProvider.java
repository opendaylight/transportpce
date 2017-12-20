/*
 * Copyright © 2016 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.networkmodel.util.ClliNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.NetworkutilsService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final NetworkutilsService networkutilsService;
    private final NetConfTopologyListener topologyListener;
    private final OpenRoadmTopology openRoadmTopology;
    private ListenerRegistration<NetConfTopologyListener> dataTreeChangeListenerRegistration;
    private BindingAwareBroker.RpcRegistration<NetworkutilsService> networkutilsServiceRpcRegistration;

    public NetworkModelProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry,
            final NetworkutilsService networkutilsService, final NetConfTopologyListener topologyListener,
            OpenRoadmTopology openRoadmTopology) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.networkutilsService = networkutilsService;
        this.topologyListener = topologyListener;
        this.openRoadmTopology = openRoadmTopology;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NetworkModelProvider Session Initiated");
        ClliNetwork.createClliLayer(dataBroker);
        OpenRoadmNetwork.createOpenRoadmNetworkLayer(dataBroker);
        openRoadmTopology.createTopoLayer(dataBroker);
        dataTreeChangeListenerRegistration =
                dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                        InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), topologyListener);
        networkutilsServiceRpcRegistration =
                rpcProviderRegistry.addRpcImplementation(NetworkutilsService.class, networkutilsService);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("NetworkModelProvider Closed");
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
        if (networkutilsServiceRpcRegistration != null) {
            networkutilsServiceRpcRegistration.close();
        }
    }

}
