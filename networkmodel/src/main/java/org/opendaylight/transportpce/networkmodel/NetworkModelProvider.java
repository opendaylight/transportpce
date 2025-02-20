/*
 * Copyright © 2016 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.listeners.PortMappingListener;
import org.opendaylight.transportpce.networkmodel.listeners.ServiceHandlerListener;
import org.opendaylight.transportpce.networkmodel.service.FrequenciesService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.networkmodel.util.TpceNetwork;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the starting point of the networkmodel module. It starts the different objects and initialize the
 * different topology layer instances.
 */
@Component
public class NetworkModelProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelProvider.class);
    private static final DataObjectReference<Node> NETCONF_NODE_II = DataObjectReference
            .builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
            .child(Node.class)
            .build();
    private static final DataObjectReference<Mapping> MAPPING_II = DataObjectReference
            .builder(Network.class)
            .child(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network
                    .Nodes.class)
            .child(Mapping.class)
            .build();


    private final DataBroker dataBroker;
    private final NetConfTopologyListener topologyListener;
    private List<Registration> listeners;
    private TpceNetwork tpceNetwork;
    private Registration serviceHandlerListenerRegistration;
    private NotificationService notificationService;
    private FrequenciesService frequenciesService;
    private PortMappingListener portMappingListener;

    /**
     * Instantiate the NetworkModelProvider.
     *
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     * @param dataBroker Provides access to the conceptual data tree store in order to register data change listeners
     * @param networkModelService Service that eases data handling in topology datastores
     * @param deviceTransactionManager Manages data transactions with the netconf devices
     * @param portMapping Store the abstraction view of the netconf device
     * @param notificationService Notification broker which allows to subscribe for notifications
     * @param frequenciesService Object that ease WDM spectrum handling
     */
    @Activate
    public NetworkModelProvider(@Reference NetworkTransactionService networkTransactionService,
            @Reference final DataBroker dataBroker,
            @Reference final NetworkModelService networkModelService,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference PortMapping portMapping,
            @Reference NotificationService notificationService,
            @Reference FrequenciesService frequenciesService) {
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
        this.frequenciesService = frequenciesService;
        this.listeners = new ArrayList<>();
        this.topologyListener = new NetConfTopologyListener(networkModelService, dataBroker, deviceTransactionManager,
            portMapping);
        this.tpceNetwork = new TpceNetwork(networkTransactionService);
        this.portMappingListener = new PortMappingListener(networkModelService);
        this.init();
    }

    /**
     * Method called when OSGi service is created.
     */
    private void init() {
        LOG.info("NetworkModelProvider Session Initiated");
        tpceNetwork.createLayer(StringConstants.CLLI_NETWORK);
        tpceNetwork.createLayer(StringConstants.OPENROADM_NETWORK);
        tpceNetwork.createLayer(StringConstants.OPENROADM_TOPOLOGY);
        tpceNetwork.createLayer(StringConstants.OTN_NETWORK);
        listeners.add(dataBroker.registerTreeChangeListener(LogicalDatastoreType.OPERATIONAL, NETCONF_NODE_II,
                topologyListener));
        listeners.add(dataBroker.registerTreeChangeListener(LogicalDatastoreType.CONFIGURATION, MAPPING_II,
                portMappingListener));
        serviceHandlerListenerRegistration = notificationService.registerCompositeListener(
            new ServiceHandlerListener(frequenciesService).getCompositeListener());
    }

    /**
     * Method called when the OSGi service is destroyed.
     */
    @Deactivate
    public void close() {
        LOG.info("NetworkModelProvider Closed");
        listeners.forEach(lis -> lis.close());
        listeners.clear();
        serviceHandlerListenerRegistration.close();
    }
}
