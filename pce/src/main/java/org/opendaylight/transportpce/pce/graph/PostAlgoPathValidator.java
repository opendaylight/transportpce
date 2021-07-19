/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.SpectrumAssignmentBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAlgoPathValidator {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PostAlgoPathValidator.class);

    private static final double MIN_OSNR_W100G = 17;
    private static final double TRX_OSNR = 33;
    private static final double ADD_OSNR = 30;
    public static final Long CONST_OSNR = 1L;
    public static final double SYS_MARGIN = 0;

    @SuppressFBWarnings(
        value = "SF_SWITCH_FALLTHROUGH",
        justification = "intentional fallthrough")
    public PceResult checkPath(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes,
        PceResult pceResult, PceConstraints pceHardConstraints, String serviceType) {

        // check if the path is empty
        if (path.getEdgeList().isEmpty()) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            return pceResult;
        }
        int spectralWidthSlotNumber = GridConstant.SPECTRAL_WIDTH_SLOT_NUMBER_MAP
            .getOrDefault(serviceType, GridConstant.NB_SLOTS_100G);
        SpectrumAssignment spectrumAssignment = null;
        //variable to deal with 1GE (Nb=1) and 10GE (Nb=10) cases
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_400GE:
                spectralWidthSlotNumber = GridConstant.SPECTRAL_WIDTH_SLOT_NUMBER_MAP
                    .getOrDefault(serviceType, GridConstant.NB_SLOTS_400G);
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
                    LOG.info("Spectrum assignment flexgrid mode");
                    pceResult.setResultWavelength(GridConstant.IRRELEVANT_WAVELENGTH_NUMBER);
                } else {
                    LOG.info("Spectrum assignment fixedgrid mode");
                    pceResult.setResultWavelength(
                            GridUtils.getWaveLengthIndexFromSpectrumAssigment(spectrumAssignment.getBeginIndex()
                                .toJava()));
                }
                pceResult.setMinFreq(GridUtils.getStartFrequencyFromIndex(spectrumAssignment.getBeginIndex().toJava()));
                pceResult.setMaxFreq(GridUtils.getStopFrequencyFromIndex(spectrumAssignment.getStopIndex().toJava()));
                LOG.info("In PostAlgoPathValidator: spectrum assignment found {} {}", spectrumAssignment, path);

                // Check the OSNR
                if (!checkOSNR(path)) {
                    pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                    pceResult.setLocalCause(PceResult.LocalCause.OUT_OF_SPEC_OSNR);
                    return pceResult;
                }

                // Check if MaxLatency is defined in the hard constraints
                if ((pceHardConstraints.getMaxLatency() != -1)
                        && (!checkLatency(pceHardConstraints.getMaxLatency(), path))) {
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
                break;
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                Map<String, Integer> tribSlotNbMap = Map.of(
                    StringConstants.SERVICE_TYPE_100GE_M, 20,
                    StringConstants.SERVICE_TYPE_10GE, 8,
                    StringConstants.SERVICE_TYPE_1GE, 1);
                int tribSlotNb = tribSlotNbMap.get(serviceType);
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
                break;
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC4:
                pceResult.setRC(ResponseCodes.RESPONSE_OK);
                LOG.info("In PostAlgoPathValidator: ODU4/ODUC4 path found {}", path);
                break;
            default:
                pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                LOG.warn("In PostAlgoPathValidator checkPath: unsupported serviceType {} found {}",
                    serviceType, path);
                break;
        }
        return pceResult;
    }

    // Check the latency
    private boolean checkLatency(Long maxLatency, GraphPath<String, PceGraphEdge> path) {
        double latency = 0;

        for (PceGraphEdge edge : path.getEdgeList()) {
            try {
                latency += edge.link().getLatency();
                LOG.debug("- In checkLatency: latency of {} = {} units", edge.link().getLinkId().getValue(), latency);
            } catch (NullPointerException e) {
                LOG.warn("- In checkLatency: the link {} does not contain latency field",
                    edge.link().getLinkId().getValue());
            }
        }
        return (latency < maxLatency);
    }

    // Check the inclusion if it is defined in the hard constraints
    private boolean checkInclude(GraphPath<String, PceGraphEdge> path, PceConstraints pceHardConstraintsInput) {
        List<ResourcePair> listToInclude = pceHardConstraintsInput.getListToInclude();
        if (listToInclude.isEmpty()) {
            return true;
        }

        List<PceGraphEdge> pathEdges = path.getEdgeList();
        LOG.debug(" in checkInclude vertex list: [{}]", path.getVertexList());

        List<String> listOfElementsSubNode = new ArrayList<>();
        listOfElementsSubNode.add(pathEdges.get(0).link().getsourceNetworkSupNodeId());
        listOfElementsSubNode.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.NODE,
            pceHardConstraintsInput));

        List<String> listOfElementsCLLI = new ArrayList<>();
        listOfElementsCLLI.add(pathEdges.get(0).link().getsourceCLLI());
        listOfElementsCLLI.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.CLLI,
            pceHardConstraintsInput));

        List<String> listOfElementsSRLG = new ArrayList<>();
        // first link is XPONDEROUTPUT, no SRLG for it
        listOfElementsSRLG.add("NONE");
        listOfElementsSRLG.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.SRLG,
            pceHardConstraintsInput));

        // validation: check each type for each element
        for (ResourcePair next : listToInclude) {
            int indx = -1;
            switch (next.getType()) {
                case NODE:
                    if (listOfElementsSubNode.contains(next.getName())) {
                        indx = listOfElementsSubNode.indexOf(next.getName());
                    }
                    break;
                case SRLG:
                    if (listOfElementsSRLG.contains(next.getName())) {
                        indx = listOfElementsSRLG.indexOf(next.getName());
                    }
                    break;
                case CLLI:
                    if (listOfElementsCLLI.contains(next.getName())) {
                        indx = listOfElementsCLLI.indexOf(next.getName());
                    }
                    break;
                default:
                    LOG.warn(" in checkInclude vertex list unsupported resource type: [{}]", next.getType());
            }

            if (indx < 0) {
                LOG.debug(" in checkInclude stopped : {} ", next.getName());
                return false;
            }

            LOG.debug(" in checkInclude next found {} in {}", next.getName(), path.getVertexList());

            listOfElementsSubNode.subList(0, indx).clear();
            listOfElementsCLLI.subList(0, indx).clear();
            listOfElementsSRLG.subList(0, indx).clear();
        }

        LOG.info(" in checkInclude passed : {} ", path.getVertexList());
        return true;
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
        LOG.info("In choosetribPort: edgeList = {} ", path.getEdgeList());
        Map<String, Uint16> tribPortMap = new HashMap<>();

        for (PceGraphEdge edge : path.getEdgeList()) {
            NodeId linkSrcNode = edge.link().getSourceId();
            String linkSrcTp = edge.link().getSourceTP().getValue();
            NodeId linkDestNode = edge.link().getDestId();
            String linkDestTp = edge.link().getDestTP().getValue();
            PceNode pceOtnNodeSrc = allPceNodes.get(linkSrcNode);
            PceNode pceOtnNodeDest = allPceNodes.get(linkDestNode);
            List<Uint16> srcTpnPool = pceOtnNodeSrc.getAvailableTribPorts().get(linkSrcTp);
            List<Uint16> destTpnPool = pceOtnNodeDest.getAvailableTribPorts().get(linkDestTp);
            List<Uint16> commonEdgeTpnPool = new ArrayList<>();
            for (Uint16 srcTpn : srcTpnPool) {
                if (destTpnPool.contains(srcTpn)) {
                    commonEdgeTpnPool.add(srcTpn);
                }
            }
            Collections.sort(commonEdgeTpnPool);
            if (!commonEdgeTpnPool.isEmpty()) {
                Integer startTribSlot = tribSlotMap.values().stream().findFirst().get().get(0).toJava();
                Integer tribPort = (int) Math.ceil((double)startTribSlot / nbSlot);
                for (Uint16 commonTribPort : commonEdgeTpnPool) {
                    if (tribPort.equals(commonTribPort.toJava())) {
                        tribPortMap.put(edge.link().getLinkId().getValue(), commonTribPort);
                    }
                }
            }
        }
        tribPortMap.forEach((k,v) -> LOG.info("TribPortMap : k = {}, v = {}", k, v));
        return tribPortMap;
    }

    private Map<String, List<Uint16>> chooseTribSlot(GraphPath<String,
        PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes, int nbSlot) {
        LOG.info("In choosetribSlot: edgeList = {} ", path.getEdgeList());
        Map<String, List<Uint16>> tribSlotMap = new HashMap<>();

        for (PceGraphEdge edge : path.getEdgeList()) {
            NodeId linkSrcNode = edge.link().getSourceId();
            String linkSrcTp = edge.link().getSourceTP().getValue();
            NodeId linkDestNode = edge.link().getDestId();
            String linkDestTp = edge.link().getDestTP().getValue();
            PceNode pceOtnNodeSrc = allPceNodes.get(linkSrcNode);
            PceNode pceOtnNodeDest = allPceNodes.get(linkDestNode);
            List<Uint16> srcTsPool = pceOtnNodeSrc.getAvailableTribSlots().get(linkSrcTp);
            List<Uint16> destTsPool = pceOtnNodeDest.getAvailableTribSlots().get(linkDestTp);
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
                if (Integer.valueOf(1).equals(startEdgeTsPool.toJava() % nbSlot)
                        || nbSlot == 1) {
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
        @SuppressWarnings("unchecked")
        List<Uint16> tsList = (List<Uint16>) tribSlot.values().toArray()[0];
        OpucnTribSlotDef minOpucnTs = OpucnTribSlotDef
            .getDefaultInstance(String.join(".", tribport, tsList.get(0).toString()));
        OpucnTribSlotDef maxOpucnTs = OpucnTribSlotDef
            .getDefaultInstance(String.join(".", tribport, tsList.get(tsList.size() - 1).toString()));
        List<OpucnTribSlotDef> minmaxTpTsList = new ArrayList<>();
        minmaxTpTsList.add(minOpucnTs);
        minmaxTpTsList.add(maxOpucnTs);
        return minmaxTpTsList;
    }

    // Check the path OSNR
    private boolean checkOSNR(GraphPath<String, PceGraphEdge> path) {
        double linkOsnrDb;
        double osnrDb = 0;
        LOG.info("- In checkOSNR: OSNR of the transmitter = {} dB", TRX_OSNR);
        LOG.info("- In checkOSNR: add-path incremental OSNR = {} dB", ADD_OSNR);
        double inverseLocalOsnr = getInverseOsnrLinkLu(TRX_OSNR) + getInverseOsnrLinkLu(ADD_OSNR);
        for (PceGraphEdge edge : path.getEdgeList()) {
            if (edge.link().getlinkType() == OpenroadmLinkType.ROADMTOROADM) {
                // link OSNR in dB
                linkOsnrDb = edge.link().getosnr();
                LOG.info("- In checkOSNR: OSNR of {} = {} dB", edge.link().getLinkId().getValue(), linkOsnrDb);
                // 1 over the local OSNR, in linear units
                inverseLocalOsnr += getInverseOsnrLinkLu(linkOsnrDb);
            }
        }
        try {
            osnrDb = getOsnrDb(1 / inverseLocalOsnr);
        } catch (ArithmeticException e) {
            LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}", path.getEdgeList().size());
            return false;
        }
        LOG.info("In checkOSNR: OSNR of the path is {} dB", osnrDb);
        return ((osnrDb + SYS_MARGIN) > MIN_OSNR_W100G);
    }

    private double getOsnrDb(double osnrLu) {
        return (10 * Math.log10(osnrLu));
    }

    private double getInverseOsnrLinkLu(double linkOsnrDb) {
        // 1 over the link OSNR, in linear units
        double linkOsnrLu = Math.pow(10, (linkOsnrDb / 10.0));
        LOG.debug("In retrieveosnr: the inverse of link osnr is {} (Linear Unit)", linkOsnrLu);
        return (CONST_OSNR / linkOsnrLu);
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
        LOG.info("Processing path {} with length {}", path, path.getLength());
        BitSet pceNodeFreqMap;
        for (PceGraphEdge edge : path.getEdgeList()) {
            LOG.info("Processing source {} ", edge.link().getSourceId());
            if (allPceNodes.containsKey(edge.link().getSourceId())) {
                PceNode pceNode = allPceNodes.get(edge.link().getSourceId());
                LOG.info("Processing PCE node {}", pceNode);
                if (StringConstants.OPENROADM_DEVICE_VERSION_1_2_1.equals(pceNode.getVersion())) {
                    LOG.info("Node {}: version is {} and slot width granularity is {} -> fixed grid mode",
                        pceNode.getNodeId(), pceNode.getVersion(), pceNode.getSlotWidthGranularity());
                    isFlexGrid = false;
                }
                if ((pceNode.getSlotWidthGranularity().equals(GridConstant.SLOT_WIDTH_50))
                    && (pceNode.getCentralFreqGranularity().equals(GridConstant.SLOT_WIDTH_50))) {
                    LOG.info("Node {}: version is {} with slot width granularity  {} and central "
                            + "frequency granularity is {} -> fixed grid mode",
                        pceNode.getNodeId(), pceNode.getVersion(), pceNode.getSlotWidthGranularity(),
                        pceNode.getCentralFreqGranularity());
                    isFlexGrid = false;
                }
                pceNodeFreqMap = pceNode.getBitSetData();
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
    private SpectrumAssignment computeBestSpectrumAssignment(BitSet spectrumOccupation, int spectralWidthSlotNumber,
            boolean isFlexGrid) {
        SpectrumAssignmentBuilder spectrumAssignmentBldr = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(0))
            .setStopIndex(Uint16.valueOf(0))
            .setFlexGrid(isFlexGrid);
        BitSet referenceBitSet = new BitSet(spectralWidthSlotNumber);
        referenceBitSet.set(0, spectralWidthSlotNumber);
        int nbSteps = isFlexGrid ? spectralWidthSlotNumber : 1;
        //higher is the frequency, smallest is the wavelength number
        //in operational, the allocation is done through wavelength starting from the smallest
        //so we have to loop from the last element of the spectrum occupation
        for (int i = spectrumOccupation.size(); i >= spectralWidthSlotNumber; i -= nbSteps) {
            if (spectrumOccupation.get(i - spectralWidthSlotNumber, i).equals(referenceBitSet)) {
                spectrumAssignmentBldr.setBeginIndex(Uint16.valueOf(i - spectralWidthSlotNumber));
                spectrumAssignmentBldr.setStopIndex(Uint16.valueOf(i - 1));
                break;
            }
        }
        return spectrumAssignmentBldr.build();
    }
}
