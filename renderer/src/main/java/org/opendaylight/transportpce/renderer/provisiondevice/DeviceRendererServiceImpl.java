/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServiceListTopology;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.ServiceNodelist;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.NodelistBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.NodelistKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.CreateOtsOmsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.renderer.rollback.output.FailedToRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.renderer.rollback.output.FailedToRollbackBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.renderer.rollback.output.FailedToRollbackKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.Topology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeviceRendererServiceImpl implements DeviceRendererService {
    private static final String IS_NOT_MOUNTED_ON_THE_CONTROLLER = " is not mounted on the controller";
    private static final String ODU4 = "-ODU4";
    private static final Logger LOG = LoggerFactory.getLogger(DeviceRendererServiceImpl.class);
    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final CrossConnect crossConnect;
    private final PortMapping portMapping;
    private final NetworkModelService networkModelService;

    public DeviceRendererServiceImpl(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
            OpenRoadmInterfaceFactory openRoadmInterfaceFactory, OpenRoadmInterfaces openRoadmInterfaces,
            CrossConnect crossConnect, PortMapping portMapping, NetworkModelService networkModelService) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaceFactory = openRoadmInterfaceFactory;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.crossConnect = crossConnect;
        this.portMapping = portMapping;
        this.networkModelService = networkModelService;
    }

    @Override
    public ServicePathOutput setupServicePath(ServicePathInput input, ServicePathDirection direction) {
        LOG.info("setup service path for input {} and direction {}", input, direction);
        List<Nodes> nodes = new ArrayList<>();
        if (input.getNodes() != null) {
            nodes.addAll(input.getNodes());
        }
        SpectrumInformation spectrumInformation = GridUtils.initSpectrumInformationFromServicePathInput(input);
        // Register node for suppressing alarms
        if (!alarmSuppressionNodeRegistration(input)) {
            LOG.warn("Alarm suppresion node registration failed!!!!");
        }
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        Map<NodeInterfaceKey,NodeInterface> nodeInterfaces = new ConcurrentHashMap<>();
        Set<String> nodesProvisioned = Sets.newConcurrentHashSet();
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned = new CopyOnWriteArrayList<>();
        ServiceListTopology topology = new ServiceListTopology();
        AtomicBoolean success = new AtomicBoolean(true);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask forkJoinTask = forkJoinPool.submit(() -> nodes.parallelStream().forEach(node -> {
            String nodeId = node.getNodeId();
            // take the index of the node
            int nodeIndex = nodes.indexOf(node);
            LOG.info("Starting provisioning for node : {}", nodeId);
            List<String> createdEthInterfaces = new ArrayList<>();
            List<String> createdOtuInterfaces = new ArrayList<>();
            List<String> createdOduInterfaces = new ArrayList<>();
            List<String> createdOchInterfaces = new ArrayList<>();
            List<String> createdConnections = new ArrayList<>();
            int crossConnectFlag = 0;
            try {
                // if the node is currently mounted then proceed
                if (this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                    String srcTp = node.getSrcTp();
                    String destTp = node.getDestTp();
                    if ((destTp != null) && destTp.contains(StringConstants.NETWORK_TOKEN)) {
                        LOG.info("Adding supporting OCH interface for node {}, dest tp {}, spectrumInformation {}",
                                nodeId, destTp, spectrumInformation);
                        crossConnectFlag++;
                        String supportingOchInterface = this.openRoadmInterfaceFactory.createOpenRoadmOchInterface(
                                nodeId, destTp, spectrumInformation);
                        createdOchInterfaces.add(supportingOchInterface);
                        // Here we pass logical connection-point of z-end to set SAPI and DAPI
                        Nodes tgtNode = null;
                        if (nodeIndex + 1 == nodes.size()) {
                            // For the end node, tgtNode becomes the first node in the list
                            tgtNode = nodes.get(0);
                        } else {
                            tgtNode = nodes.get(nodeIndex + 1);
                        }
                        // tgtNode srcTp is null in this if cond
                        String supportingOtuInterface = this.openRoadmInterfaceFactory
                                .createOpenRoadmOtu4Interface(nodeId, destTp, supportingOchInterface,
                                    tgtNode.getNodeId(), tgtNode.getDestTp());
                        createdOtuInterfaces.add(supportingOtuInterface);
                        if (srcTp == null) {
                            otnNodesProvisioned.add(node);
                        } else {
                            createdOduInterfaces.add(this.openRoadmInterfaceFactory.createOpenRoadmOdu4Interface(nodeId,
                                    destTp, supportingOtuInterface));
                        }
                    }
                    if ((srcTp != null) && srcTp.contains(StringConstants.CLIENT_TOKEN)) {
                        LOG.info("Adding supporting EThernet interface for node {}, src tp {}", nodeId, srcTp);
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Client Interfaces
                        createdEthInterfaces.add(
                            this.openRoadmInterfaceFactory.createOpenRoadmEthInterface(nodeId, srcTp));
                    }
                    if ((srcTp != null) && srcTp.contains(StringConstants.NETWORK_TOKEN)) {
                        LOG.info("Adding supporting OCH interface for node {}, src tp {}, spectrumInformation {}",
                                nodeId, srcTp, spectrumInformation);
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Line Interfaces
                        String supportingOchInterface = this.openRoadmInterfaceFactory.createOpenRoadmOchInterface(
                                nodeId, srcTp, spectrumInformation);
                        createdOchInterfaces.add(supportingOchInterface);
                        String supportingOtuInterface = this.openRoadmInterfaceFactory
                                .createOpenRoadmOtu4Interface(nodeId, srcTp, supportingOchInterface);
                        createdOtuInterfaces.add(supportingOtuInterface);
                        createdOduInterfaces.add(this.openRoadmInterfaceFactory.createOpenRoadmOdu4Interface(nodeId,
                                srcTp, supportingOtuInterface));
                        Mapping mapping = this.portMapping.getMapping(nodeId,srcTp);
                        if (mapping != null && mapping.getXponderType() != null
                            && (mapping.getXponderType().getIntValue() == 3
                            || mapping.getXponderType().getIntValue() == 2)) {
                            createdOduInterfaces.add(this.openRoadmInterfaceFactory
                                .createOpenRoadmOtnOdu4Interface(nodeId, destTp, supportingOtuInterface));
                        } else {
                            createdOduInterfaces.add(this.openRoadmInterfaceFactory.createOpenRoadmOdu4Interface(nodeId,
                                    destTp, supportingOtuInterface));
                        }
                    }
                    if ((destTp != null) && destTp.contains(StringConstants.CLIENT_TOKEN)) {
                        LOG.info("Adding supporting EThernet interface for node {}, dest tp {}", nodeId, destTp);
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Client Interfaces
                        createdEthInterfaces.add(
                            this.openRoadmInterfaceFactory.createOpenRoadmEthInterface(nodeId, destTp));
                    }
                    if ((srcTp != null) && (srcTp.contains(StringConstants.TTP_TOKEN)
                            || srcTp.contains(StringConstants.PP_TOKEN))) {
                        LOG.info("Adding supporting OCH interface for node {}, src tp {}, spectrumInformation {}",
                                nodeId, srcTp, spectrumInformation);
                        createdOchInterfaces.addAll(this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaces(nodeId,
                                srcTp, spectrumInformation));
                    }
                    if ((destTp != null) && (destTp.contains(StringConstants.TTP_TOKEN)
                            || destTp.contains(StringConstants.PP_TOKEN))) {
                        LOG.info("Adding supporting OCH interface for node {}, dest tp {}, spectrumInformation {}",
                                nodeId, destTp, spectrumInformation);
                        createdOchInterfaces.addAll(this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaces(nodeId,
                                destTp, spectrumInformation));
                    }
                    if (crossConnectFlag < 1) {
                        LOG.info("Creating cross connect between source {} and destination {} for node {}", srcTp,
                                destTp, nodeId);
                        Optional<String> connectionNameOpt =
                                this.crossConnect.postCrossConnect(nodeId, srcTp, destTp, spectrumInformation);
                        if (connectionNameOpt.isPresent()) {
                            nodesProvisioned.add(nodeId);
                            createdConnections.add(connectionNameOpt.get());
                        } else {
                            processErrorMessage("Unable to post Roadm-connection for node " + nodeId, forkJoinPool,
                                    results);
                            success.set(false);
                        }
                    }
                } else {
                    processErrorMessage(nodeId + IS_NOT_MOUNTED_ON_THE_CONTROLLER, forkJoinPool, results);
                    success.set(false);
                }
            } catch (OpenRoadmInterfaceException ex) {
                processErrorMessage("Setup service path failed! Exception:" + ex.toString(), forkJoinPool, results);
                success.set(false);
            }
            NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder()
                .withKey(new NodeInterfaceKey(nodeId))
                .setNodeId(nodeId)
                .setConnectionId(createdConnections)
                .setEthInterfaceId(createdEthInterfaces)
                .setOtuInterfaceId(createdOtuInterfaces)
                .setOduInterfaceId(createdOduInterfaces)
                .setOchInterfaceId(createdOchInterfaces);
            NodeInterface nodeInterface = nodeInterfaceBuilder.build();
            nodeInterfaces.put(nodeInterface.key(),nodeInterface);
        }));
        try {
            forkJoinTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while setting up service paths!", e);
        }
        forkJoinPool.shutdown();

        if (success.get()) {
            results.add("Roadm-connection successfully created for nodes: " + String.join(", ", nodesProvisioned));
        }
        // setting topology in the service list data store
        try {
            setTopologyForService(input.getServiceName(), topology.getTopology());
            updateOtnTopology(otnNodesProvisioned, false);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.warn("Failed to write topologies for service {}.", input.getServiceName(), e);
        }
        if (!alarmSuppressionNodeRemoval(input.getServiceName())) {
            LOG.error("Alarm suppresion node removal failed!!!!");
        }
        ServicePathOutputBuilder setServBldr = new ServicePathOutputBuilder()
            .setNodeInterface(nodeInterfaces)
            .setSuccess(success.get())
            .setResult(String.join("\n", results));
        return setServBldr.build();
    }

    private ConcurrentLinkedQueue<String> processErrorMessage(String message, ForkJoinPool forkJoinPool,
            ConcurrentLinkedQueue<String> messages) {
        LOG.warn("Received error message {}", message);
        messages.add(message);
        forkJoinPool.shutdown();
        return messages;
    }

    @Override
    public ServicePathOutput deleteServicePath(ServicePathInput input) {
        List<Nodes> nodes = input.getNodes();
        AtomicBoolean success = new AtomicBoolean(true);
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        if (!alarmSuppressionNodeRegistration(input)) {
            LOG.warn("Alarm suppresion node registraion failed!!!!");
        }
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned = new CopyOnWriteArrayList<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask forkJoinTask = forkJoinPool.submit(() -> nodes.parallelStream().forEach(node -> {
            List<String> interfacesToDelete = new LinkedList<>();
            String nodeId = node.getNodeId();
            LOG.info("Deleting service setup on node {}", nodeId);
            String srcTp;
            String destTp;
            if (node.getDestTp() == null) {
                LOG.error("Destination termination point must not be null.");
                return;
            } else {
                destTp = node.getDestTp();
            }
            if (node.getSrcTp() != null) {
                srcTp = node.getSrcTp();
            } else {
                srcTp = "";
                otnNodesProvisioned.add(node);
            }
            // if the node is currently mounted then proceed.
            if (this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                interfacesToDelete.addAll(getInterfaces2delete(nodeId, srcTp, destTp,
                        input.getLowerSpectralSlotNumber().intValue(),
                        input.getHigherSpectralSlotNumber().intValue()));
            } else {
                String result = nodeId + IS_NOT_MOUNTED_ON_THE_CONTROLLER;
                results.add(result);
                success.set(false);
                LOG.warn(result);
                forkJoinPool.shutdown();
                return;
                //TODO should deletion end here?
            }
            for (String interfaceId : interfacesToDelete) {
                try {
                    this.openRoadmInterfaces.deleteInterface(nodeId, interfaceId);
                } catch (OpenRoadmInterfaceException e) {
                    String result = String.format("Failed to delete interface %s on node %s!", interfaceId, nodeId);
                    success.set(false);
                    LOG.error(result, e);
                    results.add(result);
                }
            }
        }));
        try {
            forkJoinTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while deleting service paths!", e);
        }
        forkJoinPool.shutdown();
        updateOtnTopology(otnNodesProvisioned, true);
        if (!alarmSuppressionNodeRemoval(input.getServiceName())) {
            LOG.error("Alarm suppresion node removal failed!!!!");
        }
        ServicePathOutputBuilder delServBldr = new ServicePathOutputBuilder();
        delServBldr.setSuccess(success.get());
        if (results.isEmpty()) {
            return delServBldr.setResult("Request processed").build();
        } else {
            return delServBldr.setResult(String.join("\n", results)).build();
        }
    }

    private List<String>  getInterfaces2delete(
            String nodeId, String srcTp, String destTp, int lowerSpectralSlotNumber, int higherSpectralSlotNumber) {
        String spectralSlotName = String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber),
                String.valueOf(higherSpectralSlotNumber));
        List<String> interfacesToDelete = new LinkedList<>();
        if (destTp.contains(StringConstants.NETWORK_TOKEN)
                || srcTp.contains(StringConstants.CLIENT_TOKEN)
                || srcTp.contains(StringConstants.NETWORK_TOKEN)
                || destTp.contains(StringConstants.CLIENT_TOKEN)) {
            if (destTp.contains(StringConstants.NETWORK_TOKEN)) {
                try {
                    if (this.openRoadmInterfaces.getInterface(nodeId, destTp + "-ODU").isPresent()) {
                        interfacesToDelete.add(destTp + "-ODU");
                    }
                    if (this.openRoadmInterfaces.getInterface(nodeId, destTp + ODU4).isPresent()) {
                        interfacesToDelete.add(destTp + ODU4);
                    }
                }
                catch (OpenRoadmInterfaceException e) {
                    LOG.error("impossible to get interface {} or {}", destTp + "-ODU", destTp + ODU4, e);
                }
                interfacesToDelete.add(destTp + "-OTU");
                interfacesToDelete.add(
                        this.openRoadmInterfaceFactory
                        .createOpenRoadmOchInterfaceName(destTp,spectralSlotName));
            }
            if (srcTp.contains(StringConstants.NETWORK_TOKEN)) {
                interfacesToDelete.add(srcTp + "-ODU");
                interfacesToDelete.add(srcTp + "-OTU");
                interfacesToDelete
                        .add(this.openRoadmInterfaceFactory
                                .createOpenRoadmOchInterfaceName(srcTp,spectralSlotName));
            }
            if (srcTp.contains(StringConstants.CLIENT_TOKEN)) {
                interfacesToDelete.add(srcTp + "-ETHERNET");
            }
            if (destTp.contains(StringConstants.CLIENT_TOKEN)) {
                interfacesToDelete.add(destTp + "-ETHERNET");
            }
        } else {
            String connectionNumber = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, srcTp, destTp,
                    spectralSlotName);
            List<String> intToDelete = this.crossConnect.deleteCrossConnect(nodeId, connectionNumber, false);
            connectionNumber = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, destTp, srcTp, spectralSlotName);
            if (intToDelete != null) {
                for (String interf : intToDelete) {
                    if (!this.openRoadmInterfaceFactory.isUsedByXc(nodeId, interf, connectionNumber,
                        this.deviceTransactionManager)) {
                        interfacesToDelete.add(interf);
                    }
                }
            }
        }
        return interfacesToDelete;
    }

    @Override
    public RendererRollbackOutput rendererRollback(RendererRollbackInput input) {
        boolean success = true;
        Map<FailedToRollbackKey,FailedToRollback> failedToRollbackList = new HashMap<>();
        for (NodeInterface nodeInterfaces : input.nonnullNodeInterface().values()) {
            List<String> failedInterfaces = new ArrayList<>();
            String nodeId = nodeInterfaces.getNodeId();
            for (String connectionId : nodeInterfaces.getConnectionId()) {
                List<String> listInter = this.crossConnect.deleteCrossConnect(nodeId, connectionId, false);
                if (listInter != null) {
                    LOG.info("Cross connect {} on node {} successfully deleted.", connectionId, nodeId);
                } else {
                    LOG.error("Failed to delete cross connect {} on node {}!", connectionId, nodeId);
                    success = false;
                    failedInterfaces.add(connectionId);
                }
            }
            // Interfaces needs to be in specific order to delete. Order is:
            // 1. ODU interfaces
            // 2. OTU interfaces
            // 3. OCH interfaces
            // 4. ETH interfaces
            LinkedList<String> interfacesToDelete = new LinkedList<>();
            if (nodeInterfaces.getOduInterfaceId() != null) {
                interfacesToDelete.addAll(nodeInterfaces.getOduInterfaceId());
            }
            if (nodeInterfaces.getOtuInterfaceId() != null) {
                interfacesToDelete.addAll(nodeInterfaces.getOtuInterfaceId());
            }
            if (nodeInterfaces.getOchInterfaceId() != null) {
                interfacesToDelete.addAll(nodeInterfaces.getOchInterfaceId());
            }
            if (nodeInterfaces.getEthInterfaceId() != null) {
                interfacesToDelete.addAll(nodeInterfaces.getEthInterfaceId());
            }
            LOG.info("Going to execute rollback on node {}. Interfaces to rollback: {}", nodeId,
                    String.join(", ", interfacesToDelete));
            for (String interfaceId : interfacesToDelete) {
                try {
                    this.openRoadmInterfaces.deleteInterface(nodeId, interfaceId);
                    LOG.info("Interface {} on node {} successfully deleted.", interfaceId, nodeId);
                } catch (OpenRoadmInterfaceException e) {
                    LOG.error("Failed to delete interface {} on node {}!", interfaceId, nodeId);
                    success = false;
                    failedInterfaces.add(interfaceId);
                }
            }
            FailedToRollback failedToRollack = new FailedToRollbackBuilder().withKey(new FailedToRollbackKey(nodeId))
                    .setNodeId(nodeId).setInterface(failedInterfaces).build();
            failedToRollbackList.put(failedToRollack.key(),failedToRollack);
        }
        return new RendererRollbackOutputBuilder().setSuccess(success).setFailedToRollback(failedToRollbackList)
                .build();
    }

    private boolean alarmSuppressionNodeRegistration(ServicePathInput input) {
        NodelistBuilder nodeListBuilder = new NodelistBuilder()
            .withKey(new NodelistKey(input.getServiceName()))
            .setServiceName(input.getServiceName());
        Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service
            .nodelist.nodelist.NodesKey,
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service
            .nodelist.nodelist.Nodes> nodeList =
                new HashMap<>();
        if (input.getNodes() != null) {
            for (Nodes node : input.getNodes()) {
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
                    .service.nodelist.nodelist.Nodes nodes =
                        new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
                            .service.nodelist.nodelist.NodesBuilder().setNodeId(node.getNodeId()).build();
                nodeList.put(nodes.key(),nodes);
            }
        }
        nodeListBuilder.setNodes(nodeList);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
            .service.nodelist.Nodelist> nodeListIID =
                 InstanceIdentifier.create(ServiceNodelist.class)
                     .child(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
                         .service.nodelist.Nodelist.class, new NodelistKey(input.getServiceName()));
        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodeListIID, nodeListBuilder.build());
        FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();
        try {
            commit.get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            LOG.info("Nodes are register for alarm suppression for service: {}", input.getServiceName());
            return true;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            LOG.warn("Failed to alarm suppresslist for service: {}", input.getServiceName(), e);
            return false;
        }
    }

    private boolean alarmSuppressionNodeRemoval(String serviceName) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
            .service.nodelist.Nodelist> nodeListIID =
                InstanceIdentifier.create(ServiceNodelist.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
                        .service.nodelist.Nodelist.class, new NodelistKey(serviceName));
        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, nodeListIID);
        FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();
        try {
            commit.get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            LOG.info("Nodes are unregister for alarm suppression for service: {}", serviceName);
            return true;
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.warn("Failed to alarm suppresslist for service: {}", serviceName, e);
            return false;
        }
    }

    private void setTopologyForService(String name, Topology topo)
            throws InterruptedException, ExecutionException, TimeoutException {
        ServicesBuilder servicesBuilder;
        // Get the service from the service list inventory
        ServicesKey serviceKey = new ServicesKey(name);
        InstanceIdentifier<Services> iid =
                InstanceIdentifier.create(ServiceList.class).child(Services.class, serviceKey);
        Optional<Services> services;
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Future<java.util.Optional<Services>> future =
                    readTx.read(LogicalDatastoreType.OPERATIONAL, iid);
            services = future.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw e;
        }
        if (services.isPresent()) {
            LOG.info("service {} already exists", name);
            servicesBuilder = new ServicesBuilder(services.get()).setTopology(topo);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, servicesBuilder.build());
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } else {
            LOG.warn("Service {} does not exist - topology can not be updated", name);
        }
    }

    @Override
    public CreateOtsOmsOutput createOtsOms(CreateOtsOmsInput input) throws OpenRoadmInterfaceException {
        CreateOtsOmsOutputBuilder output = new CreateOtsOmsOutputBuilder();
        String result = "";
        Boolean success = false;
        // if the node is currently mounted then proceed.
        if (this.deviceTransactionManager.isDeviceMounted(input.getNodeId())) {
            Mapping oldMapping = null;
            Mapping newMapping = null;
            oldMapping = this.portMapping.getMapping(input.getNodeId(), input.getLogicalConnectionPoint());
            if (oldMapping != null) {
                String otsInterface =
                        this.openRoadmInterfaceFactory.createOpenRoadmOtsInterface(input.getNodeId(), oldMapping);
                newMapping = this.portMapping.getMapping(input.getNodeId(), input.getLogicalConnectionPoint());
                int count = 0;
                Boolean isSupportingOtsPresent = isSupportingOtsPresent(newMapping);
                while (!isSupportingOtsPresent && (count < 6)) {
                    LOG.info("waiting for post interface operation on node '{}'...", input.getNodeId());
                    try {
                        Thread.sleep(10000);
                        this.portMapping.updateMapping(input.getNodeId(), oldMapping);
                    } catch (InterruptedException e) {
                        LOG.error("Failed to wait for post interface operation ");
                    }
                    newMapping = this.portMapping.getMapping(input.getNodeId(), input.getLogicalConnectionPoint());
                    isSupportingOtsPresent = isSupportingOtsPresent(newMapping);
                    count++;
                }
                if (count < 6) {
                    String omsInterface =
                            this.openRoadmInterfaceFactory.createOpenRoadmOmsInterface(input.getNodeId(), newMapping);
                    if (omsInterface != null) {
                        result = "Interfaces " + otsInterface + " - " + omsInterface + " successfully created on node "
                                + input.getNodeId();
                        success = true;
                    } else {
                        LOG.error("Fail to create OpenRoadmOms Interface for node '{}'", input.getNodeId());
                        result = "Fail to create OpenRoadmOms Interface for node : " + input.getNodeId();
                    }
                } else {
                    LOG.error("Unable to get ots interface from mapping {} for node {}",
                            oldMapping.getLogicalConnectionPoint(), input.getNodeId());
                    result = String.format("Unable to get ots interface from mapping %s - %s",
                            oldMapping.getLogicalConnectionPoint(), input.getNodeId());
                }
            } else {
                result = "Logical Connection point " + input.getLogicalConnectionPoint() + " does not exist for "
                        + input.getNodeId();
            }
        } else {
            result = input.getNodeId() + IS_NOT_MOUNTED_ON_THE_CONTROLLER;
            LOG.warn("{} is not mounted on the controller",input.getNodeId());
        }
        return output.setResult(result).setSuccess(success).build();
    }

    private Boolean isSupportingOtsPresent(Mapping mapping) {
        Boolean result = false;
        if (mapping != null) {
            if (mapping.getSupportingOts() != null) {
                LOG.info("SupportingOts info is present in mapping {}", mapping);
                result = true;
            } else {
                LOG.warn("SupportingOts info not present in mapping {}", mapping);
            }
        }
        return result;
    }

    private void updateOtnTopology(CopyOnWriteArrayList<Nodes> nodes, boolean isDeletion) {
        if (nodes.size() != 2) {
            LOG.error("Error with OTU4 links to update in otn-topology");
            return;
        }
        if (isDeletion) {
            LOG.info("updating otn-topology removing OTU4 links");
            this.networkModelService.deleteOtnLinks(nodes.get(0).getNodeId(), nodes.get(0).getDestTp(),
                nodes.get(1).getNodeId(), nodes.get(1).getDestTp(), OtnLinkType.OTU4);
        } else {
            LOG.info("updating otn-topology adding OTU4 links");
            this.networkModelService.createOtnLinks(nodes.get(0).getNodeId(), nodes.get(0).getDestTp(),
                nodes.get(1).getNodeId(), nodes.get(1).getDestTp(), OtnLinkType.OTU4);
        }
    }
}
