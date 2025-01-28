/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.catalog.CatalogConstant;
import org.opendaylight.transportpce.common.catalog.CatalogConstant.CatalogNodeType;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.common.device.observer.EventSubscriber;
import org.opendaylight.transportpce.common.device.observer.Ignore;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.frequency.FrequencySelectionFactory;
import org.opendaylight.transportpce.pce.frequency.Select;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.spectrum.assignment.Assign;
import org.opendaylight.transportpce.pce.spectrum.assignment.AssignSpectrum;
import org.opendaylight.transportpce.pce.spectrum.assignment.Range;
import org.opendaylight.transportpce.pce.spectrum.centerfrequency.CenterFrequencyGranularityCollection;
import org.opendaylight.transportpce.pce.spectrum.centerfrequency.Collection;
import org.opendaylight.transportpce.pce.spectrum.index.Base;
import org.opendaylight.transportpce.pce.spectrum.index.BaseFrequency;
import org.opendaylight.transportpce.pce.spectrum.index.SpectrumIndex;
import org.opendaylight.transportpce.pce.spectrum.slot.CapabilityCollection;
import org.opendaylight.transportpce.pce.spectrum.slot.InterfaceMcCapability;
import org.opendaylight.transportpce.pce.spectrum.slot.McCapabilityCollection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PceConstraintMode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class PostAlgoPathValidator {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PostAlgoPathValidator.class);

    public static final Long CONST_OSNR = 1L;
    public static final double SYS_MARGIN = 0;

    private Double tpceCalculatedMargin = 0.0;
    private final NetworkTransactionService networkTransactionService;
    private final BitSet spectrumConstraint;
    private final ClientInput clientInput;

    public PostAlgoPathValidator(NetworkTransactionService networkTransactionService,
                                 BitSet spectrumConstraint,
                                 ClientInput clientInput) {
        this.networkTransactionService = networkTransactionService;
        this.spectrumConstraint = spectrumConstraint;
        this.clientInput = clientInput;
    }

    @SuppressWarnings("fallthrough")
    @SuppressFBWarnings(
        value = "SF_SWITCH_FALLTHROUGH",
        justification = "intentional fallthrough")
    public PceResult checkPath(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, PceResult pceResult, PceConstraints pceHardConstraints,
            String serviceType, PceConstraintMode mode) {
        LOG.info("path = {}", path);
        // check if the path is empty
        if (path.getEdgeList().isEmpty()) {
            return pceResult;
        }
        int spectralWidthSlotNumber =
            GridConstant.SPECTRAL_WIDTH_SLOT_NUMBER_MAP.getOrDefault(serviceType, GridConstant.NB_SLOTS_100G);
        SpectrumAssignment spectrumAssignment = null;
        //variable to deal with 1GE (Nb=1) and 10GE (Nb=10) cases
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_OTUC2:
            case StringConstants.SERVICE_TYPE_OTUC3:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_400GE:
                spectralWidthSlotNumber =
                    GridConstant.SPECTRAL_WIDTH_SLOT_NUMBER_MAP.getOrDefault(serviceType, GridConstant.NB_SLOTS_400G);
            //fallthrough
            case StringConstants.SERVICE_TYPE_100GE_T:
            case StringConstants.SERVICE_TYPE_OTU4:
            case StringConstants.SERVICE_TYPE_OTHER:
                Subscriber subscriber = new EventSubscriber();
                spectrumAssignment = getSpectrumAssignment(path, allPceNodes, spectralWidthSlotNumber, subscriber);
                pceResult.setServiceType(serviceType);
                if (spectrumAssignment.getBeginIndex().equals(Uint16.valueOf(0))
                        && spectrumAssignment.getStopIndex().equals(Uint16.valueOf(0))) {
                    pceResult.error(subscriber.last(Level.ERROR, "No frequencies available."));
                    pceResult.setLocalCause(PceResult.LocalCause.NO_PATH_EXISTS);
                    return pceResult;
                }
                if (spectrumAssignment.getFlexGrid()) {
                    LOG.debug("Spectrum assignment flexgrid mode");
                    pceResult.setResultWavelength(GridConstant.IRRELEVANT_WAVELENGTH_NUMBER);
                } else {
                    LOG.debug("Spectrum assignment fixedgrid mode");
                    pceResult.setResultWavelength(
                        GridUtils.getWaveLengthIndexFromSpectrumAssigment(spectrumAssignment.getBeginIndex().toJava()));
                }
                pceResult.setMinFreq(GridUtils.getStartFrequencyFromIndex(spectrumAssignment.getBeginIndex().toJava()));
                pceResult.setMaxFreq(GridUtils.getStopFrequencyFromIndex(spectrumAssignment.getStopIndex().toJava()));
                LOG.debug("In PostAlgoPathValidator: spectrum assignment found {} {}", spectrumAssignment, path);

                // Check the OSNR
                CatalogUtils cu = new CatalogUtils(networkTransactionService);
                if (cu.isCatalogFilled()) {
                    double margin1 = checkOSNR(path, allPceNodes, allPceLinks, serviceType,
                            StringConstants.SERVICE_DIRECTION_AZ, cu);
                    double margin2 = checkOSNR(path, allPceNodes, allPceLinks, serviceType,
                            StringConstants.SERVICE_DIRECTION_ZA, cu);
                    if (margin1 < 0 || margin2 < 0 || margin1 == Double.NEGATIVE_INFINITY
                            || margin2 == Double.NEGATIVE_INFINITY) {
                        pceResult.error(String.format("OSNR out of range (%s - %s)", margin1, margin2));
                        pceResult.setLocalCause(PceResult.LocalCause.OUT_OF_SPEC_OSNR);
                        return pceResult;
                    }
                    this.tpceCalculatedMargin = Math.min(margin1, margin2);
                    LOG.info(
                        "In PostAlgoPathValidator: Minimum margin estimated by tpce on AtoZ and ZtoA path is of  {} dB",
                        this.tpceCalculatedMargin);
                } else {
                    this.tpceCalculatedMargin = 0.0;
                    LOG.info("In PostAlgoPathValidator: Operational mode Catalog not filled, delegate OSNR calculation"
                        + " to GNPy and margin set to 0");
                }
                // Check if MaxLatency is defined in the hard constraints
                if (pceHardConstraints.getMaxLatency() != -1
                        && !checkLatency(pceHardConstraints.getMaxLatency(), path)) {
                    pceResult.error("Latency is too high according to pce hard constraints.");
                    pceResult.setLocalCause(PceResult.LocalCause.TOO_HIGH_LATENCY);
                    return pceResult;
                }
                // Check if nodes are included in the hard constraints
                if (!checkInclude(path, pceHardConstraints, mode)) {
                    pceResult.error("Nodes in path are not included in pce hard constraints.");
                    pceResult.setLocalCause(PceResult.LocalCause.HD_NODE_INCLUDE);
                    return pceResult;
                }
                // TODO here other post algo validations can be added
                // more data can be sent to PceGraph module via PceResult structure if required
                pceResult.success();
                pceResult.setLocalCause(PceResult.LocalCause.NONE);
                return pceResult;
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                int tribSlotNb = Map.of(
                        StringConstants.SERVICE_TYPE_100GE_M, 20,
                        StringConstants.SERVICE_TYPE_10GE, 8,
                        StringConstants.SERVICE_TYPE_1GE, 1)
                    .get(serviceType);
                pceResult.error("An unknown error occurred while trying to find the spectrum assignment.");
                pceResult.setServiceType(serviceType);
                Map<String, List<Uint16>> tribSlot = chooseTribSlot(path, allPceNodes, tribSlotNb);
                Map<String, Uint16> tribPort = chooseTribPort(path, allPceNodes, tribSlot, tribSlotNb);
                List<OpucnTribSlotDef> resultTribPortTribSlot = getMinMaxTpTs(tribPort, tribSlot);
                if (resultTribPortTribSlot.get(0) != null && resultTribPortTribSlot.get(1) != null) {
                    pceResult.setResultTribPortTribSlot(resultTribPortTribSlot);
                    pceResult.success();
                    LOG.info("In PostAlgoPathValidator: found TribPort {} - tribSlot {} - tribSlotNb {}",
                        tribPort, tribSlot, tribSlotNb);
                }
                return pceResult;
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC2:
            case StringConstants.SERVICE_TYPE_ODUC3:
            case StringConstants.SERVICE_TYPE_ODUC4:
            case StringConstants.SERVICE_TYPE_100GE_S:
                pceResult.success();
                pceResult.setServiceType(serviceType);
                LOG.info("In PostAlgoPathValidator: ODU4/ODUCn path found {}", path);
                return pceResult;
            default:
                pceResult.error(String.format("Unsupported service type %s", serviceType));
                LOG.warn("In PostAlgoPathValidator checkPath: unsupported serviceType {} found {}",
                    serviceType, path);
                return pceResult;
        }
    }

    // Check the latency
    private boolean checkLatency(Long maxLatency, GraphPath<String, PceGraphEdge> path) {
        double latency = 0;
        for (PceGraphEdge edge : path.getEdgeList()) {
            if (edge.link() == null || edge.link().getLatency() == null) {
                LOG.warn("- In checkLatency: the link {} does not contain latency field",
                    edge.link().getLinkId().getValue());
                return false;
            }
            latency += edge.link().getLatency();
            LOG.debug("- In checkLatency: latency of {} = {} units", edge.link().getLinkId().getValue(), latency);
        }
        return (latency < maxLatency);
    }

    // Check the inclusion if it is defined in the hard constraints
    //TODO: remove this checkstyle false positive warning when the checkstyle bug will be fixed
    @SuppressWarnings("MissingSwitchDefault")
    private boolean checkInclude(GraphPath<String, PceGraphEdge> path, PceConstraints pceHardConstraintsInput,
            PceConstraintMode mode) {
        List<ResourcePair> listToInclude = pceHardConstraintsInput.getListToInclude();
        if (listToInclude.isEmpty()) {
            return true;
        }
        List<PceGraphEdge> pathEdges = path.getEdgeList();
        LOG.debug(" in checkInclude vertex list: [{}]", path.getVertexList());
        LOG.debug("listToInclude = {}", listToInclude);
        List<String> listOfElementsSubNode = new ArrayList<>();
        listOfElementsSubNode.add(pathEdges.get(0).link().getsourceNetworkSupNodeId());
        listOfElementsSubNode.addAll(
            listOfElementsBuild(pathEdges, PceConstraints.ResourceType.NODE, pceHardConstraintsInput));
        List<String> listOfElementsCLLI = new ArrayList<>();
        listOfElementsCLLI.add(pathEdges.get(0).link().getsourceCLLI());
        listOfElementsCLLI.addAll(
            listOfElementsBuild(pathEdges, PceConstraints.ResourceType.CLLI, pceHardConstraintsInput));
        List<String> listOfElementsSRLG = new ArrayList<>();
        // first link is XPONDEROUTPUT, no SRLG for it
        listOfElementsSRLG.add("NONE");
        listOfElementsSRLG.addAll(
            listOfElementsBuild(pathEdges, PceConstraints.ResourceType.SRLG, pceHardConstraintsInput));
        // validation: check each type for each element
        LOG.debug("listOfElementsSubNode = {}", listOfElementsSubNode);
        return switch (mode) {
            case Loose -> listOfElementsSubNode
                .containsAll(listToInclude.stream()
                    .filter(rp -> PceConstraints.ResourceType.NODE.equals(rp.getType()))
                    .map(ResourcePair::getName).collect(Collectors.toList()));
            case Strict -> listOfElementsSubNode
                .equals(listToInclude.stream()
                    .filter(rp -> PceConstraints.ResourceType.NODE.equals(rp.getType()))
                    .map(ResourcePair::getName).collect(Collectors.toList()));
        }
            && listOfElementsSRLG.containsAll(
                listToInclude
                    .stream().filter(rp -> PceConstraints.ResourceType.SRLG.equals(rp.getType()))
                    .map(ResourcePair::getName).collect(Collectors.toList()))
            && listOfElementsCLLI.containsAll(
                listToInclude
                    .stream().filter(rp -> PceConstraints.ResourceType.CLLI.equals(rp.getType()))
                    .map(ResourcePair::getName).collect(Collectors.toList()));
    }

    private List<String> listOfElementsBuild(List<PceGraphEdge> pathEdges, PceConstraints.ResourceType type,
            PceConstraints pceHardConstraints) {
        Set<String> listOfElements = new LinkedHashSet<>();
        for (PceGraphEdge link : pathEdges) {
            switch (type) {
                case NODE:
                    listOfElements.add(link.link().getdestNetworkSupNodeId());
                    break;
                case CLLI:
                    listOfElements.add(link.link().getdestCLLI());
                    break;
                case SRLG:
                    if (link.link().getlinkType() != OpenroadmLinkType.ROADMTOROADM) {
                        listOfElements.add("NONE");
                        break;
                    }
                    // srlg of link is List<Long>. But in this algo we need string representation of
                    // one SRLG
                    // this should be any SRLG mentioned in include constraints if any of them if
                    // mentioned
                    boolean found = false;
                    for (Long srlg : link.link().getsrlgList()) {
                        String srlgStr = String.valueOf(srlg);
                        if (pceHardConstraints.getSRLGnames().contains(srlgStr)) {
                            listOfElements.add(srlgStr);
                            LOG.info("listOfElementsBuild. FOUND SRLG {} in link {}", srlgStr, link.link());
                            found = true;
                        }
                    }
                    if (!found) {
                        // there is no specific srlg to include. thus add to list just the first one
                        listOfElements.add("NONE");
                    }
                    break;
                default:
                    LOG.debug("listOfElementsBuild unsupported resource type");
            }
        }
        return new ArrayList<>(listOfElements);
    }

    private Map<String, Uint16> chooseTribPort(GraphPath<String,
            PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes, Map<String, List<Uint16>> tribSlotMap, int nbSlot) {
        LOG.debug("In choosetribPort: edgeList = {} ", path.getEdgeList());
        Map<String, Uint16> tribPortMap = new HashMap<>();
        for (PceGraphEdge edge : path.getEdgeList()) {
            List<Uint16> srcTpnPool =
                allPceNodes
                    .get(edge.link().getSourceId())
                    .getAvailableTribPorts()
                    .get(edge.link().getSourceTP().getValue());
            List<Uint16> destTpnPool =
                allPceNodes
                    .get(edge.link().getDestId())
                    .getAvailableTribPorts()
                    .get(edge.link().getDestTP().getValue());
            List<Uint16> commonEdgeTpnPool = new ArrayList<>();
            for (Uint16 srcTpn : srcTpnPool) {
                if (destTpnPool.contains(srcTpn)) {
                    commonEdgeTpnPool.add(srcTpn);
                }
            }
            if (commonEdgeTpnPool.isEmpty()) {
                continue;
            }
            Integer startTribSlot = tribSlotMap.values().stream().findFirst().orElseThrow().get(0).toJava();
            Integer tribPort = (int) Math.ceil((double)startTribSlot / nbSlot);
            for (Uint16 commonTribPort : commonEdgeTpnPool) {
                if (tribPort.equals(commonTribPort.toJava())) {
                    tribPortMap.put(edge.link().getLinkId().getValue(), commonTribPort);
                }
            }
        }
        tribPortMap.forEach((k,v) -> LOG.info("TribPortMap : k = {}, v = {}", k, v));
        return tribPortMap;
    }

    private Map<String, List<Uint16>> chooseTribSlot(GraphPath<String,
            PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes, int nbSlot) {
        LOG.debug("In choosetribSlot: edgeList = {} ", path.getEdgeList());
        Map<String, List<Uint16>> tribSlotMap = new HashMap<>();
        for (PceGraphEdge edge : path.getEdgeList()) {
            List<Uint16> srcTsPool =
                allPceNodes
                    .get(edge.link().getSourceId())
                    .getAvailableTribSlots()
                    .get(edge.link().getSourceTP().getValue());
            List<Uint16> destTsPool =
                allPceNodes
                    .get(edge.link().getDestId())
                    .getAvailableTribSlots()
                    .get(edge.link().getDestTP().getValue());
            List<Uint16> commonEdgeTsPoolList = new ArrayList<>();
            for (Uint16 integer : srcTsPool) {
                if (destTsPool.contains(integer)) {
                    commonEdgeTsPoolList.add(integer);
                }
            }
            Collections.sort(commonEdgeTsPoolList);
            List<Uint16> commonGoodStartEdgeTsPoolList = new ArrayList<>();
            for (Uint16 startEdgeTsPool : commonEdgeTsPoolList) {
                if (Integer.valueOf(1).equals(startEdgeTsPool.toJava() % nbSlot) || nbSlot == 1) {
                    commonGoodStartEdgeTsPoolList.add(startEdgeTsPool);
                }
            }
            Collections.sort(commonGoodStartEdgeTsPoolList);
            boolean goodTsList = false;
            List<Uint16> tribSlotList = new ArrayList<>();
            for (Uint16 goodStartTsPool : commonGoodStartEdgeTsPoolList) {
                int goodStartIndex = commonEdgeTsPoolList.indexOf(Uint16.valueOf(goodStartTsPool.intValue()));
                if (!goodTsList && commonEdgeTsPoolList.size() - goodStartIndex >= nbSlot) {
                    for (int i = 0; i < nbSlot; i++) {
                        if (!commonEdgeTsPoolList.get(goodStartIndex + i)
                                .equals(Uint16.valueOf(goodStartTsPool.toJava() + i))) {
                            goodTsList = false;
                            tribSlotList.clear();
                            break;
                        }
                        tribSlotList.add(commonEdgeTsPoolList.get(goodStartIndex + i));
                        goodTsList = true;
                    }
                }
            }
            tribSlotMap.put(edge.link().getLinkId().getValue(), tribSlotList);
        }
        tribSlotMap.forEach((k,v) -> LOG.info("TribSlotMap : k = {}, v = {}", k, v));
        return tribSlotMap;
    }

    private List<OpucnTribSlotDef> getMinMaxTpTs(Map<String, Uint16> tribPort, Map<String, List<Uint16>> tribSlot) {
        String tribport = tribPort.values().toArray()[0].toString();
        @SuppressWarnings("unchecked") List<Uint16> tsList = (List<Uint16>) tribSlot.values().toArray()[0];
        return new ArrayList<>(List.of(
            OpucnTribSlotDef.getDefaultInstance(String.join(".", tribport, tsList.get(0).toString())),
            OpucnTribSlotDef.getDefaultInstance(String.join(".", tribport, tsList.get(tsList.size() - 1).toString()))));
    }

    private double checkOSNR(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, String serviceType, String direction, CatalogUtils cu) {
        switch (direction) {
            case StringConstants.SERVICE_DIRECTION_AZ:
                return checkOSNRaz(path, allPceNodes, allPceLinks, serviceType, cu);
            case StringConstants.SERVICE_DIRECTION_ZA:
                return checkOSNRza(path, allPceNodes, allPceLinks, serviceType, cu);
            default:
                LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported direction {}", direction);
                return 0.0;
        }
    }

    /**
     * Calculates the OSNR of a path, according to the direction (AtoZ/ZtoA), using the operational-modes Catalog.
     *
     * @param path                      the AtoZ path provided by the PCE.
     * @param allPceNode                The map of chosen/relevant PceNodes build from topology pruning.
     * @param allPceLinks               The map of PceLinks build corresponding to the whole topology.
     * @param serviceType               The service Type used to extrapolate Operational mode when it is not provided.
     * @param cu                        CatalogUtils instance.
     * @return the calculated margin according to the Transponder performances and path impairments.
     */
    private double checkOSNRaz(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, String serviceType, CatalogUtils cu) {
        Map<String, Double> signal = new HashMap<>(
            Map.of(
                "spacing", Double.valueOf(50.0),
                "calcPdl2", Double.valueOf(0),
                "calcCd", Double.valueOf(0),
                "calcPmd2", Double.valueOf(0),
                "calcOnsrLin", Double.valueOf(0.0001),
                "pwrIn", Double.valueOf(-60.0),
                "pwrOut", Double.valueOf(-60.0)));
        double calcOnsrdB = 0;
        boolean transponderPresent = false;
        List<String> vertices = path.getVertexList();
        List<PceGraphEdge> edges = path.getEdgeList();
        // LOOP that scans the different Nodes/Links of the path and calculates
        // associated degradations
        // using CatalogUtils primitives to retrieve physical parameters and make a
        // first level calculation
        int bypassDegree = 0;
        for (int pathElement = 0; pathElement < 2; pathElement++) {
            bypassDegree = 0;
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement + 1)));
            LOG.debug("loop of check OSNR direction AZ, Path Element = {}", pathElement);
            switch (currentNode.getORNodeType()) {
                case XPONDER:
                    LOG.debug("loop of check OSNR direction AZ: XPDR, Path Element = {}", pathElement);
                    transponderPresent = true;
                    calcXpdrOSNR(cu, signal,
                        pathElement == 0
                            // First transponder on the Path (TX side) / Last Xponder of the path (RX side)
                            ? edges.get(pathElement).link().getSourceTP().getValue()
                            : edges.get(pathElement - 1).link().getDestTP().getValue(),
                        serviceType, currentNode, nextNode, vertices.get(pathElement), pathElement);
                    break;
                case SRG:
                    LOG.debug("loop of check OSNR direction AZ: SRG, Path Element = {}", pathElement);
                    // This is ADD case : First (optical-tunnel) or 2nd (Regular E2E service from
                    // Xponder to Xponder) node element of the path is the ADD SRG.
                    if (edges.get(pathElement).link().getlinkType() != OpenroadmLinkType.ADDLINK) {
                        LOG.error("Error processing Node {} for which output link {} is not an ADDLINK Type",
                            currentNode.getNodeId(), pathElement);
                    }
                    signal.put("pwrIn", Double.valueOf(0));
                    calcAddContrib(cu, signal, currentNode, edges.get(pathElement + 1).link());
                    LOG.debug("loop of check OSNR direction AZ: SRG, pathElement = {} link {} Pout = {}",
                        pathElement, pathElement + 1, signal.get("pwrOut"));
                    double calcOnsr = signal.get("calcOnsrLin").doubleValue();
                    if (calcOnsr == Double.NEGATIVE_INFINITY || calcOnsr == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // For the ADD, degradation brought by the node are calculated from the MW-WR spec.
                    // The Degree is not considered. This means we must bypass the add-link (ADD)
                    // and the next node (Degree) which are not considered in the impairments.
                    pathElement++;
                    bypassDegree = 1;
                    break;
                case DEGREE:
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        for (int pathElement = 2 + bypassDegree; pathElement < vertices.size() - 1; pathElement++) {
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement + 1)));
            LOG.debug("loop of check OSNR direction AZ: Path Element = {}", pathElement);
            switch (currentNode.getORNodeType()) {
                case SRG:
                    LOG.debug("loop of check OSNR direction AZ: SRG, Path Element = {}", pathElement);
                    // Other case is DROP, for which cnt is unchanged (.DROP)
                    if (edges.get(pathElement - 1).link().getlinkType() != OpenroadmLinkType.DROPLINK) {
                        LOG.error("Error processing Node {} for which input link {} is not a DROPLINK Type",
                            currentNode.getNodeId(), pathElement - 1);
                    }
                    PceLink pceLink = edges.get(pathElement - 2).link();
                    LOG.info("loop of check OSNR : SRG, pathElement = {} CD on preceeding link {} = {} ps",
                        pathElement, pathElement - 2, pceLink.getcd());
                    calcDropContrib(cu, signal, currentNode, pceLink);
                    double calcOnsr = signal.get("calcOnsrLin").doubleValue();
                    if (calcOnsr == Double.NEGATIVE_INFINITY || calcOnsr == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // If SRG is not the first or the second element of the Path, it is the DROP
                    // side.
                    // After accumulated degradations are calculated, we also need to calculate
                    // resulting OSNR in dB to pass it to the method that verifies end Xponder
                    // performances are compatible with degradations experienced on the path
                    try {
                        calcOnsrdB = getOsnrDbfromOnsrLin(calcOnsr);
                        LOG.info("checkOSNR loop, last SRG osnr is {} dB", calcOnsrdB);
                        LOG.info("Loop pathElement = {}, DROP, calcOnsrdB= {}", pathElement, calcOnsrdB);
                    } catch (ArithmeticException e) {
                        LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}",
                            path.getEdgeList().size());
                        return -1.0;
                    }
                    break;
                case DEGREE:
                    if (nextNode.getORNodeType() != OpenroadmNodeType.DEGREE) {
                        //This is the case of DROP, ROADM degree is not considered
                        break;
                    }
                    LOG.info("loop of check OSNR direction AZ: DEGREE, Path Element = {}", pathElement);
                    calcBypassContrib(cu, signal, currentNode, nextNode,
                        edges.get(pathElement - 1).link(), edges.get(pathElement + 1).link());
                    double calcOnsrLin = signal.get("calcOnsrLin").doubleValue();
                    LOG.debug(
                        "Loop pathElement= {}, DEGREE, calcOnsrdB= {}", pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // increment pathElement so that in next step we will not point to Degree2 but
                    // next node
                    pathElement++;
                    LOG.info("Accumulated degradations in the path including ROADM {} + {} are CD: {}; PMD2: "
                        + "{}; Pdl2 : {}; ONSRdB : {}", currentNode.getNodeId(), nextNode.getNodeId(),
                        signal.get("calcCd"), signal.get("calcPmd2"), signal.get("calcPdl2"),
                        getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                case XPONDER:
                    LOG.debug("loop of check OSNR direction AZ: XPDR, Path Element = {}", pathElement);
                    LOG.error("unsupported back to back transponder configuration");
                    return -1.0;
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        double margin = 0;
        PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(vertices.size() - 1)));
        LOG.debug("loop of check OSNR, Path Element = {}", vertices.size() - 1);
        switch (currentNode.getORNodeType()) {
            case XPONDER:
                LOG.debug("loop of check OSNR direction AZ: XPDR, Path Element = {}", vertices.size() - 1);
                transponderPresent = true;
                // TSP is the last of the path
                margin = getLastXpdrMargin(cu, signal, edges.get(vertices.size() - 2).link().getDestTP().getValue(),
                    serviceType, currentNode, vertices.get(vertices.size() - 1), vertices.size() - 1);
                break;
            case SRG:
                LOG.debug("loop of check OSNR direction AZ: SRG, Path Element = {}", vertices.size() - 1);
                // Other case is DROP, for which cnt is unchanged (.DROP)
                if (edges.get(vertices.size() - 2).link().getlinkType() != OpenroadmLinkType.DROPLINK) {
                    LOG.error("Error processing Node {} for which input link {} is not a DROPLINK Type",
                        currentNode.getNodeId(), vertices.size() - 2);
                }
                PceLink pceLink = edges.get(vertices.size() - 3).link();
                LOG.info("loop of check OSNR : SRG, pathElement = {} CD on preceeding link {} = {} ps",
                    vertices.size() - 1, vertices.size() - 3, pceLink.getcd());
                calcDropContrib(cu, signal, currentNode, pceLink);
                double calcOnsr = signal.get("calcOnsrLin").doubleValue();
                //commented out to avoid spotbug DLS_DEAD_LOCAL_STORE pwrIn = impairments.get("pwrIn");
                if (calcOnsr == Double.NEGATIVE_INFINITY || calcOnsr == Double.POSITIVE_INFINITY) {
                    return -1.0;
                }
                // If SRG is not the first or the second element of the Path, it is the DROP
                // side.
                // After accumulated degradations are calculated, we also need to calculate
                // resulting OSNR in dB to pass it to the method that verifies end Xponder
                // performances are compatible with degradations experienced on the path
                try {
                    calcOnsrdB = getOsnrDbfromOnsrLin(calcOnsr);
                    LOG.info("checkOSNR loop, last SRG osnr is {} dB", calcOnsrdB);
                    LOG.info("Loop pathElement = {}, DROP, calcOnsrdB= {}", vertices.size() - 1, calcOnsrdB);
                } catch (ArithmeticException e) {
                    LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}",
                        path.getEdgeList().size());
                    return -1.0;
                }
                break;
            case DEGREE:
            default:
                LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain last element");
        }
        LOG.info("- In checkOSNR: accumulated CD = {} ps, PMD = {} ps, PDL = {} dB, and resulting OSNR calcOnsrdB = {} "
            + "dB and ONSR dB exterapolated from calcosnrlin = {} including non linear contributions",
            signal.get("calcCd"), Math.sqrt(signal.get("calcPmd2").doubleValue()),
            Math.sqrt(signal.get("calcPdl2").doubleValue()), calcOnsrdB,
            getOsnrDbfromOnsrLin(signal.get("calcOnsrLin").doubleValue()));
        if (!transponderPresent) {
            LOG.info("No transponder in the path, User shall check from CD, PMD, and OSNR values provided "
                + "that optical tunnel degradations are compatible with external transponder performances");
            return 0.0;
        }
        double delta = margin - SYS_MARGIN;
        LOG.info("In checkOSNR: Transponder Operational mode results in a residual margin of {} dB, according "
            + "to CD, PMD and DGD induced penalties and set System Margin of {} dB.",
            delta, SYS_MARGIN);
        String validationMessage = delta >= 0 ? "VALIDATED" : "INVALIDATED";
        LOG.info("- In checkOSNR: A to Z Path from {} to {} {}",
                vertices.get(0), vertices.get(vertices.size() - 1), validationMessage);
        return delta;
    }

    /**
     * Calculates the OSNR of a path, according to the direction (AtoZ/ZtoA), using the operational-modes Catalog.
     *
     * @param path                      the AtoZ path provided by the PCE.
     * @param allPceNode                The map of chosen/relevant PceNodes build from topology pruning.
     * @param allPceLinks               The map of PceLinks build corresponding to the whole topology.
     * @param serviceType               The service Type used to extrapolate Operational mode when it is not provided.
     * @param cu                        CatalogUtils instance.
     * @return the calculated margin according to the Transponder performances and path impairments.
     */
    private double checkOSNRza(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, String serviceType, CatalogUtils cu) {
        Map<String, Double> signal = new HashMap<>(
            Map.of(
                "spacing", Double.valueOf(50.0),
                "calcPdl2", Double.valueOf(0),
                "calcCd", Double.valueOf(0),
                "calcPmd2", Double.valueOf(0),
                "calcOnsrLin", Double.valueOf(0.0001),
                "pwrIn", Double.valueOf(-60.0),
                "pwrOut", Double.valueOf(-60.0)));
        double calcOnsrdB = 0;
        boolean transponderPresent = false;
        List<String> vertices = path.getVertexList();
        List<PceGraphEdge> edges = path.getEdgeList();
        // LOOP that scans the different Nodes/Links of the path and calculates
        // associated degradations
        // using CatalogUtils primitives to retrieve physical parameters and make a
        // first level calculation
        int bypassDegree = 0;
        for (int pathElement = vertices.size() - 1; pathElement > vertices.size() - 3; pathElement--) {
            bypassDegree = 0;
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement - 1)));
            LOG.debug("loop of check OSNR direction ZA:  Path Element = {}", pathElement);
            switch (currentNode.getORNodeType()) {
                case XPONDER:
                    LOG.debug("loop of check OSNR direction ZA: XPDR, Path Element = {}", pathElement);
                    transponderPresent = true;
                    calcXpdrOSNR(cu, signal,
                        pathElement == vertices.size() - 1
                            // First transponder on the Path (TX side) / Last Xponder of the path (RX side)
                            ? getOppPceLink(pathElement - 1, edges, allPceLinks).getSourceTP().getValue()
                            : getOppPceLink((pathElement), edges, allPceLinks).getDestTP().getValue(),
                        serviceType, currentNode, nextNode, vertices.get(pathElement), pathElement);
                    break;
                case SRG:
                    LOG.debug("loop of check OSNR direction ZA: SRG, Path Element = {}", pathElement);
                    // This is ADD case : First (optical-tunnel) or 2nd (Regular E2E service from
                    // Xponder to Xponder) node element of the path is the ADD SRG.
                    if (getOppPceLink(pathElement - 1, edges, allPceLinks).getlinkType() != OpenroadmLinkType.ADDLINK) {
                        LOG.error("Error processing Node {} for which output link {} is not an ADDLINK Type",
                            currentNode.getNodeId(), pathElement - 1);
                    }
                    signal.put("pwrIn", Double.valueOf(0));
                    calcAddContrib(cu, signal, currentNode, getOppPceLink(pathElement - 2, edges, allPceLinks));
                    double calcOnsr = signal.get("calcOnsrLin").doubleValue();
                    if (calcOnsr == Double.NEGATIVE_INFINITY || calcOnsr == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // For the ADD, degradation brought by the node are calculated from the MW-WR spec.
                    // The Degree is not considered. This means we must bypass the add-link (ADD)
                    // and the next node (Degree) which are not considered in the impairments.
                    pathElement--;
                    bypassDegree = 1;
                    break;
                case DEGREE:
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        for (int pathElement = vertices.size() - 3 - bypassDegree; pathElement > 0; pathElement--) {
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement - 1)));
            LOG.debug("loop of check OSNR direction ZA: Path Element = {}", pathElement);
            switch (currentNode.getORNodeType()) {
                case SRG:
                    LOG.debug("loop of check OSNR direction ZA: SRG, Path Element = {}", pathElement);
                    if (getOppPceLink(pathElement, edges, allPceLinks).getlinkType() != OpenroadmLinkType.DROPLINK) {
                        LOG.error("Error processing Node {} for which input link {} is not a DROPLINK Type",
                            currentNode.getNodeId(), pathElement);
                    }
                    PceLink pceLink = getOppPceLink(pathElement + 1, edges, allPceLinks);
                    LOG.info("loop of check OSNR direction ZA: SRG, path Element = {} CD on preceeding link {} = {} ps",
                        pathElement, pathElement + 1, pceLink.getcd());
                    calcDropContrib(cu, signal, currentNode, pceLink);
                    double calcOnsr = signal.get("calcOnsrLin").doubleValue();
                    if (calcOnsr == Double.NEGATIVE_INFINITY || calcOnsr == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // If SRG is not the first or the second element of the Path, it is the DROP
                    // side.
                    // After accumulated degradations are calculated, we also need to calculate
                    // resulting OSNR in dB to pass it to the method that verifies end Xponder
                    // performances are compatible with degradations experienced on the path
                    try {
                        calcOnsrdB = getOsnrDbfromOnsrLin(calcOnsr);
                        LOG.info("checkOSNR loop, last SRG osnr is {} dB", calcOnsrdB);
                        LOG.info("Loop Path Element = {}, DROP, calcOnsrdB= {}", pathElement, calcOnsrdB);
                    } catch (ArithmeticException e) {
                        LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}",
                            path.getEdgeList().size());
                        return -1.0;
                    }
                    break;
                case DEGREE:
                    if (nextNode.getORNodeType() != OpenroadmNodeType.DEGREE) {
                        //This is the case of DROP, ROADM degree is not considered
                        break;
                    }
                    LOG.info("loop of check OSNR direction ZA: DEGREE, Path Element = {}", pathElement);
                    calcBypassContrib(cu, signal, currentNode, nextNode,
                        getOppPceLink(pathElement, edges, allPceLinks),
                        getOppPceLink(pathElement - 2, edges, allPceLinks));
                    double calcOnsrLin = signal.get("calcOnsrLin").doubleValue();
                    LOG.debug("Loop Path Element = {}, DEGREE, calcOnsrdB= {}",
                            pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // increment pathElement so that in next step we will not point to Degree2 but
                    // next node
                    pathElement--;
                    LOG.info("Accumulated degradations in the path including ROADM {} + {} are CD: {}; PMD2: "
                        + "{}; Pdl2 : {}; ONSRdB : {}", currentNode.getNodeId(), nextNode.getNodeId(),
                        signal.get("calcCd"), signal.get("calcPmd2"), signal.get("calcPdl2"),
                        getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                case XPONDER:
                    LOG.debug("loop of check OSNR direction AZ: XPDR, Path Element = {}", pathElement);
                    LOG.error("unsupported back to back transponder configuration");
                    return -1.0;
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        double margin = 0;
        PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(0)));
        LOG.debug("loop of check OSNR direction ZA: Path Element = 0");
        switch (currentNode.getORNodeType()) {
            case XPONDER:
                LOG.debug("loop of check OSNR direction ZA: XPDR, Path Element = 0");
                transponderPresent = true;
                // TSP is the last of the path
                margin = getLastXpdrMargin(cu, signal, getOppPceLink(0, edges, allPceLinks).getDestTP().getValue(),
                    serviceType, currentNode, vertices.get(0), 0);
                break;
            case SRG:
                LOG.debug("loop of check OSNR direction ZA: SRG, Path Element = 0");
                if (getOppPceLink(0, edges, allPceLinks).getlinkType() != OpenroadmLinkType.DROPLINK) {
                    LOG.error("Error processing Node {} for which input link 0 is not a DROPLINK Type",
                        currentNode.getNodeId());
                }
                PceLink pceLink = getOppPceLink(1, edges, allPceLinks);
                LOG.info("loop of check OSNR direction ZA: SRG, path Element = 0 CD on preceeding link 1 = {} ps",
                    pceLink.getcd());
                calcDropContrib(cu, signal, currentNode, pceLink);
                double calcOnsr = signal.get("calcOnsrLin").doubleValue();
                //commented out to avoid spotbug DLS_DEAD_LOCAL_STORE pwrIn = impairments.get("pwrIn");
                if (calcOnsr == Double.NEGATIVE_INFINITY || calcOnsr == Double.POSITIVE_INFINITY) {
                    return -1.0;
                }
                // If SRG is not the first or the second element of the Path, it is the DROP
                // side.
                // After accumulated degradations are calculated, we also need to calculate
                // resulting OSNR in dB to pass it to the method that verifies end Xponder
                // performances are compatible with degradations experienced on the path
                try {
                    calcOnsrdB = getOsnrDbfromOnsrLin(calcOnsr);
                    LOG.info("checkOSNR loop, last SRG osnr is {} dB", calcOnsrdB);
                    LOG.info("Loop Path Element = 0, DROP, calcOnsrdB= {}", calcOnsrdB);
                } catch (ArithmeticException e) {
                    LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}",
                        path.getEdgeList().size());
                    return -1.0;
                }
                break;
            case DEGREE:
            default:
                LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain last element");
        }
        LOG.info("- In checkOSNR: accumulated CD = {} ps, PMD = {} ps, PDL = {} dB, and resulting OSNR calcOnsrdB = {} "
            + "dB and ONSR dB exterapolated from calcosnrlin = {} including non linear contributions",
            signal.get("calcCd"), Math.sqrt(signal.get("calcPmd2").doubleValue()),
            Math.sqrt(signal.get("calcPdl2").doubleValue()), calcOnsrdB,
            getOsnrDbfromOnsrLin(signal.get("calcOnsrLin").doubleValue()));
        if (!transponderPresent) {
            LOG.info("No transponder in the path, User shall check from CD, PMD, and OSNR values provided "
                + "that optical tunnel degradations are compatible with external transponder performances");
            return 0.0;
        }
        double delta = margin - SYS_MARGIN;
        LOG.info("In checkOSNR: Transponder Operational mode results in a residual margin of {} dB, according "
            + "to CD, PMD and DGD induced penalties and set System Margin of {} dB.",
            delta, SYS_MARGIN);
        String validationMessage = delta >= 0 ? "VALIDATED" : "INVALIDATED";
        LOG.info("- In checkOSNR: Z to A Path from {} to {} {}",
                vertices.get(vertices.size() - 1), vertices.get(0), validationMessage);
        return delta;
    }

    private String setOpMode(String opMode, String defaultMode) {
        return
            opMode == null || opMode.isEmpty() || opMode.contentEquals(StringConstants.UNKNOWN_MODE)
                ? defaultMode
                : opMode;
    }

    private PceLink getOppPceLink(Integer pathEltNber, List<PceGraphEdge> edges,
            Map<LinkId, PceLink> allPceLinks) {
        return allPceLinks.get(new LinkId(edges.get(pathEltNber).link().getOppositeLink()));
    }

    private String getXpdrOpMode(String nwTpId, String vertice, int pathElement, PceNode currentNode,
            String serviceType, CatalogUtils cu) {
        DataObjectIdentifier<TerminationPoint1> nwTpIid =
                InstanceIdentifiers.createNetworkTerminationPoint1IIDBuilder(vertice, nwTpId);
        String opMode = cu.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP, serviceType);
        try {
            if (networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, nwTpIid).get().isPresent()) {
                // If the operational mode of the Xponder is not consistent nor declared in the topology (Network TP)
                opMode = setOpMode(
                    currentNode.getXponderOperationalMode(
                        networkTransactionService
                                .read(LogicalDatastoreType.CONFIGURATION, nwTpIid)
                                .get().orElseThrow().getXpdrNetworkAttributes()),
                    // Operational mode is found as an attribute of the network TP
                    opMode);
                    // Operational mode is retrieved from the service Type assuming it is supported
                    // by the Xponder
                LOG.debug(
                    "Transponder {} corresponding to path Element {} in the path has {} operational mode",
                    currentNode.getNodeId().getValue(), pathElement, opMode);
                return opMode;
            }
        } catch (InterruptedException | ExecutionException e1) {
            LOG.error("Issue accessing the XponderNetworkAttributes of {} for Transponder {}"
                + " corresponding to path Element {} in the path ",
                nwTpId, currentNode.getNodeId().getValue(), pathElement);
        }
        LOG.info("Did not succeed finding network TP {} in Configuration Datastore. Retrieve"
            + " default Operational Mode {} from serviceType {}", nwTpId, opMode, serviceType);
        return opMode;
    }

    private double getLastXpdrMargin(
            CatalogUtils cu, Map<String, Double> signal,
            String nwTpId, String serviceType, PceNode currentNode, String vertice, int pathElement) {
        LOG.debug("Loop Path Element = {}, Step5.1, XPDR, tries calculating Margin, just before call", pathElement);
        // Check that accumulated degradations are compatible with TSP performances
        // According to OpenROADM spec :
        // margin = cu.getPceRxTspParameters(opMode, calcCd, Math.sqrt(calcPmd2), Math.sqrt(calcPdl2),
        //              getOsnrDbfromOnsrLin(calcOnsrLin));
        // Calculation modified for pdl according to calculation in Julia's Tool
        double calcOnsrdB = getOsnrDbfromOnsrLin(signal.get("calcOnsrLin").doubleValue());
        LOG.info("Loop Path Element = {}, XPDR, calcosnrdB= {}", pathElement, calcOnsrdB);
        return cu.getPceRxTspParameters(
            getXpdrOpMode(nwTpId, vertice, pathElement, currentNode, serviceType, cu),
            signal.get("calcCd").doubleValue(),
            Math.sqrt(signal.get("calcPmd2").doubleValue()),
            Math.sqrt(signal.get("calcPdl2").doubleValue()),
            calcOnsrdB);
    }

    private void calcXpdrOSNR(
            CatalogUtils cu, Map<String, Double> signal, String nwTpId, String serviceType,
            PceNode currentNode, PceNode nextNode, String vertice, int pathElement) {
        // If the Xponder operational mode (setOpMode Arg1) is not consistent nor declared in the topology (Network TP)
        // Operational mode is retrieved from the service Type assuming it is supported by the Xponder (setOpMode Arg2)
        String opMode = getXpdrOpMode(nwTpId, vertice, pathElement, currentNode, serviceType, cu);
        // If the operational mode of the ADD/DROP MUX is not consistent nor declared in the topology (Network TP)
        // Operational mode is set by default to standard opMode for ADD SRGs
        String adnMode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWWRCORE);
        double calcOnsrLin = cu.getPceTxTspParameters(opMode, adnMode);
        LOG.debug(
            "Transponder {} corresponding to path Element {} is connected to SRG which has {} operational mode",
            currentNode.getNodeId().getValue(), pathElement, adnMode);
        LOG.info("Transponder {} corresponding to path Element {} in the path has a TX OSNR of {} dB",
            currentNode.getNodeId().getValue(), pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
        // Return the Tx ONSR of the Xponder which results from IB and OOB OSNR contributions
        // and the spacing associated with Xponder operational mode that is needed to calculate OSNR
        signal.put("spacing", Double.valueOf(cu.getPceTxTspChannelSpacing(opMode)));
        signal.put("calcOnsrLin", Double.valueOf(calcOnsrLin));
    }

    private void calcDropContrib(
            CatalogUtils cu, Map<String, Double> signal, PceNode currentNode, PceLink pceLink) {
        //calculation of the SRG contribution for Drop
        calcLineDegradation(cu, signal, pceLink);
        Map<String, Double> impairments = cu.getPceRoadmAmpParameters(
            CatalogConstant.CatalogNodeType.DROP,
            setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWWRCORE),
        // If the operational mode of the ADD/DROP MUX is not consistent or not declared in the topology (Network TP)
        // Operational mode is set by default to standard opMode for ADD/DROP SRGs
            signal.get("pwrIn").doubleValue(),
            signal.get("calcCd").doubleValue(),
            signal.get("calcPmd2").doubleValue(),
            signal.get("calcPdl2").doubleValue(),
            signal.get("calcOnsrLin").doubleValue(),
            signal.get("spacing").doubleValue());
        signal.putAll(
            Map.of(
                "calcCd", impairments.get("CD"),
                "calcPmd2", impairments.get("DGD2"),
                "calcPdl2", impairments.get("PDL2"),
                "calcOnsrLin", impairments.get("ONSRLIN")));
    }

    private void calcAddContrib(
            CatalogUtils cu, Map<String, Double> signal, PceNode currentNode, PceLink pceLink) {
        //calculation of the SRG contribution for Add
        String srgMode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWWRCORE);
        // If the operational mode of the ADD/DROP MUX is not consistent or is not declared in the topology (Network TP)
        // Operational mode is set by default to standard opMode for ADD/DROP SRGs
        CatalogNodeType cnt = CatalogConstant.CatalogNodeType.ADD;
        double pwrOut = cu.getPceRoadmAmpOutputPower(
                cnt, srgMode, pceLink.getspanLoss(), signal.get("spacing").doubleValue(), pceLink.getpowerCorrection());
        //calculation of the SRG contribution either for Add and Drop
        Map<String, Double> impairments = cu.getPceRoadmAmpParameters(cnt, srgMode, 0,
            signal.get("calcCd").doubleValue(), signal.get("calcPmd2").doubleValue(),
            signal.get("calcPdl2").doubleValue(),
            signal.get("calcOnsrLin").doubleValue(), signal.get("spacing").doubleValue());
        signal.putAll(
            Map.of(
                "calcCd", impairments.get("CD"),
                "calcPmd2", impairments.get("DGD2"),
                "calcPdl2", impairments.get("PDL2"),
                "calcOnsrLin", impairments.get("ONSRLIN"),
                "pwrOut", Double.valueOf(pwrOut)));
    }

    private void calcBypassContrib(CatalogUtils cu, Map<String, Double> signal,
            PceNode currentNode, PceNode nextNode, PceLink pceLink0, PceLink pceLink1) {
        // If the operational mode of the Degree is not consistent or declared in the topology
        // Operational mode is set by default to standard opMode for Degree
        String degree1Mode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWMWCORE);
        // Same for next node which is the second degree of a ROADM node
        String degree2Mode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWMWCORE);
        // At that time OpenROADM provides only one spec for the ROADM nodes
        if (!degree1Mode.equals(degree2Mode)) {
            LOG.warn("Unsupported Hybrid ROADM configuration with Degree1 {} of {} operational mode and Degree2 "
                + "{} of {} operational mode. Will by default use operational mode of Degree2",
                currentNode.getNodeId(), degree1Mode, nextNode.getNodeId(), degree2Mode);
        }
        calcLineDegradation(cu, signal, pceLink0);
        CatalogNodeType cnt = CatalogConstant.CatalogNodeType.EXPRESS;
        double pwrOut = cu.getPceRoadmAmpOutputPower(cnt, degree2Mode, pceLink1.getspanLoss(),
            signal.get("spacing").doubleValue(), pceLink1.getpowerCorrection());
        // Adds to accumulated impairments the degradation associated with the Express
        // path of ROADM : Degree1, express link, Degree2
        Map<String, Double> impairments = cu.getPceRoadmAmpParameters(cnt, degree2Mode,
            signal.get("pwrIn").doubleValue(), signal.get("calcCd").doubleValue(),
            signal.get("calcPmd2").doubleValue(), signal.get("calcPdl2").doubleValue(),
            signal.get("calcOnsrLin").doubleValue(), signal.get("spacing").doubleValue());
        signal.putAll(
            Map.of(
                "calcCd", impairments.get("CD"),
                "calcPmd2", impairments.get("DGD2"),
                "calcPdl2", impairments.get("PDL2"),
                "calcOnsrLin", impairments.get("ONSRLIN"),
                "pwrOut", Double.valueOf(pwrOut)));
    }
    //TODO these methods might be more indicated in a catalog utils refactoring

    private void calcLineDegradation(CatalogUtils cu, Map<String, Double> signal, PceLink pceLink) {
        // Calculate degradation accumulated across incoming Link and add them to
        // accumulated impairments
        // This also includes Non Linear Contribution from the path
        signal.putAll(Map.of(
            "pwrIn", Double.valueOf(signal.get("pwrOut").doubleValue() - pceLink.getspanLoss()),
            "calcCd", Double.valueOf(signal.get("calcCd").doubleValue() + pceLink.getcd()),
            "calcPmd2", Double.valueOf(signal.get("calcPmd2").doubleValue() + pceLink.getpmd2()),
            "calcOnsrLin", Double.valueOf(
                signal.get("calcOnsrLin").doubleValue()
                + cu.calculateNLonsrContribution(
                    signal.get("pwrOut").doubleValue(), pceLink.getLength(), signal.get("spacing").doubleValue()))));
    }

    private double getOsnrDbfromOnsrLin(double osnrLu) {
        return 10 * Math.log10(1 / osnrLu);
    }

    /**
     * Get spectrum assignment for path.
     *
     * @param path                    the path for which we get spectrum assignment.
     * @param allPceNodes             all optical nodes.
     * @param spectralWidthSlotNumber number of slot for spectral width. Depends on
     *                                service type.
     * @param subscriber              will be notified about errors.
     * @return a spectrum assignment object which contains begin and end index. If
     *         no spectrum assignment found, beginIndex = stopIndex = 0
     */
    public SpectrumAssignment getSpectrumAssignment(GraphPath<String, PceGraphEdge> path,
            Map<NodeId, PceNode> allPceNodes, int spectralWidthSlotNumber, Subscriber subscriber) {
        byte[] freqMap = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(freqMap, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        BitSet result = BitSet.valueOf(freqMap);
        boolean isFlexGrid = true;
        LOG.debug("Processing path {} with length {}", path, path.getLength());
        BitSet pceNodeFreqMap;
        Set<PceNode> pceNodes = new LinkedHashSet<>();

        for (PceGraphEdge edge : path.getEdgeList()) {
            NodeId srcId = edge.link().getSourceId();
            NodeId dstId = edge.link().getDestId();
            LOG.debug("Processing {} to {}", srcId.getValue(), dstId.getValue());
            if (allPceNodes.containsKey(srcId)) {
                pceNodes.add(allPceNodes.get(srcId));
            }
            if (allPceNodes.containsKey(dstId)) {
                pceNodes.add(allPceNodes.get(dstId));
            }
        }

        Collection centerFrequencyGranularityCollection = new CenterFrequencyGranularityCollection(50);
        CapabilityCollection mcCapabilityCollection = new McCapabilityCollection(
            message -> subscriber.event(Level.ERROR, message));

        for (PceNode pceNode : pceNodes) {
            LOG.debug("Processing PCE node {}", pceNode);
            pceNodeFreqMap = pceNode.getBitSetData();
            LOG.debug("Pce node bitset {}", pceNodeFreqMap);
            if (pceNodeFreqMap != null) {
                result.and(pceNodeFreqMap);
                LOG.debug("intermediate bitset {}", result);
            }
            centerFrequencyGranularityCollection.add(pceNode.getCentralFreqGranularity());
            mcCapabilityCollection.add(
                new InterfaceMcCapability(
                    pceNode.getNodeId().getValue(),
                    pceNode.getSlotWidthGranularity(),
                    pceNode.getMinSlots(),
                    pceNode.getMaxSlots()
                )
            );
            String pceNodeVersion = pceNode.getVersion();
            BigDecimal sltWdthGran = pceNode.getSlotWidthGranularity();
            if (StringConstants.OPENROADM_DEVICE_VERSION_1_2_1.equals(pceNodeVersion)) {
                isFlexGrid = false;
            }

            LOG.debug(
                "Node {}: version is {} with slot width and central frequency granularities {} {}, flex grid = {}",
                pceNode.getNodeId(), pceNodeVersion, sltWdthGran, pceNode.getCentralFreqGranularity(), isFlexGrid);
        }

        LOG.debug("Available bitset on nodes: {}", result);

        if (result.isEmpty()) {
            subscriber.error("No frequencies available");
            return createEmptySpectrumAssignment();
        }

        int slotCount = clientInput.slotWidth(spectralWidthSlotNumber);

        if (!mcCapabilityCollection.isCompatibleService(GridConstant.GRANULARITY, slotCount)) {
            return createEmptySpectrumAssignment();
        }

        Select frequencySelectionFactory = new FrequencySelectionFactory();

        BitSet assignableBitset = frequencySelectionFactory.availableFrequencies(
                clientInput,
                spectrumConstraint,
                result);

        LOG.debug("Assignable bitset: {}", assignableBitset);

        if (assignableBitset.isEmpty()) {
            subscriber.error("No frequencies are assignable to the service.");
            return createEmptySpectrumAssignment();
        }

        return computeBestSpectrumAssignment(
            assignableBitset,
            clientInput.slotWidth(spectralWidthSlotNumber),
            centerFrequencyGranularityCollection.slots(GridConstant.GRANULARITY),
            isFlexGrid,
            subscriber);
    }

    private SpectrumAssignment createEmptySpectrumAssignment() {
        return new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(0))
            .setStopIndex(Uint16.valueOf(0))
            .setFlexGrid(true)
            .build();
    }

    /**
     * Compute spectrum assignment from spectrum occupation for spectral width.
     *
     * @param spectrumOccupation                   the spectrum occupation BitSet.
     * @param spectralWidthSlotNumber              the nb slots for spectral width.
     * @param nrOfSlotsSeparatingCenterFrequencies The nr of slots separating each central frequency.
     * @param isFlexGrid                           true if flexible grid, false otherwise.
     * @return a spectrum assignment object which contains begin and stop index. If
     *         no spectrum assignment found, beginIndex = stopIndex = 0
     */
    public SpectrumAssignment computeBestSpectrumAssignment(
        BitSet spectrumOccupation,
        int spectralWidthSlotNumber,
        int nrOfSlotsSeparatingCenterFrequencies,
        boolean isFlexGrid) {

        return computeBestSpectrumAssignment(
            spectrumOccupation,
            spectralWidthSlotNumber,
            nrOfSlotsSeparatingCenterFrequencies,
            isFlexGrid,
            new Ignore());
    }

    /**
     * Compute spectrum assignment from spectrum occupation for spectral width.
     *
     * @param spectrumOccupation                   the spectrum occupation BitSet.
     * @param spectralWidthSlotNumber              the nb slots for spectral width.
     * @param nrOfSlotsSeparatingCenterFrequencies The nr of slots separating each central frequency.
     * @param isFlexGrid                           true if flexible grid, false otherwise.
     * @param subscriber                           will be notified about errors.
     * @return a spectrum assignment object which contains begin and stop index. If
     *         no spectrum assignment found, beginIndex = stopIndex = 0
     */
    public SpectrumAssignment computeBestSpectrumAssignment(
        BitSet spectrumOccupation,
        int spectralWidthSlotNumber,
        int nrOfSlotsSeparatingCenterFrequencies,
        boolean isFlexGrid,
        Subscriber subscriber) {

        Base baseFrequency = new BaseFrequency();
        Assign assignSpectrum = new AssignSpectrum(new SpectrumIndex());
        Range range = assignSpectrum.range(
            GridConstant.EFFECTIVE_BITS,
            baseFrequency.referenceFrequencySpectrumIndex(
                GridConstant.CENTRAL_FREQUENCY,
                GridConstant.START_EDGE_FREQUENCY,
                GridConstant.GRANULARITY
            ),
            spectrumOccupation,
            nrOfSlotsSeparatingCenterFrequencies,
            spectralWidthSlotNumber
        );

        if (range.lower() == 0 && range.upper() == 0) {
            subscriber.event(Level.ERROR, "No frequencies available.");
        }

        return new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(range.lower()))
            .setStopIndex(Uint16.valueOf(range.upper()))
            .setFlexGrid(isFlexGrid)
            .build();
    }

    public Double getTpceCalculatedMargin() {
        return tpceCalculatedMargin;
    }
}
