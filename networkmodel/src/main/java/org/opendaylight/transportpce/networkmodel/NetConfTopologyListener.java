/*
 * Copyright © 2016 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration22;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener121;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener221;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener221;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.OrgOpenroadmAlarmListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.OrgOpenroadmDeOperationsListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev161014.OrgOpenroadmTcaListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetConfTopologyListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NetConfTopologyListener.class);
    private static final String RPC_SERVICE_FAILED = "Failed to get RpcService for node {}";
    private final NetworkModelService networkModelService;
    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final Map<String, NodeRegistration> registrations;
    private final Map<String, NodeRegistration22> registrations22;
    private final PortMapping portMapping;

    public NetConfTopologyListener(final NetworkModelService networkModelService, final DataBroker dataBroker,
             DeviceTransactionManager deviceTransactionManager, PortMapping portMapping) {
        this.networkModelService = networkModelService;
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.registrations = new ConcurrentHashMap<>();
        this.registrations22 = new ConcurrentHashMap<>();
        this.portMapping = portMapping;
    }

    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        LOG.info("onDataTreeChanged - {}", this.getClass().getSimpleName());
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            if (rootNode.getDataBefore() == null) {
                continue;
            }
            String nodeId = rootNode.getDataBefore().key().getNodeId().getValue();
            NetconfNode netconfNodeBefore = rootNode.getDataBefore().augmentation(NetconfNode.class);
            switch (rootNode.getModificationType()) {
                case DELETE:
                    this.networkModelService.deleteOpenRoadmnode(nodeId);
                    String deviceVersion = netconfNodeBefore
                        .getAvailableCapabilities().getAvailableCapability().stream()
                        .filter(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME))
                        .sorted((c1, c2) -> c1.getCapability().compareTo(c2.getCapability()))
                        .findFirst()
                        .get().getCapability();
                    onDeviceDisConnected(nodeId, deviceVersion);
                    LOG.info("Device {} correctly disconnected from controller", nodeId);
                    break;
                case WRITE:
                    NetconfNode netconfNodeAfter = rootNode.getDataAfter().augmentation(NetconfNode.class);
                    if (ConnectionStatus.Connecting.equals(netconfNodeBefore.getConnectionStatus())
                        && ConnectionStatus.Connected.equals(netconfNodeAfter.getConnectionStatus())) {
                        LOG.info("Connecting Node: {}", nodeId);
                        Optional<AvailableCapability> deviceCapabilityOpt = netconfNodeAfter
                            .getAvailableCapabilities().getAvailableCapability().stream()
                            .filter(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME))
                            .sorted((c1, c2) -> c1.getCapability().compareTo(c2.getCapability()))
                            .findFirst();
                        if (deviceCapabilityOpt.isEmpty()) {
                            LOG.error("Unable to get openroadm-device-capability");
                            return;
                        }
                        this.networkModelService
                            .createOpenRoadmNode(nodeId, deviceCapabilityOpt.get().getCapability());
                        onDeviceConnected(nodeId,deviceCapabilityOpt.get().getCapability());
                        LOG.info("Device {} correctly connected to controller", nodeId);
                    }
                    if (ConnectionStatus.Connected.equals(netconfNodeBefore.getConnectionStatus())
                        && ConnectionStatus.Connecting.equals(netconfNodeAfter.getConnectionStatus())) {
                        LOG.warn("Node: {} is being disconnected", nodeId);
                    }
                    break;
                default:
                    LOG.debug("Unknown modification type {}", rootNode.getModificationType().name());
                    break;
            }
        }
    }

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
        final Optional<NotificationService> notificationService = mountPoint.getService(NotificationService.class);
        if (!notificationService.isPresent()) {
            LOG.error(RPC_SERVICE_FAILED, nodeId);
            return;
        }
        switch (openRoadmVersion) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                NodeRegistration node121Registration = registrateNode121Listeners(nodeId, notificationService.get());
                registrations.put(nodeId, node121Registration);
                break;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                NodeRegistration22 node221Registration = registrateNode221Listeners(nodeId, notificationService.get());
                registrations22.put(nodeId, node221Registration);
                break;
            default:
                break;
        }
        String streamName = "NETCONF";
        subscribeStream(mountPoint, streamName, nodeId);
    }

    private void onDeviceDisConnected(final String nodeId, String openRoadmVersion) {
        LOG.info("onDeviceDisConnected: {}", nodeId);
        switch (openRoadmVersion) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                NodeRegistration nodeRegistration = this.registrations.remove(nodeId);
                if (nodeRegistration != null) {
                    nodeRegistration.getAccessAlarmNotificationListenerRegistration().close();
                    nodeRegistration.getAccessDeOperationasNotificationListenerRegistration().close();
                    nodeRegistration.getAccessDeviceNotificationListenerRegistration().close();
                    nodeRegistration.getAccessTcaNotificationListenerRegistration().close();
                }
                break;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                NodeRegistration22 nodeRegistration221 = this.registrations22.remove(nodeId);
                if (nodeRegistration221 != null) {
                    nodeRegistration221.getAccessAlarmNotificationListenerRegistration().close();
                    nodeRegistration221.getAccessDeOperationasNotificationListenerRegistration().close();
                    nodeRegistration221.getAccessDeviceNotificationListenerRegistration().close();
                    nodeRegistration221.getAccessTcaNotificationListenerRegistration().close();
                }
                break;
            default:
                break;
        }
    }

    private NodeRegistration registrateNode121Listeners(String nodeId, NotificationService notificationService) {
        final OrgOpenroadmAlarmListener alarmListener = new AlarmNotificationListener(this.dataBroker);
        LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
        final ListenerRegistration<OrgOpenroadmAlarmListener> accessAlarmNotificationListenerRegistration =
            notificationService.registerNotificationListener(alarmListener);

        final OrgOpenroadmDeOperationsListener deOperationsListener = new DeOperationsListener();
        LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
        final ListenerRegistration<OrgOpenroadmDeOperationsListener>
            accessDeOperationasNotificationListenerRegistration =
            notificationService.registerNotificationListener(deOperationsListener);

        final OrgOpenroadmDeviceListener deviceListener = new DeviceListener121(nodeId, this.portMapping);
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        final ListenerRegistration<OrgOpenroadmDeviceListener> accessDeviceNotificationListenerRegistration =
            notificationService.registerNotificationListener(deviceListener);

        TcaListener tcaListener = new TcaListener();
        LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
        final ListenerRegistration<OrgOpenroadmTcaListener> accessTcaNotificationListenerRegistration =
            notificationService.registerNotificationListener(tcaListener);
        return new NodeRegistration(nodeId, accessAlarmNotificationListenerRegistration,
            accessDeOperationasNotificationListenerRegistration, accessDeviceNotificationListenerRegistration,
            null, accessTcaNotificationListenerRegistration);
    }

    private NodeRegistration22 registrateNode221Listeners(String nodeId, NotificationService notificationService) {
        final org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev181019.OrgOpenroadmAlarmListener
            alarmListener = new AlarmNotificationListener221(dataBroker);
        LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
        final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev181019
            .OrgOpenroadmAlarmListener> accessAlarmNotificationListenerRegistration =
            notificationService.registerNotificationListener(alarmListener);

        final org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev181019
            .OrgOpenroadmDeOperationsListener deOperationsListener = new DeOperationsListener221();
        LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
        final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev181019
            .OrgOpenroadmDeOperationsListener> accessDeOperationasNotificationListenerRegistration =
            notificationService.registerNotificationListener(deOperationsListener);

        final org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceListener
            deviceListener = new DeviceListener221(nodeId, this.portMapping);
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
            .OrgOpenroadmDeviceListener> accessDeviceNotificationListenerRegistration =
            notificationService.registerNotificationListener(deviceListener);

        final org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev181019.OrgOpenroadmTcaListener
            tcaListener = new TcaListener221();
        LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
        final ListenerRegistration<org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev181019
            .OrgOpenroadmTcaListener> accessTcaNotificationListenerRegistration =
            notificationService.registerNotificationListener(tcaListener);
        return new NodeRegistration22(nodeId, accessAlarmNotificationListenerRegistration,
            accessDeOperationasNotificationListenerRegistration, accessDeviceNotificationListenerRegistration,
            null, accessTcaNotificationListenerRegistration);
    }

    private boolean subscribeStream(MountPoint mountPoint, String streamName, String nodeId) {
        final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class);
        if (!service.isPresent()) {
            return false;
        }
        final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
        if (rpcService == null) {
            LOG.error(RPC_SERVICE_FAILED, nodeId);
            return false;
        }
        final CreateSubscriptionInputBuilder createSubscriptionInputBuilder = new CreateSubscriptionInputBuilder()
            .setStream(new StreamNameType(streamName));
        LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
        ListenableFuture<RpcResult<CreateSubscriptionOutput>> subscription = rpcService
            .createSubscription(createSubscriptionInputBuilder.build());
        try {
            subscription.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error during subscription to stream {}", streamName, e);
        }
        return true;
    }
}
