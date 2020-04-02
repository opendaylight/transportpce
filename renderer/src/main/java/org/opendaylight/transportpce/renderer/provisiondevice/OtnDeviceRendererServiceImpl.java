/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.otn.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.OtnLinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtnDeviceRendererServiceImpl implements OtnDeviceRendererService {
    private static final String ODU2E = "-ODU2e-";
    private static final Logger LOG = LoggerFactory.getLogger(OtnDeviceRendererServiceImpl.class);
    private final OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private final CrossConnect crossConnect;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final DeviceTransactionManager deviceTransactionManager;
    private final NetworkModelService networkModelService;

    public OtnDeviceRendererServiceImpl(OpenRoadmInterfaceFactory openRoadmInterfaceFactory, CrossConnect crossConnect,
                                        OpenRoadmInterfaces openRoadmInterfaces,
                                        DeviceTransactionManager deviceTransactionManager,
                                        NetworkModelService networkModelService) {
        this.openRoadmInterfaceFactory = openRoadmInterfaceFactory;
        this.crossConnect = crossConnect;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.deviceTransactionManager = deviceTransactionManager;
        this.networkModelService = networkModelService;
    }

    @Override
    public OtnServicePathOutput setupOtnServicePath(OtnServicePathInput input) {
        LOG.info("Calling setup service path");
        boolean success = true;
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        List<String> results = new ArrayList<>();
        if (input.getServiceType() == null || input.getServiceRate() == null) {
            OtnServicePathOutputBuilder otnServicePathOutputBuilder = new OtnServicePathOutputBuilder()
                .setSuccess(false)
                .setResult("Error - service-type and service-rate must be presents");
            return otnServicePathOutputBuilder.build();
        }
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned = new CopyOnWriteArrayList<>();
        switch (input.getServiceType()) {
            case "Ethernet":
                if ("10G".equals(input.getServiceRate()) || "1G".equals(input.getServiceRate())) {
                    try {
                        LOG.info("Calling Node interfaces {} {} {} {} {} {} {}",
                            input.getServiceRate(), input.getEthernetEncoding(),
                            input.getServiceType(), input.getOperation(), input.getTribPortNumber(),
                            input.getTribSlot(), input.getNodes());
                        nodeInterfaces = createInterface(input);
                        LOG.info("Node interfaces created just fine ");

                        List<String> nodesToUpdate = new ArrayList<>();
                        if (!nodeInterfaces.isEmpty()) {
                            for (NodeInterface nodeInterf : nodeInterfaces) {
                                if (nodeInterf.getOduInterfaceId() != null) {
                                    List<String> interList = nodeInterf.getOduInterfaceId().stream()
                                        .filter(id -> id.contains("NETWORK")).collect(Collectors.toList());
                                    if (!interList.isEmpty()) {
                                        for (String inter : interList) {
                                            String tp = inter.split("-ODU")[0];
                                            String nodeTopo = nodeInterf.getNodeId() + "-" + tp.split("-")[0];
                                            nodesToUpdate.add(nodeTopo + "--" + tp);
                                        }
                                    }
                                }
                            }
                        }
                        updateOtnTopology(null, nodesToUpdate, input.getServiceRate(), input.getTribPortNumber(),
                            input.getTribSlot(), false);
                    } catch (OpenRoadmInterfaceException e) {
                        LOG.warn("Set up service path failed", e);
                        success = false;
                    }
                } else {
                    LOG.warn("Unsupported serivce-rate for service-type Ethernet");
                }
                break;
            case "ODU":
                if ("100G".equals(input.getServiceRate())) {
                    try {
                        createODU4TtpInterface(input, nodeInterfaces, otnNodesProvisioned);
                        updateOtnTopology(otnNodesProvisioned, null, null, null, null, false);
                    } catch (OpenRoadmInterfaceException e) {
                        LOG.warn("Set up service path failed", e);
                        success = false;
                    }
                } else {
                    LOG.warn("Unsupported serivce-rate for service-type ODU");
                }
                break;
            default:
                LOG.error("service-type {} not managet yet", input.getServiceType());
                break;
        }
        if (success) {
            LOG.info("Result is success");
            for (NodeInterface nodeInterface : nodeInterfaces) {
                results.add("Otn Service path was set up successfully for node :" + nodeInterface.getNodeId());
            }
        }
        OtnServicePathOutputBuilder otnServicePathOutputBuilder = new OtnServicePathOutputBuilder()
                .setSuccess(success)
                .setNodeInterface(nodeInterfaces)
                .setResult(String.join("\n", results));
        return otnServicePathOutputBuilder.build();
    }

    @Override
    public OtnServicePathOutput deleteOtnServicePath(OtnServicePathInput input) {
        List<Nodes> nodes = input.getNodes();
        AtomicBoolean success = new AtomicBoolean(true);
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        List<String> nodesTpToUpdate = new ArrayList<>();
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned = new CopyOnWriteArrayList<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask forkJoinTask = forkJoinPool.submit(() -> nodes.parallelStream().forEach(node -> {
            List<String> interfacesToDelete = new LinkedList<>();
            String nodeId = node.getNodeId();
            LOG.info("Deleting service setup on node {}", nodeId);
            String networkTp = node.getNetworkTp();
            if (networkTp == null || input.getServiceRate() == null || input.getServiceType() == null) {
                LOG.error("destination ({}) or service rate ({}) or service type ({}) is null.", networkTp,
                    input.getServiceRate(), input.getServiceType());
                return;
            }
            // if the node is currently mounted then proceed.
            if (this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                String connectionNumber = "";
                switch (input.getServiceRate()) {
                    case ("100G"):
                        if ("ODU".equals(input.getServiceType())) {
                            interfacesToDelete.add(networkTp + "-ODU4");
                            otnNodesProvisioned.add(node);
                            if (node.getNetwork2Tp() != null) {
                                interfacesToDelete.add(node.getNetwork2Tp() + "-ODU4");
                            }
                        }
                        break;
                    case ("10G"):
                        if (node.getClientTp() != null) {
                            connectionNumber = node.getClientTp() + ODU2E + input.getServiceName() + "-x-" + networkTp
                                + ODU2E + input.getServiceName();
                        } else if (node.getNetwork2Tp() != null) {
                            connectionNumber = networkTp + ODU2E + input.getServiceName() + "-x-" + node.getNetwork2Tp()
                                + ODU2E + input.getServiceName();
                        } else {
                            return;
                        }
                        break;
                    case ("1G"):
                        if (node.getClientTp() != null) {
                            connectionNumber = node.getClientTp() + "-ODU0-" + input.getServiceName() + "-x-"
                                + networkTp + "-ODU0-" + input.getServiceName();
                        } else if (node.getNetwork2Tp() != null) {
                            connectionNumber = networkTp + "-ODU0-" + input.getServiceName() + "-x-"
                                + node.getNetwork2Tp() + "-ODU0-" + input.getServiceName();
                        } else {
                            return;
                        }
                        break;
                    default:
                        LOG.error("service rate {} not managed yet", input.getServiceRate());
                        String result = input.getServiceRate() + " is not supported";
                        results.add(result);
                        success.set(false);
                        return;
                }
                List<String> intToDelete = this.crossConnect.deleteCrossConnect(nodeId, connectionNumber, true);
                if (intToDelete != null) {
                    for (String interf : intToDelete) {
                        if (!this.openRoadmInterfaceFactory.isUsedByOtnXc(nodeId, interf, connectionNumber,
                            this.deviceTransactionManager)) {
                            interfacesToDelete.add(interf);
                            if (!getSupportedInterface(nodeId, interf).contains("ODU4")) {
                                interfacesToDelete.add(getSupportedInterface(nodeId, interf));
                            }
                        }
                    }
                }
            } else {
                String result = nodeId + " is not mounted on the controller";
                results.add(result);
                success.set(false);
                LOG.warn(result);
                forkJoinPool.shutdown();
                return;
                // TODO should deletion end here?
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
            List<String> interList = interfacesToDelete.stream().filter(ele -> ele.contains("NETWORK"))
                .collect(Collectors.toList());
            if (!interList.isEmpty()) {
                for (String inter : interList) {
                    String tp = inter.split("-ODU")[0];
                    String nodeTopo = nodeId + "-" + tp.split("-")[0];
                    nodesTpToUpdate.add(nodeTopo + "--" + tp);
                }
            }
        }));
        try {
            forkJoinTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while deleting service paths!", e);
        }
        forkJoinPool.shutdown();
        LOG.info("requesting otn-topology update...");
        if (!nodesTpToUpdate.isEmpty() && !"ODU".equals(input.getServiceType())) {
            updateOtnTopology(null, nodesTpToUpdate, input.getServiceRate(), input.getTribPortNumber(),
                input.getTribSlot(), true);
        } else if (!otnNodesProvisioned.isEmpty()) {
            updateOtnTopology(otnNodesProvisioned, null, null, null, null, true);
        }

        OtnServicePathOutputBuilder delServBldr = new OtnServicePathOutputBuilder();
        delServBldr.setSuccess(success.get());
        if (results.isEmpty()) {
            return delServBldr.setResult("Request processed").build();
        } else {
            return delServBldr.setResult(String.join("\n", results)).build();
        }

    }

    private String getSupportedInterface(String nodeId, String interf) {
        Optional<Interface> supInterfOpt;
        try {
            supInterfOpt = this.openRoadmInterfaces.getInterface(nodeId, interf);
            if (supInterfOpt.isPresent()) {
                return supInterfOpt.get().getSupportingInterface();
            } else {
                return null;
            }
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("error getting Supported Interface of {} - {}", interf, nodeId, e);
            return null;
        }
    }

    private List<NodeInterface> createInterface(OtnServicePathInput input) throws OpenRoadmInterfaceException {
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        LOG.info("Calling Create Interface entry for OTN service path");
        if (input.getServiceRate() == null
            || !("1G".equals(input.getServiceRate()) || "10G".equals(input.getServiceRate()))) {
            LOG.error("Service rate {} not managed yet", input.getServiceRate());
        } else {
            createLowOrderInterfaces(input, nodeInterfaces);
        }
        return nodeInterfaces;
    }

    private Optional<String> postCrossConnect(List<String> createdOduInterfaces, Nodes node)
            throws OpenRoadmInterfaceException {
        return this.crossConnect.postOtnCrossConnect(createdOduInterfaces, node);
    }

    private void createLowOrderInterfaces(OtnServicePathInput input, List<NodeInterface> nodeInterfaces)
        throws OpenRoadmInterfaceException {
        for (Nodes node : input.getNodes()) {
            // check if the node is mounted or not?
            List<String> createdEthInterfaces = new ArrayList<>();
            List<String> createdOduInterfaces = new ArrayList<>();
            switch (input.getServiceRate()) {
                case ("1G"):
                    LOG.info("Input service is 1G");
                    if (node.getClientTp() != null) {
                        createdEthInterfaces.add(
                            openRoadmInterfaceFactory.createOpenRoadmEth1GInterface(node.getNodeId(),
                                node.getClientTp()));
                        createdOduInterfaces.add(
                            // suppporting interface?, payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(), node.getClientTp(),
                                input.getServiceName(), "07", false, input.getTribPortNumber(), input.getTribSlot()));
                    }
                    createdOduInterfaces.add(
                        openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(), node.getNetworkTp(),
                            input.getServiceName(), "07", true, input.getTribPortNumber(), input.getTribSlot()));
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), "07", true, input.getTribPortNumber(),
                                input.getTribSlot()));
                    }
                    break;
                case ("10G"):
                    LOG.info("Input service is 10G");
                    if (node.getClientTp() != null) {
                        createdEthInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmEth10GInterface(
                            node.getNodeId(), node.getClientTp()));
                        createdOduInterfaces.add(
                            // suppporting interface?, payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getClientTp(), input.getServiceName(), "03", false, input.getTribPortNumber(),
                                input.getTribSlot()));
                    }
                    createdOduInterfaces.add(
                        // supporting interface? payload ?
                        openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(), node.getNetworkTp(),
                            input.getServiceName(), "03", true, input.getTribPortNumber(), input.getTribSlot()));
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), "03", true, input.getTribPortNumber(),
                                input.getTribSlot()));
                    }
                    break;
                default:
                    LOG.error("service rate {} not managed yet", input.getServiceRate());
                    return;
            }

            // implement cross connect
            List<String> createdConnections = new ArrayList<>();
            if (!createdOduInterfaces.isEmpty()) {
                Optional<String> connectionNameOpt = postCrossConnect(createdOduInterfaces, node);
                createdConnections.add(connectionNameOpt.get());
                LOG.info("Created cross connects");
            }
            NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder()
                .withKey(new NodeInterfaceKey(node.getNodeId()))
                .setNodeId(node.getNodeId())
                .setConnectionId(createdConnections)
                .setEthInterfaceId(createdEthInterfaces)
                .setOduInterfaceId(createdOduInterfaces);
            nodeInterfaces.add(nodeInterfaceBuilder.build());
        }
    }

    private void createODU4TtpInterface(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned) throws OpenRoadmInterfaceException {
        for (Nodes node : input.getNodes()) {
            String supportingOtuInterface = node.getNetworkTp() + "-OTU";
            List<String> createdOdu4Interfaces = new ArrayList<>();
            createdOdu4Interfaces.add(openRoadmInterfaceFactory.createOpenRoadmOtnOdu4Interface(node.getNodeId(),
                node.getNetworkTp(), supportingOtuInterface));
            NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder()
                .withKey(new NodeInterfaceKey(node.getNodeId()))
                .setNodeId(node.getNodeId())
                .setOduInterfaceId(createdOdu4Interfaces);
            nodeInterfaces.add(nodeInterfaceBuilder.build());
            otnNodesProvisioned.add(node);
        }
    }

    private void updateOtnTopology(CopyOnWriteArrayList<Nodes> nodes, List<String> nodesTps, String serviceRate,
        Short tribPortNb, Short tribSlotNb, boolean isDeletion) {
        if (nodes != null && nodes.size() == 2) {
            if (isDeletion) {
                LOG.info("updating otn-topology removing ODU4 links");
                this.networkModelService.deleteOtnLinks(nodes.get(0).getNodeId(), nodes.get(0).getNetworkTp(),
                    nodes.get(1).getNodeId(), nodes.get(1).getNetworkTp(), OtnLinkType.ODTU4);
            } else {
                LOG.info("updating otn-topology adding ODU4 links");
                this.networkModelService.createOtnLinks(nodes.get(0).getNodeId(), nodes.get(0).getNetworkTp(),
                    nodes.get(1).getNodeId(), nodes.get(1).getNetworkTp(), OtnLinkType.ODTU4);
            }
        } else if (nodesTps != null && (nodesTps.size() % 2 == 0) && serviceRate != null && tribPortNb != null
            && tribSlotNb != null) {
            LOG.info("updating otn-topology node tps -tps and tpn pools");
            this.networkModelService.updateOtnLinks(nodesTps, serviceRate, tribPortNb, tribSlotNb, isDeletion);
        }
    }

}
