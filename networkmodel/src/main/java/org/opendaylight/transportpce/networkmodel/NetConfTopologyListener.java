/*
 * Copyright © 2016 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.networkmodel.dto.NodeRegistration;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.Stream;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.ConnectionOper.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev231024.connection.oper.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev221225.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private final PortMapping portMapping;

    public NetConfTopologyListener(
            final NetworkModelService networkModelService,
            final DataBroker dataBroker,
            DeviceTransactionManager deviceTransactionManager,
            PortMapping portMapping) {
        this.networkModelService = networkModelService;
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.registrations = new ConcurrentHashMap<>();
        this.portMapping = portMapping;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
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
                    if (this.networkModelService.deleteOpenRoadmnode(nodeId)) {
                        onDeviceDisConnected(nodeId);
                        LOG.info("Device {} correctly disconnected from controller", nodeId);
                    }
                    break;
                case WRITE:
                    NetconfNode netconfNodeAfter = rootNode.getDataAfter().augmentation(NetconfNode.class);
                    if (ConnectionStatus.Connecting.equals(netconfNodeBefore.getConnectionStatus())
                            && ConnectionStatus.Connected.equals(netconfNodeAfter.getConnectionStatus())) {
                        LOG.info("Connecting Node: {}", nodeId);
                        Optional<AvailableCapability> deviceCapabilityOpt =
                            netconfNodeAfter.getAvailableCapabilities().getAvailableCapability().stream()
                                .filter(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME))
                                .sorted((c1, c2) -> c2.getCapability().compareTo(c1.getCapability()))
                                .findFirst();
                        if (deviceCapabilityOpt.isEmpty()) {
                            LOG.error("Unable to get openroadm-device-capability");
                            return;
                        }
                        this.networkModelService
                            .createOpenRoadmNode(nodeId, deviceCapabilityOpt.orElseThrow().getCapability());
                        onDeviceConnected(nodeId, deviceCapabilityOpt.orElseThrow().getCapability());
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
        if (mountPointOpt.isEmpty()) {
            LOG.error("Failed to get mount point for node {}", nodeId);
            return;
        }
        MountPoint mountPoint = mountPointOpt.orElseThrow();
        final Optional<NotificationService> notificationService = mountPoint.getService(NotificationService.class);
        if (notificationService.isEmpty()) {
            LOG.error(RPC_SERVICE_FAILED, nodeId);
            return;
        }
        NodeRegistration nodeRegistration =
            new NodeRegistration(
                nodeId, openRoadmVersion, notificationService.orElseThrow(), this.dataBroker, this.portMapping);
        nodeRegistration.registerListeners();
        registrations.put(nodeId, nodeRegistration);

        subscribeStream(mountPoint, nodeId);
    }

    private void onDeviceDisConnected(final String nodeId) {
        LOG.info("onDeviceDisConnected: {}", nodeId);
        this.registrations.remove(nodeId).unregisterListeners();
    }

    private boolean subscribeStream(MountPoint mountPoint, String nodeId) {
        final Optional<RpcConsumerRegistry> service = mountPoint.getService(RpcConsumerRegistry.class);
        if (service.isEmpty()) {
            return false;
        }
        final NotificationsService rpcService = service.orElseThrow().getRpcService(NotificationsService.class);
        if (rpcService == null) {
            LOG.error(RPC_SERVICE_FAILED, nodeId);
            return false;
        }
        // Set the default stream as OPENROADM
        for (String streamName : getSupportedStream(nodeId)) {
            LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
            ListenableFuture<RpcResult<CreateSubscriptionOutput>> subscription =
                rpcService.createSubscription(
                    new CreateSubscriptionInputBuilder().setStream(new StreamNameType(streamName)).build());
            if (checkSupportedStream(streamName, subscription)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public NetConfTopologyListener(
            final NetworkModelService networkModelService,
            final DataBroker dataBroker,
            DeviceTransactionManager deviceTransactionManager,
            PortMapping portMapping,
            Map<String, NodeRegistration> registrations) {
        this.networkModelService = networkModelService;
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.portMapping = portMapping;
        this.registrations = registrations;
    }

    private boolean checkSupportedStream(
            String streamName,
            ListenableFuture<RpcResult<CreateSubscriptionOutput>> subscription) {
        boolean subscriptionSuccessful = false;
        try {
            // Using if condition does not work, since we need to handle exceptions
            subscriptionSuccessful = subscription.get().isSuccessful();
            LOG.info("{} subscription is {}", streamName, subscriptionSuccessful);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error during subscription to stream {}", streamName, e);
        }
        return subscriptionSuccessful;
    }

    private List<String> getSupportedStream(String nodeId) {
        InstanceIdentifier<Streams> streamsIID = InstanceIdentifier.create(Netconf.class).child(Streams.class);
        Optional<Streams> ordmInfoObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, streamsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (ordmInfoObject == null || ordmInfoObject.isEmpty() || ordmInfoObject.orElseThrow().getStream().isEmpty()) {
            LOG.error("List of streams supports by device is not present");
            return List.of("OPENROADM","NETCONF");
        }
        List<String> streams = new ArrayList<>();
        List<String> netconfStreams = new ArrayList<>();
        for (Stream strm : ordmInfoObject.orElseThrow().getStream().values()) {
            LOG.debug("Streams are {}", strm);
            if ("OPENROADM".equalsIgnoreCase(strm.getName().getValue())) {
                streams.add(strm.getName().getValue());
            } else if ("NETCONF".equalsIgnoreCase(strm.getName().getValue())) {
                netconfStreams.add(strm.getName().getValue());
            }
        }
        // If OpenROADM streams are not supported, try NETCONF streams subscription
        streams.addAll(netconfStreams);
        return
            streams.isEmpty()
                ? List.of("OPENROADM","NETCONF")
                : streams;
    }

}
