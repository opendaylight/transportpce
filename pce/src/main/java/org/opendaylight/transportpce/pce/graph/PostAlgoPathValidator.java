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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.catalog.CatalogConstant;
import org.opendaylight.transportpce.common.catalog.CatalogConstant.CatalogNodeType;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.SpectrumAssignmentBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAlgoPathValidator {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PostAlgoPathValidator.class);

    public static final Long CONST_OSNR = 1L;
    public static final double SYS_MARGIN = 0;
    private Double tpceCalculatedMargin = 0.0;
    private final NetworkTransactionService networkTransactionService;

    public PostAlgoPathValidator(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @SuppressWarnings("fallthrough")
    @SuppressFBWarnings(
        value = "SF_SWITCH_FALLTHROUGH",
        justification = "intentional fallthrough")

    public PceResult checkPath(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, PceResult pceResult, PceConstraints pceHardConstraints,
            String serviceType) {
        LOG.info("path = {}", path);
        // check if the path is empty
        if (path.getEdgeList().isEmpty()) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
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
                spectrumAssignment = getSpectrumAssignment(path, allPceNodes, spectralWidthSlotNumber);
                pceResult.setServiceType(serviceType);
                if (spectrumAssignment.getBeginIndex().equals(Uint16.valueOf(0))
                        && spectrumAssignment.getStopIndex().equals(Uint16.valueOf(0))) {
                    pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
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
                        pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
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
                    pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                    pceResult.setLocalCause(PceResult.LocalCause.TOO_HIGH_LATENCY);
                    return pceResult;
                }
                // Check if nodes are included in the hard constraints
                if (!checkInclude(path, pceHardConstraints)) {
                    pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                    pceResult.setLocalCause(PceResult.LocalCause.HD_NODE_INCLUDE);
                    return pceResult;
                }
                // TODO here other post algo validations can be added
                // more data can be sent to PceGraph module via PceResult structure if required
                pceResult.setRC(ResponseCodes.RESPONSE_OK);
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
                pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                pceResult.setServiceType(serviceType);
                Map<String, List<Uint16>> tribSlot = chooseTribSlot(path, allPceNodes, tribSlotNb);
                Map<String, Uint16> tribPort = chooseTribPort(path, allPceNodes, tribSlot, tribSlotNb);
                List<OpucnTribSlotDef> resultTribPortTribSlot = getMinMaxTpTs(tribPort, tribSlot);
                if (resultTribPortTribSlot.get(0) != null && resultTribPortTribSlot.get(1) != null) {
                    pceResult.setResultTribPortTribSlot(resultTribPortTribSlot);
                    pceResult.setRC(ResponseCodes.RESPONSE_OK);
                    LOG.info("In PostAlgoPathValidator: found TribPort {} - tribSlot {} - tribSlotNb {}",
                        tribPort, tribSlot, tribSlotNb);
                }
                return pceResult;
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC2:
            case StringConstants.SERVICE_TYPE_ODUC3:
            case StringConstants.SERVICE_TYPE_ODUC4:
            case StringConstants.SERVICE_TYPE_100GE_S:
                pceResult.setRC(ResponseCodes.RESPONSE_OK);
                pceResult.setServiceType(serviceType);
                LOG.info("In PostAlgoPathValidator: ODU4/ODUCn path found {}", path);
                return pceResult;
            default:
                pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
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
    private boolean checkInclude(GraphPath<String, PceGraphEdge> path, PceConstraints pceHardConstraintsInput) {
        List<ResourcePair> listToInclude = pceHardConstraintsInput.getListToInclude()
            .stream().sorted((rp1, rp2) -> rp1.getName().compareTo(rp2.getName()))
            .collect(Collectors.toList());
        if (listToInclude.isEmpty()) {
            return true;
        }
        List<PceGraphEdge> pathEdges = path.getEdgeList();
        LOG.debug(" in checkInclude vertex list: [{}]", path.getVertexList());
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
        return listOfElementsSubNode.containsAll(
                listToInclude
                    .stream().filter(rp -> PceConstraints.ResourceType.NODE.equals(rp.getType()))
                    .map(ResourcePair::getName).collect(Collectors.toList()))
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
        List<String> listOfElements = new ArrayList<>();
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
        return listOfElements;
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
            Integer startTribSlot = tribSlotMap.values().stream().findFirst().get().get(0).toJava();
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
            List<Uint16> tribSlotList = new ArrayList<>();
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
        List<Uint16> tsList = (List<Uint16>) tribSlot.values().toArray()[0];
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
    @SuppressWarnings("deprecation")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DLS_DEAD_LOCAL_STORE")
    private double checkOSNRaz(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, String serviceType, CatalogUtils cu) {
        double spacing = 50.0;
        double calcPdl2 = 0;
        double calcOsnrdB = 0;
        double calcCd = 0;
        double calcPmd2 = 0;
        double calcOnsrLin = 0.0001;
        double margin = 0;
        double pwrIn = -60.0;
        double pwrOut = -60.0;
        boolean transponderPresent = false;
        List<String> vertices = path.getVertexList();
        List<PceGraphEdge> edges = path.getEdgeList();
        String opMode = "";
        // LOOP that scans the different Nodes/Links of the path and calculates
        // associated degradations
        // using CatalogUtils primitives to retrieve physical parameters and make a
        // first level calculation
        int bypassDegree = 0;
        for (int n = 0; n < 2; n++) {
            int pathElement = n;
            bypassDegree = 0;
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement + 1)));
            LOG.debug("loop of check OSNR, n = {} Path Element = {}", n, pathElement);
            switch (currentNode.getORNodeType()) {
                case XPONDER:
                    transponderPresent = true;
                    String nwTpId =
                            pathElement == 0
                                ? edges.get(pathElement).link().getSourceTP().getValue()
                        // last Xponder of the path (RX side)
                                : edges.get(pathElement - 1).link().getDestTP().getValue();
                    LOG.debug("loop of check OSNR : XPDR, n = {} Path Element = {}", n, pathElement);
// If the Xponder operational mode (setOpMode Arg1) is not consistent or not declared in the topology (Network TP)
// Operational mode is retrieved from the service Type assuming it is supported by the Xponder (setOpMode Arg2)
                    opMode =
                        getXpdrOpMode(nwTpId, vertices.get(pathElement), pathElement, currentNode, serviceType, cu);
                    // TSP is first element of the path . To correctly evaluate the TX OOB OSNR from
                    // its operational mode, we need to know the type of ADD/DROP Mux it is
                    // connected to
                    // If the operational mode of the ADD/DROP MUX is not consistent or
                    // if the operational mode of the ADD/DROP MUX is not declared in the topology
                    // (Network TP)
                        // Operational mode is set by default to standard opMode for ADD SRGs
                    String adnMode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                        // Operational mode is found in SRG attributes of the Node
                    LOG.debug("Transponder {} corresponding to path Element {} in the path is connected to SRG "
                        + "which has {} operational mode", currentNode.getNodeId().getValue(), pathElement,
                        adnMode);
                    // Retrieve the Tx ONSR of the Xponder which results from IB and OOB OSNR
                    // contributions
                    calcOnsrLin = cu.getPceTxTspParameters(opMode, adnMode);
                    // Retrieve the spacing associated with Xponder operational mode that is needed
                    // to calculate OSNR
                    spacing = cu.getPceTxTspChannelSpacing(opMode);
                    LOG.info("Transponder {} corresponding to path Element {} in the path has a TX OSNR of {} dB",
                        currentNode.getNodeId().getValue(), pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                case SRG:
                    LOG.debug("loop of check OSNR : SRG, n = {} Path Element = {}", n, pathElement);
                    // This is ADD case : First (optical-tunnel) or 2nd (Regular E2E service from
                    // Xponder to Xponder) node element of the path is the ADD SRG.
                    if (edges.get(pathElement).link().getlinkType() != OpenroadmLinkType.ADDLINK) {
                        LOG.error("Error processing Node {} for which output link {} is not an ADDLINK Type",
                            currentNode.getNodeId().toString(), pathElement);
                    }
                    pwrIn = 0.0;
                    PceLink pceLink = edges.get(pathElement + 1).link();
// If the operational mode of the ADD/DROP MUX is not consistent or is not declared in the topology (Network TP)
// Operational mode is set by default to standard opMode for ADD/DROP SRGs
                    String srgMode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                    CatalogNodeType cnt = CatalogConstant.CatalogNodeType.ADD;
                    pwrOut = cu.getPceRoadmAmpOutputPower(
                            cnt, srgMode, pceLink.getspanLoss(), spacing, pceLink.getpowerCorrection());
                    LOG.debug("loop of check OSNR : SRG, n = {} link {} Pout = {}",
                        pathElement, pathElement + 1, pwrOut);
                    //calculation of the SRG contribution either for Add and Drop
                    Map<String, Double> impairments = cu.getPceRoadmAmpParameters(cnt, srgMode,
                        pwrIn, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments.get("CD").doubleValue();
                    calcPmd2 = impairments.get("DGD2").doubleValue();
                    calcPdl2 = impairments.get("PDL2").doubleValue();
                    calcOnsrLin = impairments.get("ONSRLIN").doubleValue();
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    impairments.clear();
                    // For the ADD, degradation brought by the node are calculated from the MW-WR spec.
                    // The Degree is not considered. This means we must bypass the add-link (ADD)
                    // and the next node (Degree) which are not considered in the impairments.
                    n++;
                    bypassDegree = 1;
                    break;
                case DEGREE:
                    if (nextNode.getORNodeType() != OpenroadmNodeType.DEGREE) {
                        //This is the case of DROP, ROADM degree is not considered
                        break;
                    }
                    LOG.info("loop of check OSNR : DEGREE, n = {} Path Element = {}", n, pathElement);
                    Map<String, Double> impairments0 =  degreeCheckOSNR(
                            cu, currentNode, nextNode,
                            edges.get(pathElement - 1).link(), edges.get(pathElement + 1).link(),
                            pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments0.get("CD").doubleValue();
                    calcPmd2 = impairments0.get("DGD2").doubleValue();
                    calcPdl2 = impairments0.get("PDL2").doubleValue();
                    calcOnsrLin = impairments0.get("ONSRLIN").doubleValue();
                    //TODO rename impariments0 var and/or adapt catalog utils
                    pwrIn = impairments0.get("pwrIn").doubleValue();
                    pwrOut = impairments0.get("pwrOut").doubleValue();
                    LOG.debug("Loop n = {}, DEGREE, calcOsnrdB= {}", n, getOsnrDbfromOnsrLin(calcOnsrLin));
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // increment pathElement so that in next step we will not point to Degree2 but
                    // next node
                    n++;
                    bypassDegree = 1;
                    LOG.info("Accumulated degradations in the path including ROADM {} + {} are CD: {}; PMD2: "
                        + "{}; Pdl2 : {}; ONSRdB : {}", currentNode.getNodeId().toString(),
                        nextNode.getNodeId().toString(), calcCd, calcPmd2, calcPdl2, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        for (int n = 2 + bypassDegree; n < vertices.size() - 1; n++) {
            int pathElement = n;
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement + 1)));
            LOG.debug("loop of check OSNR, n = {} Path Element = {}", n, pathElement);
            switch (currentNode.getORNodeType()) {
                case XPONDER:
                    transponderPresent = true;
                    String nwTpId = edges.get(pathElement - 1).link().getDestTP().getValue();
                    LOG.debug("loop of check OSNR : XPDR, n = {} Path Element = {}", n, pathElement);
// If the Xponder operational mode (setOpMode Arg1) is not consistent or not declared in the topology (Network TP)
// Operational mode is retrieved from the service Type assuming it is supported by the Xponder (setOpMode Arg2)
                    opMode =
                        getXpdrOpMode(nwTpId, vertices.get(pathElement), pathElement, currentNode, serviceType, cu);
                    // TSP is first element of the path . To correctly evaluate the TX OOB OSNR from
                    // its operational mode, we need to know the type of ADD/DROP Mux it is
                    // connected to
                    // If the operational mode of the ADD/DROP MUX is not consistent or
                    // if the operational mode of the ADD/DROP MUX is not declared in the topology
                    // (Network TP)
                        // Operational mode is set by default to standard opMode for ADD SRGs
                    String adnMode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                        // Operational mode is found in SRG attributes of the Node
                    LOG.debug("Transponder {} corresponding to path Element {} in the path is connected to SRG "
                        + "which has {} operational mode", currentNode.getNodeId().getValue(), pathElement,
                        adnMode);
                    // Retrieve the Tx ONSR of the Xponder which results from IB and OOB OSNR
                    // contributions
                    calcOnsrLin = cu.getPceTxTspParameters(opMode, adnMode);
                    // Retrieve the spacing associated with Xponder operational mode that is needed
                    // to calculate OSNR
                    spacing = cu.getPceTxTspChannelSpacing(opMode);
                    LOG.info("Transponder {} corresponding to path Element {} in the path has a TX OSNR of {} dB",
                        currentNode.getNodeId().getValue(), pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                case SRG:
                    LOG.debug("loop of check OSNR : SRG, n = {} Path Element = {}", n, pathElement);
                    // Other case is DROP, for which cnt is unchanged (.DROP)
                    if (edges.get(pathElement - 1).link().getlinkType() != OpenroadmLinkType.DROPLINK) {
                        LOG.error("Error processing Node {} for which input link {} is not a DROPLINK Type",
                            currentNode.getNodeId().toString(), pathElement - 1);
                    }
                    PceLink pceLink = edges.get(pathElement - 2).link();
                    LOG.info("loop of check OSNR : SRG, n = {} CD on preceeding link {} = {} ps",
                        pathElement, pathElement - 2, pceLink.getcd());
                    Map<String, Double> impairments = calcDegradationOSNR(
                        cu, pceLink, pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments.get("calcCd");
                    calcPmd2 = impairments.get("calcPmd2");
                    calcOnsrLin = impairments.get("calcOnsrLin");
                    pwrIn = impairments.get("pwrIn");
// If the operational mode of the ADD/DROP MUX is not consistent or is not declared in the topology (Network TP)
// Operational mode is set by default to standard opMode for ADD/DROP SRGs
                    String srgMode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                    CatalogNodeType cnt = CatalogConstant.CatalogNodeType.DROP;
                    //calculation of the SRG contribution either for Add and Drop
                    impairments = cu.getPceRoadmAmpParameters(cnt, srgMode,
                        pwrIn, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments.get("CD").doubleValue();
                    calcPmd2 = impairments.get("DGD2").doubleValue();
                    calcPdl2 = impairments.get("PDL2").doubleValue();
                    calcOnsrLin = impairments.get("ONSRLIN").doubleValue();
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // If SRG is not the first or the second element of the Path, it is the DROP
                    // side.
                    // After accumulated degradations are calculated, we also need to calculate
                    // resulting OSNR in dB to pass it to the method that verifies end Xponder
                    // performances are compatible with degradations experienced on the path
                    try {
                        calcOsnrdB = getOsnrDbfromOnsrLin(calcOnsrLin);
                        LOG.info("checkOSNR loop, last SRG osnr is {} dB", calcOsnrdB);
                        LOG.info("Loop n = {}, DROP, calcOsnrdB= {}", n, calcOsnrdB);
                    } catch (ArithmeticException e) {
                        LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}",
                            path.getEdgeList().size());
                        return -1.0;
                    }
                    impairments.clear();
                    break;
                case DEGREE:
                    if (nextNode.getORNodeType() != OpenroadmNodeType.DEGREE) {
                        //This is the case of DROP, ROADM degree is not considered
                        break;
                    }
                    LOG.info("loop of check OSNR : DEGREE, n = {} Path Element = {}", n, pathElement);
                    Map<String, Double> impairments0 =  degreeCheckOSNR(
                            cu, currentNode, nextNode,
                            edges.get(pathElement - 1).link(), edges.get(pathElement + 1).link(),
                            pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments0.get("CD").doubleValue();
                    calcPmd2 = impairments0.get("DGD2").doubleValue();
                    calcPdl2 = impairments0.get("PDL2").doubleValue();
                    calcOnsrLin = impairments0.get("ONSRLIN").doubleValue();
                    //TODO rename impariments0 var and/or adapt catalog utils
                    pwrIn = impairments0.get("pwrIn").doubleValue();
                    pwrOut = impairments0.get("pwrOut").doubleValue();
                    LOG.debug("Loop n = {}, DEGREE, calcOsnrdB= {}", n, getOsnrDbfromOnsrLin(calcOnsrLin));
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // increment pathElement so that in next step we will not point to Degree2 but
                    // next node
                    n++;
                    LOG.info("Accumulated degradations in the path including ROADM {} + {} are CD: {}; PMD2: "
                        + "{}; Pdl2 : {}; ONSRdB : {}", currentNode.getNodeId().toString(),
                        nextNode.getNodeId().toString(), calcCd, calcPmd2, calcPdl2, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(vertices.size() - 1)));
        LOG.debug("loop of check OSNR, n = {} Path Element = {}", vertices.size() - 1, vertices.size() - 1);
        switch (currentNode.getORNodeType()) {
            case XPONDER:
                transponderPresent = true;
                String nwTpId = edges.get(vertices.size() - 2).link().getDestTP().getValue();
                LOG.debug("loop of check OSNR : XPDR, n = {} Path Element = {}",
                        vertices.size() - 1, vertices.size() - 1);
                opMode = getXpdrOpMode(nwTpId,
                        vertices.get(vertices.size() - 1), vertices.size() - 1, currentNode, serviceType, cu);
                // TSP is the last of the path
                LOG.debug(
                    "Loop n = {}, Step5.1, XPDR, tries calculating Margin, just before call", vertices.size() - 1);
                // Check that accumulated degradations are compatible with TSP performances
                // According to OpenROADM spec :
                // margin = cu.getPceRxTspParameters(opMode, calcCd, Math.sqrt(calcPmd2), Math.sqrt(calcPdl2),
                //              getOsnrDbfromOnsrLin(calcOnsrLin));
                // Calculation modified for pdl according to calculation in Julia's Tool
                margin = cu.getPceRxTspParameters(opMode, calcCd, Math.sqrt(calcPmd2),
                    Math.sqrt(calcPdl2), getOsnrDbfromOnsrLin(calcOnsrLin));
                LOG.info("Loop n = {}, XPDR, calcosnrdB= {}", vertices.size() - 1, getOsnrDbfromOnsrLin(calcOnsrLin));
                break;
            case SRG:
            case DEGREE:
            default:
                LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain last element");
        }
        LOG.info("- In checkOSNR: accumulated CD = {} ps, PMD = {} ps, PDL = {} dB, and resulting OSNR calcOsnrdB = {} "
            + "dB and ONSR dB exterapolated from calcosnrlin = {}"
            + " including non linear contributions",
            calcCd, Math.sqrt(calcPmd2), Math.sqrt(calcPdl2), calcOsnrdB, getOsnrDbfromOnsrLin(calcOnsrLin));
        if (!transponderPresent) {
            LOG.info("No transponder in the path, User shall check from CD, PMD, and OSNR values provided "
                + "that optical tunnel degradations are compatible with external transponder performances");
            return 0.0;
        }
        double delta = margin - SYS_MARGIN;
        LOG.info("In checkOSNR: Transponder Operational mode {} results in a residual margin of {} dB, according "
            + "to CD, PMD and DGD induced penalties and set System Margin of {} dB.",
            opMode, delta, SYS_MARGIN);
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
    @SuppressWarnings("deprecation")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DLS_DEAD_LOCAL_STORE")
    private double checkOSNRza(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, String serviceType, CatalogUtils cu) {
        double spacing = 50.0;
        double calcPdl2 = 0;
        double calcOsnrdB = 0;
        double calcCd = 0;
        double calcPmd2 = 0;
        double calcOnsrLin = 0.0001;
        double margin = 0;
        double pwrIn = -60.0;
        double pwrOut = -60.0;
        boolean transponderPresent = false;
        List<String> vertices = path.getVertexList();
        List<PceGraphEdge> edges = path.getEdgeList();
        String opMode = "";
        // LOOP that scans the different Nodes/Links of the path and calculates
        // associated degradations
        // using CatalogUtils primitives to retrieve physical parameters and make a
        // first level calculation
        int bypassDegree = 0;
        for (int n = 0; n < 2; n++) {
            int pathElement = vertices.size() - n - 1;
            bypassDegree = 0;
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement - 1)));
            LOG.debug("loop of check OSNR, n = {} Path Element = {}", n, pathElement);
            switch (currentNode.getORNodeType()) {
                case XPONDER:
                    transponderPresent = true;
                    String nwTpId =
                        pathElement == vertices.size() - 1
                            ? getOppPceLink(pathElement - 1, edges, allPceLinks).getSourceTP().getValue()
                        // last Xponder of the path (RX side)
                            : getOppPceLink((pathElement), edges, allPceLinks).getDestTP().getValue();
                    LOG.debug("loop of check OSNR : XPDR, n = {} Path Element = {}", n, pathElement);
                    opMode =
                        getXpdrOpMode(nwTpId, vertices.get(pathElement), pathElement, currentNode, serviceType, cu);
                    // TSP is first element of the path . To correctly evaluate the TX OOB OSNR from
                    // its operational mode, we need to know the type of ADD/DROP Mux it is
                    // connected to
                    // If the operational mode of the ADD/DROP MUX is not consistent or
                    // if the operational mode of the ADD/DROP MUX is not declared in the topology
                    // (Network TP)
                        // Operational mode is set by default to standard opMode for ADD SRGs
                    String adnMode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                        // Operational mode is found in SRG attributes of the Node
                    LOG.debug("Transponder {} corresponding to path Element {} in the path is connected to SRG "
                        + "which has {} operational mode", currentNode.getNodeId().getValue(), pathElement,
                        adnMode);
                    // Retrieve the Tx ONSR of the Xponder which results from IB and OOB OSNR
                    // contributions
                    calcOnsrLin = cu.getPceTxTspParameters(opMode, adnMode);
                    // Retrieve the spacing associated with Xponder operational mode that is needed
                    // to calculate OSNR
                    spacing = cu.getPceTxTspChannelSpacing(opMode);
                    LOG.info("Transponder {} corresponding to path Element {} in the path has a TX OSNR of {} dB",
                        currentNode.getNodeId().getValue(), pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                case SRG:
                    LOG.debug("loop of check OSNR : SRG, n = {} Path Element = {}", n, pathElement);
                    // This is ADD case : First (optical-tunnel) or 2nd (Regular E2E service from
                    // Xponder to Xponder) node element of the path is the ADD SRG.
                    if (getOppPceLink(pathElement - 1, edges, allPceLinks).getlinkType()
                            != OpenroadmLinkType.ADDLINK) {
                        LOG.error("Error processing Node {} for which output link {} is not an ADDLINK Type",
                            currentNode.getNodeId().toString(), pathElement - 1);
                    }
                    pwrIn = 0.0;
                    PceLink pceLink = getOppPceLink(pathElement - 2, edges, allPceLinks);
// If the operational mode of the ADD/DROP MUX is not consistent or not declared in the topology (Network TP)
// Operational mode is set by default to standard opMode for ADD/DROP SRGs
                    String srgMode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                    CatalogNodeType cnt = CatalogConstant.CatalogNodeType.ADD;
                    pwrOut = cu.getPceRoadmAmpOutputPower(
                        cnt, srgMode, pceLink.getspanLoss(), spacing, pceLink.getpowerCorrection());
                    LOG.debug("loop of check OSNR : SRG, n = {} link {} Pout = {}",
                        pathElement, pathElement - 2, pwrOut);
                    //calculation of the SRG contribution either for Add and Drop
                    Map<String, Double> impairments = cu.getPceRoadmAmpParameters(cnt, srgMode,
                        pwrIn, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments.get("CD").doubleValue();
                    calcPmd2 = impairments.get("DGD2").doubleValue();
                    calcPdl2 = impairments.get("PDL2").doubleValue();
                    calcOnsrLin = impairments.get("ONSRLIN").doubleValue();
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    impairments.clear();
                    // For the ADD, degradation brought by the node are calculated from the MW-WR spec.
                    // The Degree is not considered. This means we must bypass the add-link (ADD)
                    // and the next node (Degree) which are not considered in the impairments.
                    n++;
                    bypassDegree = 1;
                    break;
                case DEGREE:
                    if (nextNode.getORNodeType() != OpenroadmNodeType.DEGREE) {
                        //This is the case of DROP, ROADM degree is not considered
                        break;
                    }
                    LOG.info("loop of check OSNR : DEGREE, n = {} Path Element = {}", n, pathElement);
                    Map<String, Double> impairments0 =  degreeCheckOSNR(
                        cu, currentNode, nextNode,
                        getOppPceLink(pathElement, edges, allPceLinks),
                        getOppPceLink(pathElement - 2, edges, allPceLinks),
                        pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments0.get("CD").doubleValue();
                    calcPmd2 = impairments0.get("DGD2").doubleValue();
                    calcPdl2 = impairments0.get("PDL2").doubleValue();
                    calcOnsrLin = impairments0.get("ONSRLIN").doubleValue();
                    //TODO rename impariments0 var and/or adapt catalog utils
                    pwrIn = impairments0.get("pwrIn").doubleValue();
                    pwrOut = impairments0.get("pwrOut").doubleValue();
                    LOG.debug("Loop n = {}, DEGREE, calcOsnrdB= {}", n, getOsnrDbfromOnsrLin(calcOnsrLin));
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // increment pathElement so that in next step we will not point to Degree2 but
                    // next node
                    n++;
                    bypassDegree = 1;
                    LOG.info("Accumulated degradations in the path including ROADM {} + {} are CD: {}; PMD2: "
                        + "{}; Pdl2 : {}; ONSRdB : {}", currentNode.getNodeId().toString(),
                        nextNode.getNodeId().toString(), calcCd, calcPmd2, calcPdl2, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        for (int n = 2 + bypassDegree; n < vertices.size() - 1; n++) {
            int pathElement = vertices.size() - n - 1;
            PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodes.get(new NodeId(vertices.get(pathElement - 1)));
            LOG.debug("loop of check OSNR, n = {} Path Element = {}", n, pathElement);
            switch (currentNode.getORNodeType()) {
                case XPONDER:
                    transponderPresent = true;
                    String nwTpId = getOppPceLink((pathElement), edges, allPceLinks).getDestTP().getValue();
                    LOG.debug("loop of check OSNR : XPDR, n = {} Path Element = {}", n, pathElement);
                    opMode =
                        getXpdrOpMode(nwTpId, vertices.get(pathElement), pathElement, currentNode, serviceType, cu);
                    // TSP is first element of the path . To correctly evaluate the TX OOB OSNR from
                    // its operational mode, we need to know the type of ADD/DROP Mux it is
                    // connected to
                    // If the operational mode of the ADD/DROP MUX is not consistent or
                    // if the operational mode of the ADD/DROP MUX is not declared in the topology
                    // (Network TP)
                        // Operational mode is set by default to standard opMode for ADD SRGs
                    String adnMode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                        // Operational mode is found in SRG attributes of the Node
                    LOG.debug("Transponder {} corresponding to path Element {} in the path is connected to SRG "
                        + "which has {} operational mode", currentNode.getNodeId().getValue(), pathElement,
                        adnMode);
                    // Retrieve the Tx ONSR of the Xponder which results from IB and OOB OSNR
                    // contributions
                    calcOnsrLin = cu.getPceTxTspParameters(opMode, adnMode);
                    // Retrieve the spacing associated with Xponder operational mode that is needed
                    // to calculate OSNR
                    spacing = cu.getPceTxTspChannelSpacing(opMode);
                    LOG.info("Transponder {} corresponding to path Element {} in the path has a TX OSNR of {} dB",
                        currentNode.getNodeId().getValue(), pathElement, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                case SRG:
                    LOG.debug("loop of check OSNR : SRG, n = {} Path Element = {}", n, pathElement);
                    if (getOppPceLink(pathElement, edges, allPceLinks).getlinkType()
                            != OpenroadmLinkType.DROPLINK) {
                        LOG.error("Error processing Node {} for which input link {} is not a DROPLINK Type",
                            currentNode.getNodeId().toString(), pathElement);
                    }
                    PceLink pceLink = getOppPceLink(pathElement + 1, edges, allPceLinks);
                    LOG.info("loop of check OSNR : SRG, n = {} CD on preceeding link {} = {} ps",
                        pathElement, pathElement + 1, pceLink.getcd());
                    Map<String, Double> impairments = calcDegradationOSNR(
                        cu, pceLink, pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments.get("calcCd");
                    calcPmd2 = impairments.get("calcPmd2");
                    calcOnsrLin = impairments.get("calcOnsrLin");
                    pwrIn = impairments.get("pwrIn");
// If the operational mode of the ADD/DROP MUX is not consistent or not declared in the topology (Network TP)
// Operational mode is set by default to standard opMode for ADD/DROP SRGs
                    String srgMode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWWRCORE);
                    CatalogNodeType cnt = CatalogConstant.CatalogNodeType.DROP;
                    //calculation of the SRG contribution either for Add and Drop
                    impairments = cu.getPceRoadmAmpParameters(cnt, srgMode,
                        pwrIn, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments.get("CD").doubleValue();
                    calcPmd2 = impairments.get("DGD2").doubleValue();
                    calcPdl2 = impairments.get("PDL2").doubleValue();
                    calcOnsrLin = impairments.get("ONSRLIN").doubleValue();
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }

                    // If SRG is not the first or the second element of the Path, it is the DROP
                    // side.
                    // After accumulated degradations are calculated, we also need to calculate
                    // resulting OSNR in dB to pass it to the method that verifies end Xponder
                    // performances are compatible with degradations experienced on the path
                    try {
                        calcOsnrdB = getOsnrDbfromOnsrLin(calcOnsrLin);
                        LOG.info("checkOSNR loop, last SRG osnr is {} dB", calcOsnrdB);
                        LOG.info("Loop n = {}, DROP, calcOsnrdB= {}", n, calcOsnrdB);
                    } catch (ArithmeticException e) {
                        LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}",
                            path.getEdgeList().size());
                        return -1.0;
                    }
                    impairments.clear();
                    break;
                case DEGREE:
                    if (nextNode.getORNodeType() != OpenroadmNodeType.DEGREE) {
                        //This is the case of DROP, ROADM degree is not considered
                        break;
                    }
                    LOG.info("loop of check OSNR : DEGREE, n = {} Path Element = {}", n, pathElement);
                    Map<String, Double> impairments0 =  degreeCheckOSNR(
                        cu, currentNode, nextNode,
                        getOppPceLink(pathElement, edges, allPceLinks),
                        getOppPceLink(pathElement - 2, edges, allPceLinks),
                        pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
                    calcCd = impairments0.get("CD").doubleValue();
                    calcPmd2 = impairments0.get("DGD2").doubleValue();
                    calcPdl2 = impairments0.get("PDL2").doubleValue();
                    calcOnsrLin = impairments0.get("ONSRLIN").doubleValue();
                    //TODO rename impariments0 var and/or adapt catalog utils
                    pwrIn = impairments0.get("pwrIn").doubleValue();
                    pwrOut = impairments0.get("pwrOut").doubleValue();
                    LOG.debug("Loop n = {}, DEGREE, calcOsnrdB= {}", n, getOsnrDbfromOnsrLin(calcOnsrLin));
                    if (calcOnsrLin == Double.NEGATIVE_INFINITY || calcOnsrLin == Double.POSITIVE_INFINITY) {
                        return -1.0;
                    }
                    // increment pathElement so that in next step we will not point to Degree2 but
                    // next node
                    n++;
                    LOG.info("Accumulated degradations in the path including ROADM {} + {} are CD: {}; PMD2: "
                        + "{}; Pdl2 : {}; ONSRdB : {}", currentNode.getNodeId().toString(),
                        nextNode.getNodeId().toString(), calcCd, calcPmd2, calcPdl2, getOsnrDbfromOnsrLin(calcOnsrLin));
                    break;
                default:
                    LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain");
            }
        }
        PceNode currentNode = allPceNodes.get(new NodeId(vertices.get(0)));
        LOG.debug("loop of check OSNR, n = {} Path Element = {}", vertices.size() - 1, 0);
        switch (currentNode.getORNodeType()) {
            case XPONDER:
                transponderPresent = true;
                LOG.debug("loop of check OSNR : XPDR, n = {} Path Element = {}", vertices.size() - 1, 0);
                String nwTpId = getOppPceLink(0, edges, allPceLinks).getDestTP().getValue();
                opMode = getXpdrOpMode(nwTpId, vertices.get(0), 0, currentNode, serviceType, cu);
                // If TSP is the last of the path
                LOG.debug(
                    "Loop n = {}, Step5.1, XPDR, tries calculating Margin, just before call", vertices.size() - 1);
                // Check that accumulated degradations are compatible with TSP performances
                // According to OpenROADM spec :
                // margin = cu.getPceRxTspParameters(opMode, calcCd, Math.sqrt(calcPmd2), Math.sqrt(calcPdl2),
                //              getOsnrDbfromOnsrLin(calcOnsrLin));
                // Calculation modified for pdl according to calculation in Julia's Tool
                margin = cu.getPceRxTspParameters(opMode, calcCd, Math.sqrt(calcPmd2),
                    Math.sqrt(calcPdl2), getOsnrDbfromOnsrLin(calcOnsrLin));
                LOG.info("Loop n = {}, XPDR, calcosnrdB= {}", vertices.size() - 1, getOsnrDbfromOnsrLin(calcOnsrLin));
                break;
            case SRG:
            case DEGREE:
            default:
                LOG.error("PostAlgoPathValidator.CheckOSNR : unsupported resource type in the path chain last element");
        }
        LOG.info("- In checkOSNR: accumulated CD = {} ps, PMD = {} ps, PDL = {} dB, and resulting OSNR calcOsnrdB = {} "
            + "dB and ONSR dB exterapolated from calcosnrlin = {}"
            + " including non linear contributions",
            calcCd, Math.sqrt(calcPmd2), Math.sqrt(calcPdl2), calcOsnrdB, getOsnrDbfromOnsrLin(calcOnsrLin));
        if (!transponderPresent) {
            LOG.info("No transponder in the path, User shall check from CD, PMD, and OSNR values provided "
                + "that optical tunnel degradations are compatible with external transponder performances");
            return 0.0;
        }
        double delta = margin - SYS_MARGIN;
        LOG.info("In checkOSNR: Transponder Operational mode {} results in a residual margin of {} dB, according "
            + "to CD, PMD and DGD induced penalties and set System Margin of {} dB.",
            opMode, delta, SYS_MARGIN);
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
        InstanceIdentifier<TerminationPoint1> nwTpIid =
            InstanceIdentifiers.createNetworkTerminationPoint1IIDBuilder(vertice, nwTpId);
        String opMode = cu.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP, serviceType);
        try {
            if (networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, nwTpIid).get().isPresent()) {
                // If the operational mode of the Xponder is not consistent or
                // if the operational mode of the Xponder is not declared in the topology
                // (Network TP)
                opMode = setOpMode(
                    currentNode.getXponderOperationalMode(
                        networkTransactionService
                                .read(LogicalDatastoreType.CONFIGURATION, nwTpIid)
                                .get().get().getXpdrNetworkAttributes()),
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


    private Map<String, Double> degreeCheckOSNR(
            CatalogUtils cu, PceNode currentNode, PceNode nextNode, PceLink pceLink0, PceLink pceLink1,
            double pwrOut, double calcCd, double calcPmd2, double calcPdl2, double calcOnsrLin, double spacing) {
        // If the operational mode of the Degree is not consistent or declared in the topology
        // Operational mode is set by default to standard opMode for Degree
        String degree1Mode = setOpMode(currentNode.getOperationalMode(), CatalogConstant.MWMWCORE);
        // Same for next node which is the second degree of a ROADM node
        String degree2Mode = setOpMode(nextNode.getOperationalMode(), CatalogConstant.MWMWCORE);
        // At that time OpenROADM provides only one spec for the ROADM nodes
        if (!degree1Mode.equals(degree2Mode)) {
            LOG.warn("Unsupported Hybrid ROADM configuration with Degree1 {} of {} operational mode and Degree2 "
                + "{} of {} operational mode. Will by default use operational mode of Degree2",
                currentNode.getNodeId().toString(), degree1Mode, nextNode.getNodeId().toString(), degree2Mode);
        }
        Map<String, Double> impairments = calcDegradationOSNR(
            cu, pceLink0, pwrOut, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
        calcCd = impairments.get("calcCd");
        calcPmd2 = impairments.get("calcPmd2");
        calcOnsrLin = impairments.get("calcOnsrLin");
        CatalogNodeType cnt0 = CatalogConstant.CatalogNodeType.EXPRESS;
        double pwrIn = impairments.get("pwrIn");
        pwrOut = cu.getPceRoadmAmpOutputPower(
            cnt0, degree2Mode, pceLink1.getspanLoss(), spacing, pceLink1.getpowerCorrection());
        // Adds to accumulated impairments the degradation associated with the Express
        // path of ROADM : Degree1, express link, Degree2
        impairments = cu.getPceRoadmAmpParameters(
            cnt0, degree2Mode, pwrIn, calcCd, calcPmd2, calcPdl2, calcOnsrLin, spacing);
        impairments.put("pwrIn", pwrIn);
        impairments.put("pwrOut", pwrOut);
        return impairments;
    }
    //TODO these methods might be more indicated in a catalog utils refactoring

    private Map<String, Double> calcDegradationOSNR(
            CatalogUtils cu, PceLink pceLink,
            double pwrOut, double calcCd, double calcPmd2, double calcPdl2, double calcOnsrLin, double spacing) {
        // Calculate degradation accumulated across incoming Link and add them to
        // accumulated impairments
        // This also includes Non Linear Contribution from the path
        return Map.of(
            "pwrIn", pwrOut - pceLink.getspanLoss(),
            "calcCd", calcCd + pceLink.getcd(),
            "calcPmd2", calcPmd2 + pceLink.getpmd2(),
            "calcOnsrLin", calcOnsrLin + cu.calculateNLonsrContribution(pwrOut, pceLink.getLength(), spacing));
    }

    private double getOsnrDbfromOnsrLin(double onsrLu) {
        return 10 * Math.log10(1 / onsrLu);
    }

    /**
     * Get spectrum assignment for path.
     *
     * @param path                    the path for which we get spectrum assignment.
     * @param allPceNodes             all optical nodes.
     * @param spectralWidthSlotNumber number of slot for spectral width. Depends on
     *                                service type.
     * @return a spectrum assignment object which contains begin and end index. If
     *         no spectrum assignment found, beginIndex = stopIndex = 0
     */
    private SpectrumAssignment getSpectrumAssignment(GraphPath<String, PceGraphEdge> path,
            Map<NodeId, PceNode> allPceNodes, int spectralWidthSlotNumber) {
        byte[] freqMap = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(freqMap, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        BitSet result = BitSet.valueOf(freqMap);
        boolean isFlexGrid = true;
        LOG.debug("Processing path {} with length {}", path, path.getLength());
        for (PceGraphEdge edge : path.getEdgeList()) {
            NodeId srcNodeId = edge.link().getSourceId();
            LOG.debug("Processing source {} ", srcNodeId);
            if (allPceNodes.containsKey(srcNodeId)) {
                PceNode pceNode = allPceNodes.get(srcNodeId);
                LOG.debug("Processing PCE node {}", pceNode);
                String pceNodeVersion = pceNode.getVersion();
                NodeId pceNodeId = pceNode.getNodeId();
                BigDecimal sltWdthGran = pceNode.getSlotWidthGranularity();
                BigDecimal ctralFreqGran = pceNode.getCentralFreqGranularity();
                if (StringConstants.OPENROADM_DEVICE_VERSION_1_2_1.equals(pceNodeVersion)) {
                    LOG.debug("Node {}: version is {} and slot width granularity is {} -> fixed grid mode",
                        pceNodeId, pceNodeVersion, sltWdthGran);
                    isFlexGrid = false;
                }
                if (sltWdthGran.setScale(0, RoundingMode.CEILING).equals(GridConstant.SLOT_WIDTH_50)
                        && ctralFreqGran.setScale(0, RoundingMode.CEILING).equals(GridConstant.SLOT_WIDTH_50)) {
                    LOG.debug("Node {}: version is {} with slot width granularity {} and central "
                            + "frequency granularity is {} -> fixed grid mode",
                        pceNodeId, pceNodeVersion, sltWdthGran, ctralFreqGran);
                    isFlexGrid = false;
                }
                BitSet pceNodeFreqMap = pceNode.getBitSetData();
                LOG.debug("Pce node bitset {}", pceNodeFreqMap);
                if (pceNodeFreqMap != null) {
                    result.and(pceNodeFreqMap);
                    LOG.debug("intermediate bitset {}", result);
                }
            }
        }
        LOG.debug("Bitset result {}", result);
        return computeBestSpectrumAssignment(result, spectralWidthSlotNumber, isFlexGrid);
    }

    /**
     * Compute spectrum assignment from spectrum occupation for spectral width.
     *
     * @param spectrumOccupation      the spectrum occupation BitSet.
     * @param spectralWidthSlotNumber the nb slots for spectral width.
     * @param isFlexGrid              true if flexible grid, false otherwise.
     * @return a spectrum assignment object which contains begin and stop index. If
     *         no spectrum assignment found, beginIndex = stopIndex = 0
     */
    private SpectrumAssignment computeBestSpectrumAssignment(
            BitSet spectrumOccupation, int spectralWidthSlotNumber, boolean isFlexGrid) {
        SpectrumAssignmentBuilder spectrumAssignmentBldr = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(0))
            .setStopIndex(Uint16.valueOf(0))
            .setFlexGrid(isFlexGrid);
        BitSet referenceBitSet = new BitSet(spectralWidthSlotNumber);
        referenceBitSet.set(0, spectralWidthSlotNumber);
        //higher is the frequency, smallest is the wavelength number
        //in operational, the allocation is done through wavelength starting from the smallest
        //so we have to loop from the last element of the spectrum occupation
        for (int i = spectrumOccupation.size(); i >= spectralWidthSlotNumber;
                i -= isFlexGrid ? spectralWidthSlotNumber : 1) {
            if (spectrumOccupation.get(i - spectralWidthSlotNumber, i).equals(referenceBitSet)) {
                spectrumAssignmentBldr.setBeginIndex(Uint16.valueOf(i - spectralWidthSlotNumber));
                spectrumAssignmentBldr.setStopIndex(Uint16.valueOf(i - 1));
                break;
            }
        }
        return spectrumAssignmentBldr.build();
    }

    public Double getTpceCalculatedMargin() {
        return tpceCalculatedMargin;
    }
}
