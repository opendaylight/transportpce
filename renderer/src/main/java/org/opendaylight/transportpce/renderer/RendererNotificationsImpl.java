/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.renderer.listeners.AlarmNotificationListener;
import org.opendaylight.transportpce.renderer.listeners.DeOperationsListener;
import org.opendaylight.transportpce.renderer.listeners.DeviceListener;
import org.opendaylight.transportpce.renderer.listeners.LldpListener;
import org.opendaylight.transportpce.renderer.listeners.TcaListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.AlarmNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.OrgOpenroadmAlarmListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.OrgOpenroadmDeOperationsListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.OrgOpenroadmLldpListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev161014.OrgOpenroadmTcaListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererNotificationsImpl implements DataTreeChangeListener<Node> {
    private final DataBroker dataBroker;
    private final MountPointService mountService;
    private static final Logger LOG = LoggerFactory.getLogger(RendererNotificationsImpl.class);
    private ListenerRegistration<RendererNotificationsImpl> dataTreeChangeListenerRegistration;
    private final PortMapping portMapping;
    private final DeviceTransactionManager deviceTransactionManager;
    private final Set<String> currentMountedDevice;
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID = InstanceIdentifier.create(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    LoadingCache<String, KeyedInstanceIdentifier<Node, NodeKey>> mountIds = CacheBuilder.newBuilder().maximumSize(20)
        .build(new CacheLoader<String, KeyedInstanceIdentifier<Node, NodeKey>>() {
            @Override
            public KeyedInstanceIdentifier<Node, NodeKey> load(final String key) {
                return NETCONF_TOPO_IID.child(Node.class, new NodeKey(new NodeId(key)));
            }
        });

    public RendererNotificationsImpl(final DataBroker dataBroker, final MountPointService mountService,
        Set<String> currentMountedDevice, PortMapping portMapping,DeviceTransactionManager deviceTransactionManager) {
        this.dataBroker = dataBroker;
        this.mountService = mountService;
        this.currentMountedDevice = currentMountedDevice;
        this.portMapping = portMapping;
        this.deviceTransactionManager = deviceTransactionManager;
        if (portMapping == null) {
            LOG.error("Portmapping is null !");
        }
        if (deviceTransactionManager == null) {
            LOG.error("deviceTransactionManager is null");
        }
        if (mountService == null) {
            LOG.error("Mount service is null");
        }
        if (dataBroker != null) {
            this.dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID.child(Node.class)), this);
        }
    }

    private void registerNotificationListener(final String nodeId) {

        LOG.info("onDeviceConnected: {}", nodeId);
        Optional<MountPoint> mountPointOpt = this.deviceTransactionManager.getDeviceMountPoint(nodeId);
        MountPoint mountPoint;
        if (mountPointOpt.isPresent()) {
            mountPoint = mountPointOpt.get();
        } else {
            LOG.error("Failed to get mount point for node {}", nodeId);
            return;
        }

        final Optional<NotificationService> notificationService =
                mountPoint.getService(NotificationService.class).toJavaUtil();
        if (!notificationService.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeId);
            return;
        }

        final OrgOpenroadmAlarmListener alarmListener;
        alarmListener = new AlarmNotificationListener();
        LOG.info("Registering notification listener on {} for node: {}", AlarmNotification.QNAME, nodeId);
        // Register notification listener

        final OrgOpenroadmDeOperationsListener deOperationsListener;
        deOperationsListener = new DeOperationsListener();
        LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
        // Register notification listener

        final OrgOpenroadmDeviceListener deviceListener;
        deviceListener = new DeviceListener();
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        // Register notification listener

        final OrgOpenroadmLldpListener lldpListener;
        lldpListener = new LldpListener();
        LOG.info("Registering notification listener on OrgOpenroadmLldpListener for node: {}", nodeId);
        // Register notification listener

        final OrgOpenroadmTcaListener tcaListener;
        tcaListener = new TcaListener();
        LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
        // Register notification listener

        // Listening to NETCONF datastream
        final String streamName = "NETCONF";
        final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class).toJavaUtil();
        if (!service.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeId);
        }

        final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
        final CreateSubscriptionInputBuilder createSubscriptionInputBuilder = new CreateSubscriptionInputBuilder();
        createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
        LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);

    }

    public void close() {
        LOG.info("RenderernotificationsImpl Closed");
        // Clean up the Data Change Listener registration
        if (this.dataTreeChangeListenerRegistration != null) {
            this.dataTreeChangeListenerRegistration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {

        for (DataTreeModification<Node> change : changes) {

            DataObjectModification<Node> rootNode = change.getRootNode();
            NetconfNode nnode = null;
            String nodeId = new String();
            if (rootNode.getDataAfter() != null) {
                nnode = Preconditions.checkNotNull(rootNode.getDataAfter().getAugmentation(NetconfNode.class),
                    "Node not connected via Netconf protocol");
                nodeId = rootNode.getDataAfter().getKey().getNodeId().getValue();
            }

            if (rootNode.getModificationType() == ModificationType.DELETE) {
                String nodeid = rootNode.getDataBefore().getKey().getNodeId().getValue();
                LOG.info("Node {} removed...", nodeid);
                this.portMapping.deleteMappingData(nodeid);
            }

            if (nnode != null) {
                if (nodeId.equals("controller-config")) {
                    // We shouldn't process controller-config as an OpenROAM device
                    LOG.info("{} ignored: org-openroadm-device advertised but not a real ROADM device", nodeId);
                    return;
                }
                if ((rootNode.getModificationType() == ModificationType.WRITE) ||
                        (rootNode.getModificationType() == ModificationType.SUBTREE_MODIFIED)) {
                    LOG.info("Node added or modified {}", nodeId);
                    ConnectionStatus csts = nnode.getConnectionStatus();

                    switch (csts) {
                        case Connected: {
                            LOG.info("NETCONF Node: {} is fully connected", nodeId);
                            List<String> capabilities = nnode.getAvailableCapabilities().getAvailableCapability()
                                .stream().map(cp -> cp.getCapability()).collect(Collectors.toList());
                            LOG.info("Capabilities: {}", capabilities);
                            /*
                             * TODO: check for required capabilities to listen
                             * for notifications
                             */
                            registerNotificationListener(nodeId);
                            this.currentMountedDevice.add(nodeId);
                            this.portMapping.createMappingData(nodeId);
                            break;
                        }
                        case Connecting: {
                            LOG.info("NETCONF Node: {} is (dis)connecting", nodeId);
                            break;
                        }
                        case UnableToConnect: {
                            LOG.info("NETCONF Node: {} connection failed", nodeId);
                            break;
                        }
                        default:
                            LOG.warn("Unexpected connection status {}", csts.getName());
                    }
                }
            }
        }
        LOG.info("Netconf devices currently mounted are : {}", this.currentMountedDevice.toString());
    }
}
