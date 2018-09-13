/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServiceListTopology;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.ServiceNodelist;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.NodelistBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.NodelistKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.CreateOtsOmsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.RendererRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.renderer.rollback.output.FailedToRollback;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.renderer.rollback.output.FailedToRollbackBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.renderer.rollback.output.FailedToRollbackKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.Topology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.R100G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.Nodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRendererServiceImpl implements DeviceRendererService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceRendererServiceImpl.class);
    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final CrossConnect crossConnect;
    private final PortMapping portMapping;

    public DeviceRendererServiceImpl(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
            OpenRoadmInterfaceFactory openRoadmInterfaceFactory, OpenRoadmInterfaces openRoadmInterfaces,
            CrossConnect crossConnect, PortMapping portMapping) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaceFactory = openRoadmInterfaceFactory;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.crossConnect = crossConnect;
        this.portMapping = portMapping;
    }

    @Override
    public ServicePathOutput setupServicePath(ServicePathInput input, ServicePathDirection direction) {
        List<Nodes> nodes = input.getNodes();
        // Register node for suppressing alarms
        if (!alarmSuppressionNodeRegistration(input)) {
            LOG.warn("Alarm suppresion node registration failed!!!!");
        }
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        Set<NodeInterface> nodeInterfaces = Sets.newConcurrentHashSet();
        Set<String> nodesProvisioned = Sets.newConcurrentHashSet();
        ServiceListTopology topology = new ServiceListTopology();
        AtomicBoolean success = new AtomicBoolean(true);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask forkJoinTask = forkJoinPool.submit(() -> nodes.parallelStream().forEach(node -> {
            String nodeId = node.getNodeId();
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
                    Long waveNumber = input.getWaveNumber();
                    if ((destTp != null) && destTp.contains(OpenRoadmInterfacesImpl.NETWORK_TOKEN)) {
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Line Interfaces
                        String supportingOchInterface = this.openRoadmInterfaceFactory.createOpenRoadmOchInterface(
                                nodeId, destTp, waveNumber, R100G.class, ModulationFormat.DpQpsk);
                        createdOchInterfaces.add(supportingOchInterface);
                        String supportingOtuInterface = this.openRoadmInterfaceFactory
                                .createOpenRoadmOtu4Interface(nodeId, destTp, supportingOchInterface);
                        createdOtuInterfaces.add(supportingOtuInterface);
                        createdOduInterfaces.add(this.openRoadmInterfaceFactory.createOpenRoadmOdu4Interface(nodeId,
                                destTp, supportingOtuInterface));
                    }
                    if ((srcTp != null) && srcTp.contains(OpenRoadmInterfacesImpl.CLIENT_TOKEN)) {
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Client Interfaces
                        createdEthInterfaces.add(
                            this.openRoadmInterfaceFactory.createOpenRoadmEthInterface(nodeId, srcTp));
                    }
                    if ((srcTp != null) && srcTp.contains(OpenRoadmInterfacesImpl.NETWORK_TOKEN)) {
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Line Interfaces
                        String supportingOchInterface = this.openRoadmInterfaceFactory.createOpenRoadmOchInterface(
                                nodeId, srcTp, waveNumber, R100G.class, ModulationFormat.DpQpsk);
                        createdOchInterfaces.add(supportingOchInterface);
                        String supportingOtuInterface = this.openRoadmInterfaceFactory
                                .createOpenRoadmOtu4Interface(nodeId, srcTp, supportingOchInterface);
                        createdOtuInterfaces.add(supportingOtuInterface);
                        createdOduInterfaces.add(this.openRoadmInterfaceFactory.createOpenRoadmOdu4Interface(nodeId,
                                srcTp, supportingOtuInterface));
                    }
                    if ((destTp != null) && destTp.contains(OpenRoadmInterfacesImpl.CLIENT_TOKEN)) {
                        crossConnectFlag++;
                        // create OpenRoadm Xponder Client Interfaces
                        createdEthInterfaces.add(
                            this.openRoadmInterfaceFactory.createOpenRoadmEthInterface(nodeId, destTp));
                    }
                    if ((srcTp != null) && (srcTp.contains(OpenRoadmInterfacesImpl.TTP_TOKEN)
                            || srcTp.contains(OpenRoadmInterfacesImpl.PP_TOKEN))) {
                        createdOchInterfaces.add(
                            this.openRoadmInterfaceFactory
                                .createOpenRoadmOchInterface(nodeId, srcTp, waveNumber));
                    }
                    if ((destTp != null) && (destTp.contains(OpenRoadmInterfacesImpl.TTP_TOKEN)
                            || destTp.contains(OpenRoadmInterfacesImpl.PP_TOKEN))) {
                        createdOchInterfaces.add(
                            this.openRoadmInterfaceFactory
                                .createOpenRoadmOchInterface(nodeId, destTp, waveNumber));
                    }
                    if (crossConnectFlag < 1) {
                        LOG.info("Creating cross connect between source {} and destination {} for node {}", srcTp,
                                destTp, nodeId);
                        Optional<String> connectionNameOpt =
                                this.crossConnect.postCrossConnect(nodeId, waveNumber, srcTp, destTp);
                        if (connectionNameOpt.isPresent()) {
                            nodesProvisioned.add(nodeId);
                            List<Ports> ports =
                                    this.crossConnect.getConnectionPortTrail(nodeId, waveNumber, srcTp, destTp);
                            if (ServicePathDirection.A_TO_Z.equals(direction)) {
                                topology.updateAtoZTopologyList(ports, nodeId);
                            }
                            if (ServicePathDirection.Z_TO_A.equals(direction)) {
                                topology.updateZtoATopologyList(ports, nodeId);
                            }
                            createdConnections.add(connectionNameOpt.get());
                        } else {
                            processErrorMessage("Unable to post Roadm-connection for node " + nodeId, forkJoinPool,
                                    results);
                            success.set(false);
                        }
                    }
                } else {
                    processErrorMessage(nodeId + " is not mounted on the controller", forkJoinPool, results);
                    success.set(false);
                }
            } catch (OpenRoadmInterfaceException ex) {
                processErrorMessage("Setup service path failed! Exception:" + ex.toString(), forkJoinPool, results);
                success.set(false);
            }
            NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder();
            nodeInterfaceBuilder.withKey(new NodeInterfaceKey(nodeId));
            nodeInterfaceBuilder.setNodeId(nodeId);
            nodeInterfaceBuilder.setConnectionId(createdConnections);
            nodeInterfaceBuilder.setEthInterfaceId(createdEthInterfaces);
            nodeInterfaceBuilder.setOtuInterfaceId(createdOtuInterfaces);
            nodeInterfaceBuilder.setOduInterfaceId(createdOduInterfaces);
            nodeInterfaceBuilder.setOchInterfaceId(createdOchInterfaces);
            nodeInterfaces.add(nodeInterfaceBuilder.build());
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
        ServicePathOutputBuilder setServBldr = new ServicePathOutputBuilder();
        setServBldr.setNodeInterface(new ArrayList<>(nodeInterfaces));
        setServBldr.setSuccess(success.get());
        setServBldr.setResult(String.join("\n", results));
        // setting topology in the service list data store
        try {
            setTopologyForService(input.getServiceName(), topology.getTopology());
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LOG.warn("Failed to write topologies for service {}.", input.getServiceName(), e);
        }
        if (!alarmSuppressionNodeRemoval(input.getServiceName())) {
            LOG.error("Alarm suppresion node removal failed!!!!");
        }
        return setServBldr.build();
    }

    private ConcurrentLinkedQueue<String> processErrorMessage(String message, ForkJoinPool forkJoinPool,
            ConcurrentLinkedQueue<String> messages) {
        LOG.warn(message);
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
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask forkJoinTask = forkJoinPool.submit(() -> nodes.parallelStream().forEach(node -> {
            List<String> interfacesToDelete = new LinkedList<>();
            String nodeId = node.getNodeId();
            LOG.info("Deleting service setup on node {}", nodeId);
            String srcTp = node.getSrcTp();
            String destTp = node.getDestTp();
            Long waveNumber = input.getWaveNumber();
            if ((srcTp == null) || (destTp == null)) {
                LOG.error("Source ({}) or destination ({}) termination point is null.", srcTp, destTp);
                return;
            }
            // if the node is currently mounted then proceed.
            if (this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                if (destTp.contains(OpenRoadmInterfacesImpl.NETWORK_TOKEN)
                        || srcTp.contains(OpenRoadmInterfacesImpl.CLIENT_TOKEN)
                        || srcTp.contains(OpenRoadmInterfacesImpl.NETWORK_TOKEN)
                        || destTp.contains(OpenRoadmInterfacesImpl.CLIENT_TOKEN)) {
                    if (destTp.contains(OpenRoadmInterfacesImpl.NETWORK_TOKEN)) {
                        interfacesToDelete.add(destTp + "-ODU");
                        interfacesToDelete.add(destTp + "-OTU");
                        interfacesToDelete.add(
                                this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaceName(destTp, waveNumber));
                    }
                    if (srcTp.contains(OpenRoadmInterfacesImpl.NETWORK_TOKEN)) {
                        interfacesToDelete.add(srcTp + "-ODU");
                        interfacesToDelete.add(srcTp + "-OTU");
                        interfacesToDelete
                                .add(this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaceName(srcTp, waveNumber));
                    }
                    if (srcTp.contains(OpenRoadmInterfacesImpl.CLIENT_TOKEN)) {
                        interfacesToDelete.add(srcTp + "-ETHERNET");
                    }
                    if (destTp.contains(OpenRoadmInterfacesImpl.CLIENT_TOKEN)) {
                        interfacesToDelete.add(destTp + "-ETHERNET");
                    }
                } else {
                    String connectionNumber = srcTp + "-" + destTp + "-" + waveNumber;
                    if (!this.crossConnect.deleteCrossConnect(nodeId, connectionNumber)) {
                        LOG.error("Failed to delete cross connect {}", connectionNumber);
                    }
                    connectionNumber = destTp + "-" + srcTp + "-" + waveNumber;
                    String interfName =
                            this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaceName(srcTp, waveNumber);
                    if (!isUsedByXc(nodeId, interfName, connectionNumber)) {
                        interfacesToDelete.add(interfName);
                    }
                    interfName = this.openRoadmInterfaceFactory.createOpenRoadmOchInterfaceName(destTp, waveNumber);
                    if (!isUsedByXc(nodeId, interfName, connectionNumber)) {
                        interfacesToDelete.add(interfName);
                    }
                }
            } else {
                String result = nodeId + " is not mounted on the controller";
                results.add(result);
                success.set(false);
                LOG.warn(result);
                forkJoinPool.shutdown();
                return; // TODO should deletion end here?
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

    @Override
    public RendererRollbackOutput rendererRollback(RendererRollbackInput input) {
        boolean success = true;
        List<FailedToRollback> failedToRollbackList = new ArrayList<>();
        for (NodeInterface nodeInterfaces : input.getNodeInterface()) {
            List<String> failedInterfaces = new ArrayList<>();
            String nodeId = nodeInterfaces.getNodeId();
            for (String connectionId : nodeInterfaces.getConnectionId()) {
                if (this.crossConnect.deleteCrossConnect(nodeId, connectionId)) {
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
            failedToRollbackList.add(new FailedToRollbackBuilder().withKey(new FailedToRollbackKey(nodeId))
                    .setNodeId(nodeId).setInterface(failedInterfaces).build());
        }
        return new RendererRollbackOutputBuilder().setSuccess(success).setFailedToRollback(failedToRollbackList)
                .build();
    }

    private boolean alarmSuppressionNodeRegistration(ServicePathInput input) {
        NodelistBuilder nodeListBuilder = new NodelistBuilder();
        nodeListBuilder.withKey(new NodelistKey(input.getServiceName()));
        nodeListBuilder.setServiceName(input.getServiceName());
        List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service
            .nodelist.nodelist.Nodes> nodeList =
                new ArrayList<>();
        for (Nodes node : input.getNodes()) {
            nodeList.add(
                    new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
                    .service.nodelist.nodelist.NodesBuilder()
                            .setNodeId(node.getNodeId()).build());
        }
        nodeListBuilder.setNodes(nodeList);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
            .service.nodelist.Nodelist> nodeListIID =
                        InstanceIdentifier.create(ServiceNodelist.class).child(
                                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression
                                    .rev171102.service.nodelist.Nodelist.class,
                                new NodelistKey(input.getServiceName()));
        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodeListIID, nodeListBuilder.build());
        Future<Void> submit = writeTransaction.submit();
        try {
            submit.get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
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
                        InstanceIdentifier.create(ServiceNodelist.class).child(
                                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression
                                    .rev171102.service.nodelist.Nodelist.class,
                                new NodelistKey(serviceName));
        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, nodeListIID);
        Future<Void> submit = writeTransaction.submit();
        try {
            submit.get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
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
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Future<com.google.common.base.Optional<Services>> future =
                    readTx.read(LogicalDatastoreType.OPERATIONAL, iid);
            services = future.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS).toJavaUtil();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw e;
        }
        if (services.isPresent()) {
            servicesBuilder = new ServicesBuilder(services.get());
        } else {
            servicesBuilder = new ServicesBuilder();
            servicesBuilder.withKey(serviceKey);
        }
        servicesBuilder.setTopology(topo);
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, servicesBuilder.build());
        writeTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
    }

    private boolean isUsedByXc(String nodeid, String interfaceid, String xcid) {
        InstanceIdentifier<RoadmConnections> xciid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(xcid));
        LOG.info("reading xc {} in node {}", xcid, nodeid);
        Optional<RoadmConnections> crossconnection =
                this.deviceTransactionManager.getDataFromDevice(nodeid, LogicalDatastoreType.CONFIGURATION, xciid,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (crossconnection.isPresent()) {
            RoadmConnections xc = crossconnection.get();
            LOG.info("xd {} found", xcid);
            if (xc.getSource().getSrcIf().equals(interfaceid) || xc.getDestination().getDstIf().equals(interfaceid)) {
                return true;
            }
        } else {
            LOG.info("xd {} not found !", xcid);
        }
        return false;
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
            result = input.getNodeId() + " is not mounted on the controller";
            LOG.warn(result);
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
}
