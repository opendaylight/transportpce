/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import org.opendaylight.transportpce.renderer.listeners.AlarmNotificationListener;
import org.opendaylight.transportpce.renderer.listeners.DeOperationsListener;
import org.opendaylight.transportpce.renderer.listeners.DeviceListener;
import org.opendaylight.transportpce.renderer.listeners.LldpListener;
import org.opendaylight.transportpce.renderer.listeners.TcaListener;
import org.opendaylight.transportpce.renderer.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.AlarmNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.OrgOpenroadmAlarmListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.OrgOpenroadmDeOperationsListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev161014.OrgOpenroadmDeviceListener;
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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererNotificationsImpl implements DataTreeChangeListener<Node> {

    private final DataBroker dataBroker;
    private final MountPointService mountService;
    private static final Logger LOG = LoggerFactory.getLogger(RendererNotificationsImpl.class);
    private ListenerRegistration<RendererNotificationsImpl> dataTreeChangeListenerRegistration;

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
        Set<String> currentMountedDevice) {
        this.dataBroker = dataBroker;
        this.mountService = mountService;
        this.currentMountedDevice = currentMountedDevice;
        if (mountService == null) {
            LOG.error("Mount service is null");

        }
        if (dataBroker != null) {
            this.dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID.child(Node.class)), this);
        }
    }

    private void registerNotificationListener(final NodeId nodeId) {
        final Optional<MountPoint> mountPoint;
        try {
            // Get mount point for specified device
            mountPoint = mountService.getMountPoint(mountIds.get(nodeId.getValue()));
            if (!mountPoint.isPresent()) {
                LOG.error("Mount point for node {} doesn't exist", nodeId.getValue());
            }
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(e);
        }

        // Register notification service
        final Optional<NotificationService> notificationService = mountPoint.get().getService(
            NotificationService.class);
        if (!notificationService.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeId.getValue());
        }

        final OrgOpenroadmAlarmListener alarmListener;
        alarmListener = new AlarmNotificationListener();
        LOG.info("Registering notification listener on {} for node: {}", AlarmNotification.QNAME, nodeId);
        // Register notification listener
        final ListenerRegistration<OrgOpenroadmAlarmListener>
            accessAlarmNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(alarmListener);

        final OrgOpenroadmDeOperationsListener deOperationsListener;
        deOperationsListener = new DeOperationsListener();
        LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
        // Register notification listener
        final ListenerRegistration<OrgOpenroadmDeOperationsListener>
            accessDeOperationasNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(deOperationsListener);

        final OrgOpenroadmDeviceListener deviceListener;
        deviceListener = new DeviceListener();
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        // Register notification listener
        final ListenerRegistration<OrgOpenroadmDeviceListener>
            accessDeviceNotificationListenerRegistration = notificationService.get()
                .registerNotificationListener(deviceListener);

        final OrgOpenroadmLldpListener lldpListener;
        lldpListener = new LldpListener();
        LOG.info("Registering notification listener on OrgOpenroadmLldpListener for node: {}", nodeId);
        // Register notification listener
        final ListenerRegistration<OrgOpenroadmLldpListener> accessLldpNotificationListenerRegistration =
            notificationService.get().registerNotificationListener(lldpListener);

        final OrgOpenroadmTcaListener tcaListener;
        tcaListener = new TcaListener();
        LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
        // Register notification listener
        final ListenerRegistration<OrgOpenroadmTcaListener> accessTcaNotificationListenerRegistration =
            notificationService.get().registerNotificationListener(tcaListener);

        // Listening to NETCONF datastream
        final String streamName = "NETCONF";
        final Optional<RpcConsumerRegistry> service = mountPoint.get().getService(RpcConsumerRegistry.class);
        if (!service.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeId.getValue());
        }
        final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
        final CreateSubscriptionInputBuilder createSubscriptionInputBuilder = new CreateSubscriptionInputBuilder();
        createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
        LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
        final Future<RpcResult<Void>> subscription = rpcService.createSubscription(createSubscriptionInputBuilder
            .build());
    }

    public void close() {
        LOG.info("RenderernotificationsImpl Closed");
        // Clean up the Data Change Listener registration
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {

        for (DataTreeModification<Node> change : changes) {

            DataObjectModification<Node> rootNode = change.getRootNode();
            String nodeId = rootNode.getDataAfter().getKey().getNodeId().getValue();
            NetconfNode nnode = Preconditions.checkNotNull(rootNode.getDataAfter().getAugmentation(NetconfNode.class),
                "Node not connected via Netconf protocol");
            if (nnode != null) {

                if (rootNode.getModificationType() == ModificationType.WRITE) {
                    LOG.info("Node added " + nodeId);

                } else if (rootNode.getModificationType() == ModificationType.SUBTREE_MODIFIED) {

                    LOG.info("Node modified " + nodeId);
                    ConnectionStatus csts = nnode.getConnectionStatus();

                    switch (csts) {
                        case Connected: {
                            LOG.info("NETCONF Node: {} is fully connected", nodeId);
                            List<String> capabilities = nnode.getAvailableCapabilities().getAvailableCapability()
                                .stream().map(cp -> cp.getCapability()).collect(Collectors.toList());
                            LOG.info("Capabilities: {}", capabilities);
                            /*
                             * TODO: check for required
                             * capabilities to listen for notifications
                             * registerNotificationListener(rootNode.
                             * getDataAfter(). getNodeId());
                             */
                            currentMountedDevice.add(nodeId);
                            new PortMapping(dataBroker, mountService, nodeId).createMappingData();
                            break;
                        }
                        case Connecting: {
                            LOG.info("NETCONF Node: {} was disconnected", nodeId);
                            break;
                        }
                        case UnableToConnect: {
                            LOG.info("NETCONF Node: {} connection failed", nodeId);
                            break;
                        }
                        default:
                            LOG.warn("Unexpected connection status " + csts.getName());
                    }
                } else if (rootNode.getModificationType() ==  ModificationType.DELETE) {
                    LOG.info("Node removed " + nodeId);
                    currentMountedDevice.remove(nodeId);
                }
            }
        }
    }
}