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
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.link.tp.LinkTpBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.otn.renderer.nodes.Nodes;
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

//TODO Align log messages and returned results messages
    @Override
    public OtnServicePathOutput setupOtnServicePath(OtnServicePathInput input) {
        LOG.info("Calling setup otn-service path");
        if (input.getServiceFormat() == null || input.getServiceRate() == null) {
            return new OtnServicePathOutputBuilder()
                .setSuccess(false)
                .setResult("Error - service-type and service-rate must be present")
                .build();
        }
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        CopyOnWriteArrayList<LinkTp> otnLinkTps = new CopyOnWriteArrayList<>();
        String serviceType = ServiceTypes.getOtnServiceType(input.getServiceFormat(), input.getServiceRate());
        try {
            switch (serviceType) {
                case StringConstants.SERVICE_TYPE_1GE:
                case StringConstants.SERVICE_TYPE_10GE:
                case StringConstants.SERVICE_TYPE_100GE_M:
                    LOG.info("Calling Node interfaces {} {} {} {} {} {} {}",
                        input.getServiceRate(), input.getEthernetEncoding(),
                        input.getServiceFormat(), input.getOperation(), input.getTribPortNumber(),
                        input.getTribSlot(), input.getNodes());
                    if (input.getNodes() != null) {
                        createLowOrderInterfaces(input, nodeInterfaces, otnLinkTps);
                        LOG.info("Node interfaces created just fine ");
                    }
                    break;
                case StringConstants.SERVICE_TYPE_ODU4:
                    createODU4TtpInterface(input, nodeInterfaces, otnLinkTps);
                    break;
                case StringConstants.SERVICE_TYPE_ODUC4:
                    createOduc4TtpInterface(input, nodeInterfaces, otnLinkTps);
                    break;
                default:
                    LOG.error("Service-type {} not managed yet", serviceType);
                    return new OtnServicePathOutputBuilder()
                        .setSuccess(false)
                        .setResult("Service-type not managed")
                        .build();
            }
        } catch (OpenRoadmInterfaceException e) {
            LOG.warn("Service path set-up failed", e);
            Map<NodeInterfaceKey,NodeInterface> nodeInterfacesMap = new HashMap<>();
            for (NodeInterface nodeInterface : nodeInterfaces) {
                if (nodeInterface != null) {
                    nodeInterfacesMap.put(nodeInterface.key(), nodeInterface);
                }
            }
            //TODO check if we need to set a NodeInterface Map in the result in that case
            return new OtnServicePathOutputBuilder()
                    .setSuccess(false)
                    .setNodeInterface(nodeInterfacesMap)
                    .setResult("Service path set-up failed")
                    .setLinkTp(otnLinkTps)
                    .build();
        }
        LOG.info("Service path set-up succeed");
        List<String> results = new ArrayList<>();
        Map<NodeInterfaceKey,NodeInterface> nodeInterfacesMap = new HashMap<>();
        for (NodeInterface nodeInterface : nodeInterfaces) {
            if (nodeInterface != null) {
                results.add("Otn Service path was set up successfully for node :" + nodeInterface.getNodeId());
                nodeInterfacesMap.put(nodeInterface.key(), nodeInterface);
            }
        }
        return new OtnServicePathOutputBuilder()
                .setSuccess(true)
                .setNodeInterface(nodeInterfacesMap)
                .setResult(String.join("\n", results))
                .setLinkTp(otnLinkTps)
                .build();
    }

    public OtnServicePathOutput deleteOtnServicePath(OtnServicePathInput input) {
        if (input.getNodes() == null) {
            LOG.error("Unable to delete otn service path. input nodes = null");
            return new OtnServicePathOutputBuilder()
                .setResult("Unable to delete otn service path. input nodes = null")
                .setSuccess(false)
                .build();
        }
        List<Nodes> nodes = input.getNodes();
        AtomicBoolean success = new AtomicBoolean(true);
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        CopyOnWriteArrayList<LinkTp> otnLinkTps = new CopyOnWriteArrayList<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask forkJoinTask = forkJoinPool.submit(() -> nodes.parallelStream().forEach(node -> {
            String nodeId = node.getNodeId();
            LOG.info("Deleting service setup on node {}", nodeId);
            String networkTp = node.getNetworkTp();
            if (networkTp == null || input.getServiceRate() == null || input.getServiceFormat() == null) {
                LOG.error("destination ({}) or service-rate ({}) or service-format ({}) is null.",
                    networkTp, input.getServiceRate(), input.getServiceFormat());
                return;
            }
            if (!this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                String result = nodeId + " is not mounted on the controller";
                results.add(result);
                success.set(false);
                LOG.warn(result);
                forkJoinPool.shutdown();
                return;
                // TODO should deletion end here?
            }
            // if the node is currently mounted then proceed.
            List<String> interfacesToDelete = new LinkedList<>();
            String connectionNumber = "";
            switch (input.getServiceRate().intValue()) {
                case 100:
                    if ("ODU".equals(input.getServiceFormat())) {
                        interfacesToDelete.add(networkTp + "-ODU4");
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
            for (String interf : intToDelete == null ? new ArrayList<String>() : intToDelete) {
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
            List<String> interList =
                    interfacesToDelete.stream().filter(ele -> ele.contains("NETWORK")).collect(Collectors.toList());
            if (!interList.isEmpty()) {
                for (String inter : interList) {
                    otnLinkTps.add(new LinkTpBuilder()
                            .setNodeId(nodeId)
                            .setTpId(inter.split("-ODU")[0])
                            .build());
                }
            }
        }));
        try {
            forkJoinTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while deleting service paths!", e);
            return new OtnServicePathOutputBuilder()
                .setResult("Error while deleting service paths!")
                .setSuccess(false)
                .build();
        }
        forkJoinPool.shutdown();
        return new OtnServicePathOutputBuilder()
                .setSuccess(success.get())
                .setLinkTp(otnLinkTps)
                .setResult(
                    results.isEmpty()
                        ? "Request processed"
                        : String.join("\n", results))
                .build();
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

    private Optional<String> postCrossConnect(List<String> createdOduInterfaces, Nodes node)
            throws OpenRoadmInterfaceException {
        return this.crossConnect.postOtnCrossConnect(createdOduInterfaces, node);
    }

    private void createLowOrderInterfaces(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
            CopyOnWriteArrayList<LinkTp> linkTpList) throws OpenRoadmInterfaceException {
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
                    linkTpList.add(
                        new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), PT_07, true, input.getTribPortNumber(),
                                input.getTribSlot()));
                        linkTpList.add(
                            new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
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
                    linkTpList.add(
                        new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), PT_03, true, input.getTribPortNumber(),
                                input.getTribSlot()));
                        linkTpList.add(
                            new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
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
                    linkTpList.add(
                        new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
                    // Here payload-type is optional and is not used for service creation
                    // This is needed if there is an intermediate node
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            openRoadmInterfaceFactory.createOpenRoadmOtnOdu4LoInterface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), PT_07, true, minOpucnTs,
                                maxOpucnTs));
                        linkTpList.add(
                            new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
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
            nodeInterfaces.add(new NodeInterfaceBuilder()
                    .withKey(new NodeInterfaceKey(node.getNodeId()))
                    .setNodeId(node.getNodeId())
                    .setConnectionId(createdConnections)
                    .setEthInterfaceId(createdEthInterfaces)
                    .setOduInterfaceId(createdOduInterfaces)
                    .build());
        }
    }

    private void createOduc4TtpInterface(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
        CopyOnWriteArrayList<LinkTp> linkTpList) throws OpenRoadmInterfaceException {
        if (input.getNodes() == null) {
            return;
        }
        LOG.info("Creation of ODUC4 TTP interface in OTN service path {}", input);
        for (int i = 0; i < input.getNodes().size(); i++) {
            Nodes node = input.getNodes().get(i);
            String supportingOtuInterface = node.getNetworkTp() + "-OTUC4";

            Nodes tgtNode =
                i + 1 == input.getNodes().size()
                // For the end node, tgtNode becomes the first node in the list
                    ? input.getNodes().get(0)
                    : input.getNodes().get(i + 1);

            nodeInterfaces.add(new NodeInterfaceBuilder()
                    .withKey(new NodeInterfaceKey(node.getNodeId()))
                    .setNodeId(node.getNodeId())
                    .setOduInterfaceId(List.of(
                        // though this is odu, actually it has ODUC4 interfaces
                        openRoadmInterfaceFactory.createOpenRoadmOtnOduc4Interface(node.getNodeId(),
                            node.getNetworkTp(), supportingOtuInterface, tgtNode.getNodeId(), tgtNode.getNetworkTp())))
                    .build());
            linkTpList.add(new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
        }
    }

    private void createODU4TtpInterface(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
        CopyOnWriteArrayList<LinkTp> linkTpList) throws OpenRoadmInterfaceException {
        if (input.getNodes() == null) {
            return;
        }
        LOG.info("Creation of ODU4 tp interface {}", input);
        for (int i = 0; i < input.getNodes().size(); i++) {
            Nodes node = input.getNodes().get(i);
            String supportingOtuInterface = node.getNetworkTp() + "-OTU";

            Nodes tgtNode =
                i + 1 == input.getNodes().size()
                // For the end node, tgtNode becomes the first node in the list
                    ? input.getNodes().get(0)
                    : input.getNodes().get(i + 1);

            nodeInterfaces.add(new NodeInterfaceBuilder()
                    .withKey(new NodeInterfaceKey(node.getNodeId()))
                    .setNodeId(node.getNodeId())
                    .setOduInterfaceId(List.of(
                        openRoadmInterfaceFactory.createOpenRoadmOtnOdu4Interface(node.getNodeId(),
                            node.getNetworkTp(), supportingOtuInterface, tgtNode.getNodeId(), tgtNode.getNetworkTp())))
                    .build());
            linkTpList.add(new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
        }
    }
}
