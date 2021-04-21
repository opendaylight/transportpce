/*
 * Copyright © 2016 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration22;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration710;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener221;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener710;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener710;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener710;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener221;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener710;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.OrgOpenroadmAlarmListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.OrgOpenroadmDeOperationsListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev161014.OrgOpenroadmTcaListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetConfTopologyListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NetConfTopologyListener.class);

    private final NetworkModelService networkModelService;
    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final Map<String, NodeRegistration> registrations;
    private final Map<String, NodeRegistration22> registrations22;
    private final Map<String, NodeRegistration710> registrations710;

    public NetConfTopologyListener(final NetworkModelService networkModelService, final DataBroker dataBroker,
             DeviceTransactionManager deviceTransactionManager) {
        this.networkModelService = networkModelService;
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.registrations = new ConcurrentHashMap<>();
        this.registrations22 = new ConcurrentHashMap<>();
        this.registrations710 = new ConcurrentHashMap<>();
    }

    @SuppressFBWarnings(
        value = "RV_RETURN_VALUE_IGNORED",
        justification = "nothing to verify once rpc has been sent")
    private void onDeviceConnected(final String nodeId, String openRoadmVersion) {
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
                mountPoint.getService(NotificationService.class);
        if (!notificationService.isPresent()) {
            LOG.error("Failed to get RpcService for node {}", nodeId);
            return;
        }

        if (openRoadmVersion.equals(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1)) {

            final OrgOpenroadmAlarmListener alarmListener = new AlarmNotificationListener(this.dataBroker);
            LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
            final ListenerRegistration<OrgOpenroadmAlarmListener> accessAlarmNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(alarmListener);

            final OrgOpenroadmDeOperationsListener deOperationsListener = new DeOperationsListener();
            LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
            final ListenerRegistration<OrgOpenroadmDeOperationsListener>
                accessDeOperationasNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(deOperationsListener);

            final OrgOpenroadmDeviceListener deviceListener = new DeviceListener(this.deviceTransactionManager,
                    nodeId, this.networkModelService);
            LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
            final ListenerRegistration<OrgOpenroadmDeviceListener> accessDeviceNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(deviceListener);

            TcaListener tcaListener = new TcaListener();
            LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
            final ListenerRegistration<OrgOpenroadmTcaListener> accessTcaNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(tcaListener);

            String streamName = "OPENROADM";

            if (streamName == null) {
                streamName = "NETCONF";
            }

            final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class);
            if (service.isPresent()) {
                final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
                if (rpcService == null) {
                    LOG.error("Failed to get RpcService for node {}", nodeId);
                } else {
                    final CreateSubscriptionInputBuilder createSubscriptionInputBuilder =
                        new CreateSubscriptionInputBuilder();
                    createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
                    LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
                    rpcService.createSubscription(createSubscriptionInputBuilder.build());
                }
            } else {
                LOG.error("Failed to get RpcService for node {}", nodeId);
            }
            NodeRegistration nodeRegistration = new NodeRegistration(nodeId,
                accessAlarmNotificationListenerRegistration,
                accessDeOperationasNotificationListenerRegistration, accessDeviceNotificationListenerRegistration,
                null, accessTcaNotificationListenerRegistration);
            registrations.put(nodeId, nodeRegistration);

        } else if (openRoadmVersion.equals(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1)) {
            final org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev181019.OrgOpenroadmAlarmListener
                alarmListener = new AlarmNotificationListener221(dataBroker);
            LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev181019
                .OrgOpenroadmAlarmListener> accessAlarmNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(alarmListener);

            final org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev181019
                .OrgOpenroadmDeOperationsListener deOperationsListener = new DeOperationsListener221();
            LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev181019
                .OrgOpenroadmDeOperationsListener> accessDeOperationasNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(deOperationsListener);

            final org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceListener
                deviceListener = new DeviceListener221();
            LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .OrgOpenroadmDeviceListener> accessDeviceNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(deviceListener);

            final org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev181019.OrgOpenroadmTcaListener
                tcaListener = new TcaListener221();
            LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev181019
                .OrgOpenroadmTcaListener> accessTcaNotificationListenerRegistration =
                notificationService.get().registerNotificationListener(tcaListener);


            String streamName = "OPENROADM";
            if (streamName == null) {
                streamName = "NETCONF";
            }
            final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class);
            if (service.isPresent()) {
                final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
                if (rpcService == null) {
                    LOG.error("Failed to get RpcService for node {}", nodeId);
                } else {
                    final CreateSubscriptionInputBuilder createSubscriptionInputBuilder =
                        new CreateSubscriptionInputBuilder();
                    createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
                    LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
                    rpcService.createSubscription(createSubscriptionInputBuilder.build());
                }
            } else {
                LOG.error("Failed to get RpcService for node {}", nodeId);
            }
            NodeRegistration22 nodeRegistration22 = new NodeRegistration22(nodeId,
                accessAlarmNotificationListenerRegistration,
                accessDeOperationasNotificationListenerRegistration, accessDeviceNotificationListenerRegistration,
                null, accessTcaNotificationListenerRegistration);
            registrations22.put(nodeId, nodeRegistration22);

        } else if (openRoadmVersion.equals(StringConstants.OPENROADM_DEVICE_VERSION_7_1_0)) {
            final org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev200529.OrgOpenroadmAlarmListener
                    alarmListener = new AlarmNotificationListener710(dataBroker);
            LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev200529
                    .OrgOpenroadmAlarmListener> accessAlarmNotificationListenerRegistration =
                    notificationService.get().registerNotificationListener(alarmListener);

            final org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev200529
                    .OrgOpenroadmDeOperationsListener deOperationsListener = new DeOperationsListener710();
            LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev200529
                    .OrgOpenroadmDeOperationsListener> accessDeOperationasNotificationListenerRegistration =
                    notificationService.get().registerNotificationListener(deOperationsListener);

            final org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceListener
                    deviceListener = new DeviceListener710();
            LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                    .OrgOpenroadmDeviceListener> accessDeviceNotificationListenerRegistration =
                    notificationService.get().registerNotificationListener(deviceListener);

            final org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev200327.OrgOpenroadmTcaListener
                    tcaListener = new TcaListener710();
            LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
            final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev200327
                    .OrgOpenroadmTcaListener> accessTcaNotificationListenerRegistration =
                    notificationService.get().registerNotificationListener(tcaListener);

            String streamName = "OPENROADM";
            if (streamName == null) {
                streamName = "NETCONF";
            }
            final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class);
            if (service.isPresent()) {
                final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
                if (rpcService == null) {
                    LOG.error("Failed to get RpcService for node {}", nodeId);
                } else {
                    final CreateSubscriptionInputBuilder createSubscriptionInputBuilder =
                            new CreateSubscriptionInputBuilder();
                    createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
                    LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
                    rpcService.createSubscription(createSubscriptionInputBuilder.build());
                }
            } else {
                LOG.error("Failed to get RpcService for node {}", nodeId);
            }
            NodeRegistration710 nodeRegistration710 = new NodeRegistration710(nodeId,
                    accessAlarmNotificationListenerRegistration,
                    accessDeOperationasNotificationListenerRegistration, accessDeviceNotificationListenerRegistration,
                    null, accessTcaNotificationListenerRegistration);
            registrations710.put(nodeId, nodeRegistration710);
        }
    }

    private void onDeviceDisConnected(final String nodeId) {
        LOG.info("onDeviceDisConnected: {}", nodeId);
        NodeRegistration nodeRegistration = this.registrations.remove(nodeId);
        if (nodeRegistration != null) {
            nodeRegistration.getAccessAlarmNotificationListenerRegistration().close();
            nodeRegistration.getAccessDeOperationasNotificationListenerRegistration().close();
            nodeRegistration.getAccessDeviceNotificationListenerRegistration().close();
            nodeRegistration.getAccessTcaNotificationListenerRegistration().close();
        }
    }

    @Override
    @SuppressFBWarnings(
        value = "SF_SWITCH_FALLTHROUGH",
        justification = "intentional fallthrough")
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        LOG.info("onDataTreeChanged");
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            if ((rootNode.getDataAfter() == null) && (rootNode.getModificationType() != ModificationType.DELETE)) {
                LOG.error("rootNode.getDataAfter is null : Node not connected via Netconf protocol");
                continue;
            }
            if (rootNode.getModificationType() == ModificationType.DELETE) {
                if (rootNode.getDataBefore() != null) {
                    String nodeId = rootNode.getDataBefore().key().getNodeId().getValue();
                    LOG.info("Node {} deleted", nodeId);
                    this.networkModelService.deleteOpenRoadmnode(nodeId);
                    onDeviceDisConnected(nodeId);
                } else {
                    LOG.error("rootNode.getDataBefore is null !");
                }
                continue;
            }
            String nodeId = rootNode.getDataAfter().key().getNodeId().getValue();
            NetconfNode netconfNode = rootNode.getDataAfter().augmentation(NetconfNode.class);

            if ((netconfNode != null) && !StringConstants.DEFAULT_NETCONF_NODEID.equals(nodeId)) {
                switch (rootNode.getModificationType()) {
                    case WRITE:
                        LOG.info("Node added: {}", nodeId);
                    //fallthrough
                    case SUBTREE_MODIFIED:
                        NetconfNodeConnectionStatus.ConnectionStatus connectionStatus =
                                netconfNode.getConnectionStatus();
                        try {
                            List<AvailableCapability> deviceCapabilities = netconfNode.getAvailableCapabilities()
                                .getAvailableCapability().stream().filter(cp -> cp.getCapability()
                                .contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME)).collect(Collectors.toList());
                            if (!deviceCapabilities.isEmpty()) {
                                Collections.sort(deviceCapabilities, (cp0, cp1) -> cp1.getCapability()
                                    .compareTo(cp0.getCapability()));
                                LOG.info("OpenROADM node detected: {} {}", nodeId, connectionStatus.name());
                                switch (connectionStatus) {
                                    case Connected:
                                        this.networkModelService.createOpenRoadmNode(nodeId, deviceCapabilities.get(0)
                                            .getCapability());
                                        onDeviceConnected(nodeId,deviceCapabilities.get(0).getCapability());
                                        break;
                                    case Connecting:
                                    case UnableToConnect:
                                        this.networkModelService.setOpenRoadmNodeStatus(nodeId, connectionStatus);
                                        onDeviceDisConnected(nodeId);
                                        break;
                                    default:
                                        LOG.warn("Unsupported device state {}", connectionStatus.getName());
                                        break;
                                }
                            }

                        } catch (NullPointerException e) {
                            LOG.error("Cannot get available Capabilities");
                        }
                        break;
                    default:
                        LOG.warn("Unexpected connection status : {}", rootNode.getModificationType());
                        break;
                }
            }
        }
    }


    /*private String getSupportedStream(String nodeId) {
        InstanceIdentifier<Streams> streamsIID = InstanceIdentifier.create(Netconf.class).child(Streams.class);
        try {
            Optional<Streams> ordmInfoObject =
                    this.deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL,
                            streamsIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (!ordmInfoObject.isPresent()) {
                LOG.error("Get Stream RPC is not supported");
                return "NETCONF";
            }
            for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf
                        .streams.Stream strm : ordmInfoObject.get().getStream()) {

                if ("OPENROADM".equalsIgnoreCase(strm.getName().getValue())) {
                    return strm.getName().getValue().toUpperCase();
                }
            }
            return "NETCONF";
        } catch (NullPointerException ex) {
            LOG.error("NullPointerException thrown while getting Info from a non Open ROADM device {}", nodeId);
            return "NETCONF";
        }
    }*/
}
