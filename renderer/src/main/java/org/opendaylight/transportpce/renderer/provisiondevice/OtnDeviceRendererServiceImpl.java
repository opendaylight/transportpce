/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.link.tp.LinkTpBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.otn.renderer.nodes.Nodes;
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

    public OtnDeviceRendererServiceImpl(OpenRoadmInterfaceFactory openRoadmInterfaceFactory, CrossConnect crossConnect,
                                        OpenRoadmInterfaces openRoadmInterfaces,
                                        DeviceTransactionManager deviceTransactionManager,
                                        NetworkModelService networkModelService) {
        this.openRoadmInterfaceFactory = openRoadmInterfaceFactory;
        this.crossConnect = crossConnect;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.deviceTransactionManager = deviceTransactionManager;
    }

//TODO Align log messages and returned results messages
    @Override
    public OtnServicePathOutput setupOtnServicePath(OtnServicePathInput input, String serviceType) {
        LOG.info("Calling setup otn-service path");
        if (input.getServiceFormat() == null || input.getServiceRate() == null) {
            return new OtnServicePathOutputBuilder()
                .setSuccess(false)
                .setResult("Error - service-type and service-rate must be present")
                .build();
        }
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        CopyOnWriteArrayList<LinkTp> otnLinkTps = new CopyOnWriteArrayList<>();
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
                    createHighOrderInterfaces(input, nodeInterfaces, otnLinkTps);
                    break;
                // For all the intermediate rates, device renderer is generalized as
                // ODUCnTTPinterface method
                case StringConstants.SERVICE_TYPE_ODUC2:
                case StringConstants.SERVICE_TYPE_ODUC3:
                case StringConstants.SERVICE_TYPE_ODUC4:
                    createOducnTtpInterface(input, nodeInterfaces, otnLinkTps);
                    break;
                case StringConstants.SERVICE_TYPE_100GE_S:
                    LOG.info("Calling Node interface for service-type {}", serviceType);
                    if (input.getNodes() != null) {
                        createHighOrderInterfaces(input, nodeInterfaces, otnLinkTps);
                        LOG.info("Node interfaces created");
                    }
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

    @SuppressWarnings("rawtypes")
    // FIXME check if the ForkJoinTask raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    public OtnServicePathOutput deleteOtnServicePath(OtnServicePathInput input, String serviceType) {
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
            switch (serviceType) {
                case StringConstants.SERVICE_TYPE_100GE_S:
                    connectionNumber = getConnectionNumber(node, networkTp, "ODU4");
                    break;
                case StringConstants.SERVICE_TYPE_100GE_M:
                    connectionNumber = getConnectionNumber(node, networkTp, "ODU4");
                    otnLinkTps.add(new LinkTpBuilder()
                        .setNodeId(nodeId)
                        .setTpId(networkTp)
                        .build());
                    break;
                case StringConstants.SERVICE_TYPE_ODU4:
                    if (node.getClientTp() == null && node.getNetwork2Tp() == null) {
                        interfacesToDelete.add(networkTp + "-ODU4");
                        otnLinkTps.add(new LinkTpBuilder()
                            .setNodeId(nodeId)
                            .setTpId(networkTp)
                            .build());
                    }
                    if (node.getClientTp() == null && node.getNetwork2Tp() != null) {
                        interfacesToDelete.add(networkTp + "-ODU4");
                        interfacesToDelete.add(node.getNetwork2Tp() + "-ODU4");
                        connectionNumber = getConnectionNumber(node, networkTp, "ODU4");
                    }
                    break;
                case StringConstants.SERVICE_TYPE_ODUC2:
                case StringConstants.SERVICE_TYPE_ODUC3:
                case StringConstants.SERVICE_TYPE_ODUC4:
                    if (node.getClientTp() == null && node.getNetwork2Tp() == null) {
                        // Service-type can be ODUC2, ODUC3, ODUC4
                        interfacesToDelete.add(networkTp + "-" + serviceType);
                        otnLinkTps.add(new LinkTpBuilder()
                            .setNodeId(nodeId)
                            .setTpId(networkTp)
                            .build());
                    }
                    if (node.getClientTp() == null && node.getNetwork2Tp() != null) {
                        interfacesToDelete.add(networkTp + "-" + serviceType);
                        interfacesToDelete.add(node.getNetwork2Tp() + "-" + serviceType);
                        connectionNumber = getConnectionNumber(node, networkTp, serviceType);
                    }
                    break;
                case StringConstants.SERVICE_TYPE_10GE:
                    connectionNumber = getConnectionNumber(node, networkTp, "ODU2e");
                    otnLinkTps.add(new LinkTpBuilder()
                        .setNodeId(nodeId)
                        .setTpId(networkTp)
                        .build());
                    break;
                case StringConstants.SERVICE_TYPE_1GE:
                    connectionNumber = getConnectionNumber(node, networkTp, "ODU0");
                    otnLinkTps.add(new LinkTpBuilder()
                        .setNodeId(nodeId)
                        .setTpId(networkTp)
                        .build());
                    break;
                default:
                    LOG.error("service-type {} not managed yet", serviceType);
                    String result = serviceType + " is not supported";
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
                    if (supportedInterface == null) {
                        continue;
                    }
                    // Here ODUC can be ODUC2, ODUC3, ODUC4
                    if ((input.getServiceRate().intValue() == 100 && !supportedInterface.contains("ODUC"))
                        || (input.getServiceRate().intValue() != 100 && !supportedInterface.contains("ODU4"))) {
                        interfacesToDelete.add(supportedInterface);
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

    private String getConnectionNumber(Nodes node, String networkTp, String oduType) {
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>(Arrays.asList("x"));
        if (node.getClientTp() != null) {
            list1.addAll(Arrays.asList(node.getClientTp(), oduType));
            list2.addAll(Arrays.asList(networkTp, oduType));
        } else if (node.getNetwork2Tp() != null) {
            list1.addAll(Arrays.asList(networkTp, oduType));
            list2.addAll(Arrays.asList(node.getNetwork2Tp(), oduType));
        } else {
            return "";
        }
        list1.addAll(list2);
        return String.join("-", list1);
    }

    private Optional<String> postCrossConnect(List<String> createdOduInterfaces, Nodes node)
            throws OpenRoadmInterfaceException {
        return this.crossConnect.postOtnCrossConnect(createdOduInterfaces, node);
    }

    private void createLowOrderInterfaces(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
            CopyOnWriteArrayList<LinkTp> linkTpList) throws OpenRoadmInterfaceException {
        for (Nodes node : input.getNodes()) {
            AEndApiInfo apiInfoA = null;
            ZEndApiInfo apiInfoZ = null;
            if (input.getAEndApiInfo() != null && input.getAEndApiInfo().getNodeId().contains(node.getNodeId())) {
                apiInfoA = input.getAEndApiInfo();
            }
            if (input.getZEndApiInfo() != null && input.getZEndApiInfo().getNodeId().contains(node.getNodeId())) {
                apiInfoZ = input.getZEndApiInfo();
            }
            // check if the node is mounted or not?
            Set<String> createdEthInterfaces = new HashSet<>();
            Set<String> createdOduInterfaces = new HashSet<>();
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
                                input.getServiceName(), false, input.getTribPortNumber(), input.getTribSlot(), apiInfoA,
                                apiInfoZ, PT_07));
                    }
                    createdOduInterfaces.add(
                        openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(), node.getNetworkTp(),
                            input.getServiceName(), true, input.getTribPortNumber(), input.getTribSlot(), null, null,
                            null));
                    linkTpList.add(
                        new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(), true, input.getTribPortNumber(),
                                input.getTribSlot(), null, null, null));
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
                            // supporting interface?, payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getClientTp(),  input.getServiceName(), false, input.getTribPortNumber(),
                                input.getTribSlot(), apiInfoA, apiInfoZ, PT_03));
                    }
                    createdOduInterfaces.add(
                        // supporting interface? payload ?
                        openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(), node.getNetworkTp(),
                             input.getServiceName(), true, input.getTribPortNumber(), input.getTribSlot(), null,
                            null, null));
                    linkTpList.add(
                        new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
                    if (node.getNetwork2Tp() != null) {
                        createdOduInterfaces.add(
                            // supporting interface? payload ?
                            openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                                node.getNetwork2Tp(), input.getServiceName(),true, input.getTribPortNumber(),
                                input.getTribSlot(), null, null, null));
                        linkTpList.add(
                            new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
                    }
                    break;
                case 100:
                    LOG.info("Input service is 100G");
                    // Take the first and last value in the list of OpucnTribSlot (assuming SH would provide
                    // min and max value only, size two)
                    OpucnTribSlotDef minOpucnTs = input.getOpucnTribSlots().stream()
                        .min((ts1, ts2) -> ts1.getValue().compareTo(ts2.getValue())).get();
                    OpucnTribSlotDef maxOpucnTs = input.getOpucnTribSlots().stream()
                        .max((ts1, ts2) -> ts1.getValue().compareTo(ts2.getValue())).get();
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
            Set<String> createdConnections = new HashSet<>();
            if (!createdOduInterfaces.isEmpty()) {
                Optional<String> connectionNameOpt = postCrossConnect(new ArrayList<>(createdOduInterfaces), node);
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

    private void createHighOrderInterfaces(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
            CopyOnWriteArrayList<LinkTp> linkTpList) throws OpenRoadmInterfaceException {
        for (Nodes node : input.nonnullNodes()) {
            AEndApiInfo apiInfoA = null;
            ZEndApiInfo apiInfoZ = null;
            if (input.getAEndApiInfo() != null && input.getAEndApiInfo().getNodeId().contains(node.getNodeId())) {
                apiInfoA = input.getAEndApiInfo();
            }
            if (input.getZEndApiInfo() != null && input.getZEndApiInfo().getNodeId().contains(node.getNodeId())) {
                apiInfoZ = input.getZEndApiInfo();
            }
            // check if the node is mounted or not?
            Set<String> createdEthInterfaces = new HashSet<>();
            Set<String> createdOduInterfaces = new HashSet<>();
            switch (input.getServiceRate().intValue()) {
                case 100:
                    LOG.info("Input service is 100G");
                    if (node.getClientTp() != null && node.getNetwork2Tp() == null) {
                        createdEthInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmEth100GInterface(
                            node.getNodeId(), node.getClientTp()));
                        createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu4HOInterface(
                                node.getNodeId(), node.getClientTp(), false, apiInfoA, apiInfoZ, "21"));
                        // supporting interface? payload ?
                        createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu4HOInterface(
                                node.getNodeId(), node.getNetworkTp(), true, null, null, null));
                        linkTpList.add(new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getClientTp())
                                .build());
                    }
                    if (node.getClientTp() == null && node.getNetwork2Tp() == null) {
                        createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu4HOInterface(
                            node.getNodeId(), node.getNetworkTp(), false, apiInfoA, apiInfoZ, "21"));
                        linkTpList.add(new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp())
                            .build());
                    }
                    if (node.getClientTp() == null && node.getNetwork2Tp() != null) {
                        // supporting interface? payload ?
                        createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu4HOInterface(
                            node.getNodeId(), node.getNetworkTp(), true, null, null, null));
                        createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu4HOInterface(
                                node.getNodeId(), node.getNetwork2Tp(), true, null, null, null));
                    }
                    break;
                default:
                    LOG.error("service rate {} not managed yet", input.getServiceRate());
                    return;
            }

            // implement cross connect
            Set<String> createdConnections = new HashSet<>();
            if (createdOduInterfaces.size() == 2) {
                Optional<String> connectionNameOpt = postCrossConnect(new ArrayList<>(createdOduInterfaces), node);
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

    private void createOducnTtpInterface(OtnServicePathInput input, List<NodeInterface> nodeInterfaces,
        CopyOnWriteArrayList<LinkTp> linkTpList) throws OpenRoadmInterfaceException {
        if (input.getNodes() == null) {
            return;
        }
        if (input.getServiceRate() == null) {
            LOG.error("Missing service rate for ODUCn interface");
            return;
        }
        LOG.info("Creation of ODUCn TTP interface in OTN service path {}", input);
        for (int i = 0; i < input.getNodes().size(); i++) {
            Nodes node = input.getNodes().get(i);
            // Based on the service rate, we will know if it is a OTUC4, OTUC3 or OTUC2
            String supportingOtuInterface = node.getNetworkTp();
            boolean serviceRateNotSupp = false;

            switch (input.getServiceRate().intValue()) {
                case 200:
                    supportingOtuInterface += "-OTUC2";
                    break;
                case 300:
                    supportingOtuInterface += "-OTUC3";
                    break;
                case 400:
                    supportingOtuInterface += "-OTUC4";
                    break;
                default:
                    serviceRateNotSupp = true;
                    break;
            }
            if (serviceRateNotSupp) {
                LOG.error("Service rate {} is not supported", input.getServiceRate());
            }

            Nodes tgtNode =
                i + 1 == input.getNodes().size()
                // For the end node, tgtNode becomes the first node in the list
                    ? input.getNodes().get(0)
                    : input.getNodes().get(i + 1);

            nodeInterfaces.add(new NodeInterfaceBuilder()
                    .withKey(new NodeInterfaceKey(node.getNodeId()))
                    .setNodeId(node.getNodeId())
                    .setOduInterfaceId(Set.of(
                        // though this is odu, actually it has ODUCn interfaces
                        openRoadmInterfaceFactory.createOpenRoadmOtnOducnInterface(node.getNodeId(),
                            node.getNetworkTp(), supportingOtuInterface, tgtNode.getNodeId(), tgtNode.getNetworkTp())))
                    .build());
            linkTpList.add(new LinkTpBuilder().setNodeId(node.getNodeId()).setTpId(node.getNetworkTp()).build());
        }
    }
}
