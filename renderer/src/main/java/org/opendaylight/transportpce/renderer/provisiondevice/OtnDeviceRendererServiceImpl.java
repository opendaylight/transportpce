/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.otn.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OtnDeviceRendererServiceImpl implements OtnDeviceRendererService {
    private static final Logger LOG = LoggerFactory.getLogger(OtnDeviceRendererServiceImpl.class);
    private static final String PT_03 = "03";
    private static final String PT_07 = "07";
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
        LOG.info("Calling setup otn-service path");
        boolean success = true;
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        List<String> results = new ArrayList<>();
        if (input.getServiceFormat() == null || input.getServiceRate() == null) {
            OtnServicePathOutputBuilder otnServicePathOutputBuilder = new OtnServicePathOutputBuilder()
                .setSuccess(false)
                .setResult("Error - service-type and service-rate must be presents");
            return otnServicePathOutputBuilder.build();
        }
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned = new CopyOnWriteArrayList<>();
        switch (input.getServiceFormat()) {
            case "Ethernet":
                if (input.getServiceRate().intValue() == 1 || input.getServiceRate().intValue() == 10) {
                    try {
                        LOG.info("Calling Node interfaces {} {} {} {} {} {} {}",
                            input.getServiceRate(), input.getEthernetEncoding(),
                            input.getServiceFormat(), input.getOperation(), input.getTribPortNumber(),
                            input.getTribSlot(), input.getNodes());
                        nodeInterfaces = createInterface(input);
                        LOG.info("Node interfaces created just fine ");

                        List<String> nodesToUpdate = updateOduNodes(nodeInterfaces, "ODU");
                        updateOtnTopology(null, nodesToUpdate, input.getServiceRate(), input.getTribPortNumber(),
                            input.getTribSlot(), false);
                    } catch (OpenRoadmInterfaceException e) {
                        LOG.warn("Set up service path failed", e);
                        success = false;
                    }
                } else if (input.getServiceRate().intValue() == 100) {
                    try {
                        LOG.info("Calling Node interfaces {} {} {} {} {} {}",
                            input.getServiceRate(), input.getEthernetEncoding(),
                            input.getServiceFormat(), input.getOperation(), input.getOpucnTribSlots(),
                            input.getNodes());
                        nodeInterfaces = createInterface(input);
                        LOG.info("Node interfaces created just fine for 100G OTN ");
                        // TODO: Update the OTN topology accordingly with Opucn-Trib-slots
                        // List<String> nodesToUpdate = updateOduNodes(nodeInterfaces, "ODUC4");
                        // updateOtnTopology(null, nodesToUpdate, input.getServiceRate(), input.getTribPortNumber(),
                        //    input.getTribSlot(), false);
                    } catch (OpenRoadmInterfaceException e) {
                        LOG.warn("Set up service path failed", e);
                        success = false;
                    }
                } else {
                    LOG.warn("Unsupported service-rate for service-type Ethernet");
                }
                break;
            case "ODU":
                if (input.getServiceRate().intValue() == 100) {
                    try {
                        createODU4TtpInterface(input, nodeInterfaces, otnNodesProvisioned);
                        updateOtnTopology(otnNodesProvisioned, null, null, null, null, false);
                    } catch (OpenRoadmInterfaceException e) {
                        LOG.warn("Set up service path failed", e);
                        success = false;
                    }
                } else if (input.getServiceRate().intValue() == 400) {
                    try {
                        createOduc4TtpInterface(input, nodeInterfaces, otnNodesProvisioned);
                        updateOtnTopology(otnNodesProvisioned, null, null, null, null, false);
                    } catch (OpenRoadmInterfaceException e) {
                        LOG.warn("Set up service path failed", e);
                        success = false;
                    }
                } else {
                    LOG.warn("Unsupported service-rate for service-type ODU");
                }
                break;
            default:
                LOG.error("service-type {} not managed yet", input.getServiceFormat());
                break;
        }
        if (success) {
            LOG.info("Result is success");
            for (NodeInterface nodeInterface : nodeInterfaces) {
                results.add("Otn Service path was set up successfully for node :" + nodeInterface.getNodeId());
            }
        }
        Map<NodeInterfaceKey,NodeInterface> nodeInterfacesMap = new HashMap<>();
        for (NodeInterface nodeInterface : nodeInterfaces) {
            if (nodeInterface != null) {
                nodeInterfacesMap.put(nodeInterface.key(), nodeInterface);
            }
        }
        OtnServicePathOutputBuilder otnServicePathOutputBuilder = new OtnServicePathOutputBuilder()
                .setSuccess(success)
                .setNodeInterface(nodeInterfacesMap)
                .setResult(String.join("\n", results));
        return otnServicePathOutputBuilder.build();
    }

    public OtnServicePathOutput deleteOtnServicePath(OtnServicePathInput input) {
        if (input == null) {
            LOG.error("Unable to delete otn service path. input = null");
            return new OtnServicePathOutputBuilder().setResult("Unable to delete otn service path. input = null")
                .setSuccess(false).build();
        }
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
            if (networkTp == null || input.getServiceRate() == null || input.getServiceFormat() == null) {
                LOG.error("destination ({}) or service-rate ({}) or service-format ({}) is null.", networkTp,
                    input.getServiceRate(), input.getServiceFormat());
                return;
            }
            // if the node is currently mounted then proceed.
            if (this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                String connectionNumber = "";
                switch (input.getServiceRate().intValue()) {
                    case 100:
                        if ("ODU".equals(input.getServiceFormat())) {
                            interfacesToDelete.add(networkTp + "-ODU4");
                            otnNodesProvisioned.add(node);
                            if (node.getNetwork2Tp() != null) {
                                interfacesToDelete.add(node.getNetwork2Tp() + "-ODU4");
                            }
                        } else if ("Ethernet".equals(input.getServiceFormat())) {
                            connectionNumber = getConnectionNumber(input.getServiceName(), node, networkTp, "ODU4");
                        }
                        break;
                    case 400:
                        if ("ODU".equals(input.getServiceFormat())) {
                            interfacesToDelete.add(networkTp + "-ODUC4");
                            otnNodesProvisioned.add(node);
                            if (node.getNetwork2Tp() != null) {
                                interfacesToDelete.add(node.getNetwork2Tp() + "-ODUC4");
                            }
                        }
                        break;
                    case 10:
                        connectionNumber = getConnectionNumber(input.getServiceName(), node, networkTp, "ODU2e");
                        break;
                    case 1:
                        connectionNumber = getConnectionNumber(input.getServiceName(), node, networkTp, "ODU0");
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
                            String supportedInterface = this.openRoadmInterfaces.getSupportedInterface(nodeId, interf);
                            if (input.getServiceRate().intValue() == 100) {
                                if (!supportedInterface.contains("ODUC4")) {
                                    interfacesToDelete.add(supportedInterface);
                                }
                            } else {
                                if (!supportedInterface.contains("ODU4")) {
                                    interfacesToDelete.add(supportedInterface);
                                }
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
        if (!nodesTpToUpdate.isEmpty() && !"ODU".equals(input.getServiceFormat())) {
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

    private String getConnectionNumber(String serviceName, Nodes node, String networkTp, String oduType) {
        if (node.getClientTp() != null) {
            return String.join("-", node.getClientTp(), oduType, serviceName, "x", networkTp, oduType, serviceName);
        } else if (node.getNetwork2Tp() != null) {
            return String.join("-", networkTp, oduType, serviceName, "x", node.getNetwork2Tp(), oduType, serviceName);
        } else {
            return "";
        }
    }

    private List<String> updateOduNodes(List<NodeInterface> nodeInterfaceList, String deLimiter) {
        List<String> nodesToUpdate = new ArrayList<>();
        if (!(deLimiter.equals("ODU")) || !(deLimiter.equals("ODUC4"))) {
            LOG.error("ODU node list update will be incorrect");
        }

        if (!nodeInterfaceList.isEmpty()) {
            for (NodeInterface nodeInterf : nodeInterfaceList) {
                if (nodeInterf.getOduInterfaceId() != null) {
                    List<String> interList = nodeInterf.getOduInterfaceId().stream()
                        .filter(id -> id.contains("NETWORK")).collect(Collectors.toList());
                    if (!interList.isEmpty()) {
                        for (String inter : interList) {
                            String tp = inter.split("-" + deLimiter)[0];
                            String nodeTopo = nodeInterf.getNodeId() + "-" + tp.split("-")[0];
                            nodesToUpdate.add(nodeTopo + "--" + tp);
                        }
                    }
                }
            }
        }

        return nodesToUpdate;
    }

    private List<NodeInterface> createInterface(OtnServicePathInput input) throws OpenRoadmInterfaceException {
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        LOG.info("Calling Create Interface entry for OTN service path");
        if (input.getServiceRate() == null
            || !(input.getServiceRate().intValue() == 1 || input.getServiceRate().intValue() == 10
                || input.getServiceRate().intValue() == 100)) {
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
            switch (input.getServiceRate().intValue()) {
                case 1:
                    LOG.info("Input service is 1G");
                    if (node.getClientTp() != null) {
                        createdEthInterfaces.add(
                            openRoadmInterfaceFactory.createOpenRoadmEth1GInterface(node.getNodeId(),
                                node.getClientTp()));
                        createdOduInterfaces.add(
                            // suppporting interface?, payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(), node.getClientTp(),
                                input.getServiceName(), PT_07, false, input.getTribPortNumber(), input.getTribSlot()));
                    }
                    createdOduInterfaces.add(
                        openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(), node.getNetworkTp(),
                            input.getServiceName(), PT_07, true, input.getTribPortNumber(), input.getTribSlot()));
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), PT_07, true, input.getTribPortNumber(),
                                input.getTribSlot()));
                    }
                    break;
                case 10:
                    LOG.info("Input service is 10G");
                    if (node.getClientTp() != null) {
                        createdEthInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmEth10GInterface(
                            node.getNodeId(), node.getClientTp()));
                        createdOduInterfaces.add(
                            // suppporting interface?, payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getClientTp(), input.getServiceName(), PT_03, false, input.getTribPortNumber(),
                                input.getTribSlot()));
                    }
                    createdOduInterfaces.add(
                        // supporting interface? payload ?
                        openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(), node.getNetworkTp(),
                            input.getServiceName(), PT_03, true, input.getTribPortNumber(), input.getTribSlot()));
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), PT_03, true, input.getTribPortNumber(),
                                input.getTribSlot()));
                    }
                    break;
                case 100:
                    LOG.info("Input service is 100G");
                    // Take the first and last value in the list of OpucnTribSlot (assuming SH would provide
                    // min and max value only, size two)
                    OpucnTribSlotDef minOpucnTs = OpucnTribSlotDef.getDefaultInstance(
                        input.getOpucnTribSlots().get(0).getValue());
                    OpucnTribSlotDef maxOpucnTs = OpucnTribSlotDef.getDefaultInstance(
                        input.getOpucnTribSlots().get(1).getValue());
                    if (node.getClientTp() != null) {
                        createdEthInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmEth100GInterface(
                            node.getNodeId(), node.getClientTp()));
                        // OPUCn trib information is optional when creating ODU4 ethernet (client) interface
                        createdOduInterfaces.add(
                            openRoadmInterfaceFactory.createOpenRoadmOtnOdu4LoInterface(node.getNodeId(),
                            node.getClientTp(), input.getServiceName(), PT_07, false, minOpucnTs,
                                maxOpucnTs));
                    }
                    // Here payload-type is optional and is not used for interface creation (especially for network)
                    createdOduInterfaces.add(
                        openRoadmInterfaceFactory.createOpenRoadmOtnOdu4LoInterface(node.getNodeId(),
                            node.getNetworkTp(), input.getServiceName(), PT_07, true, minOpucnTs,
                            maxOpucnTs));
                    // Here payload-type is optional and is not used for service creation
                    // This is needed if there is an intermediate node
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            openRoadmInterfaceFactory.createOpenRoadmOtnOdu4LoInterface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), PT_07, true, minOpucnTs,
                                maxOpucnTs));
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

    private void createOduc4TtpInterface(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned) throws OpenRoadmInterfaceException {
        if (input.getNodes() == null) {
            return;
        }
        LOG.info("Creation of ODUC4 TTP interface in OTN service path {}", input);
        for (int i = 0; i < input.getNodes().size(); i++) {
            Nodes node = input.getNodes().get(i);
            String supportingOtuInterface = node.getNetworkTp() + "-OTUC4";
            List<String> createdOduc4Interfaces = new ArrayList<>();
            // Adding SAPI/DAPI information to the
            Nodes tgtNode = null;
            if (i + 1 == input.getNodes().size()) {
                // For the end node, tgtNode becomes the first node in the list
                tgtNode = input.getNodes().get(0);
            } else {
                tgtNode = input.getNodes().get(i + 1);
            }
            createdOduc4Interfaces.add(openRoadmInterfaceFactory.createOpenRoadmOtnOduc4Interface(node.getNodeId(),
                node.getNetworkTp(), supportingOtuInterface, tgtNode.getNodeId(), tgtNode.getNetworkTp()));

            NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder()
                .withKey(new NodeInterfaceKey(node.getNodeId()))
                .setNodeId(node.getNodeId())
                .setOduInterfaceId(createdOduc4Interfaces); // though this is odu, actually it has ODUC4 interfaces
            nodeInterfaces.add(nodeInterfaceBuilder.build());
            otnNodesProvisioned.add(node);
        }
    }

    private void createODU4TtpInterface(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
        CopyOnWriteArrayList<Nodes> otnNodesProvisioned) throws OpenRoadmInterfaceException {
        if (input.getNodes() == null) {
            return;
        }
        LOG.info("Creation of ODU4 tp interface {}", input);
        for (int i = 0; i < input.getNodes().size(); i++) {
            Nodes node = input.getNodes().get(i);
            String supportingOtuInterface = node.getNetworkTp() + "-OTU";
            List<String> createdOdu4Interfaces = new ArrayList<>();
            // Adding SAPI/DAPI information to the
            Nodes tgtNode = null;
            if (i + 1 == input.getNodes().size()) {
                // For the end node, tgtNode becomes the first node in the list
                tgtNode = input.getNodes().get(0);
            } else {
                tgtNode = input.getNodes().get(i + 1);
            }
            createdOdu4Interfaces.add(openRoadmInterfaceFactory.createOpenRoadmOtnOdu4Interface(node.getNodeId(),
                node.getNetworkTp(), supportingOtuInterface, tgtNode.getNodeId(), tgtNode.getNetworkTp()));
            NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder()
                .withKey(new NodeInterfaceKey(node.getNodeId()))
                .setNodeId(node.getNodeId())
                .setOduInterfaceId(createdOdu4Interfaces);
            nodeInterfaces.add(nodeInterfaceBuilder.build());
            otnNodesProvisioned.add(node);
        }
    }

    private void updateOtnTopology(CopyOnWriteArrayList<Nodes> nodes, List<String> nodesTps, Uint32 serviceRate,
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
