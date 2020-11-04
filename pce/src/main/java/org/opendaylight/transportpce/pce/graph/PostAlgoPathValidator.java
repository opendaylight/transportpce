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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAlgoPathValidator {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PostAlgoPathValidator.class);

    private static final int MAX_WAWELENGTH = 96;
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

        int tribSlotNb = 1;
        //variable to deal with 1GE (Nb=1) and 10GE (Nb=10) cases
        switch (serviceType) {

            case "100GE":
            case "OTU4":
                // choose wavelength available in all nodes of the path
                Long waveL = chooseWavelength(path, allPceNodes);
                pceResult.setServiceType(serviceType);
                if (waveL < 0) {
                    pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                    pceResult.setLocalCause(PceResult.LocalCause.NO_PATH_EXISTS);
                    return pceResult;
                }
                pceResult.setResultWavelength(waveL);
                LOG.info("In PostAlgoPathValidator: chooseWavelength WL found {} {}", waveL, path);

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

            case "10GE":
                tribSlotNb = 8;
            //fallthrough
            case "1GE":
                pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                pceResult.setServiceType(serviceType);
                Map<String, Uint16> tribPort = chooseTribPort(path, allPceNodes);
                Map<String, List<Uint16>> tribSlot = chooseTribSlot(path, allPceNodes, tribSlotNb);

                if (tribPort != null && tribSlot != null) {
                    pceResult.setResultTribPort(tribPort);
                    pceResult.setResultTribSlot(tribSlot);
                    pceResult.setResultTribSlotNb(tribSlotNb);
                    pceResult.setRC(ResponseCodes.RESPONSE_OK);
                    LOG.info("In PostAlgoPathValidator: found TribPort {} - tribSlot {} - tribSlotNb {}",
                        tribPort, tribSlot, tribSlotNb);
                }
                break;

            case "ODU4":
                pceResult.setRC(ResponseCodes.RESPONSE_OK);
                LOG.info("In PostAlgoPathValidator: ODU4 path found {}", path);
                break;

            default:
                pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                LOG.warn("In PostAlgoPathValidator checkPath: unsupported serviceType {} found {}",
                    serviceType, path);
                break;
        }

        return pceResult;
    }

    // Choose the first available wavelength from the source to the destination
    private Long chooseWavelength(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes) {
        Long wavelength = -1L;
        for (long i = 1; i <= MAX_WAWELENGTH; i++) {
            boolean completed = true;
            LOG.debug("In chooseWavelength: {} {}", path.getLength(), path);
            for (PceGraphEdge edge : path.getEdgeList()) {
                LOG.debug("In chooseWavelength: source {} ", edge.link().getSourceId());
                PceNode pceNode = allPceNodes.get(edge.link().getSourceId());
                if (!pceNode.checkWL(i)) {
                    completed = false;
                    break;
                }
            }
            if (completed) {
                wavelength = i;
                break;
            }
        }
        return wavelength;
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
        PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes) {
        LOG.info("In choosetribPort: edgeList = {} ", path.getEdgeList());
        Map<String, Uint16> tribPortMap = new HashMap<>();

        for (PceGraphEdge edge : path.getEdgeList()) {
            NodeId linkSrcNode = edge.link().getSourceId();
            String linkSrcTp = edge.link().getSourceTP().toString();
            NodeId linkDestNode = edge.link().getDestId();
            String linkDestTp = edge.link().getDestTP().toString();
            PceNode pceOtnNodeSrc = allPceNodes.get(linkSrcNode);
            PceNode pceOtnNodeDest = allPceNodes.get(linkDestNode);
            List<Uint16> srcTpnPool = pceOtnNodeSrc.getAvailableTribPorts().get(linkSrcTp);
            List<Uint16> destTpnPool = pceOtnNodeDest.getAvailableTribPorts().get(linkDestTp);
            List<Uint16> commonEdgeTpnPool = new ArrayList<>();
            for (Uint16 integer : srcTpnPool) {
                if (destTpnPool.contains(integer)) {
                    commonEdgeTpnPool.add(integer);
                }
            }
            Collections.sort(commonEdgeTpnPool);
            if (!commonEdgeTpnPool.isEmpty()) {
                tribPortMap.put(edge.link().getLinkId().getValue(), commonEdgeTpnPool.get(0));
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
            String linkSrcTp = edge.link().getSourceTP().toString();
            NodeId linkDestNode = edge.link().getDestId();
            String linkDestTp = edge.link().getDestTP().toString();
            PceNode pceOtnNodeSrc = allPceNodes.get(linkSrcNode);
            PceNode pceOtnNodeDest = allPceNodes.get(linkDestNode);
            List<Uint16> srcTsPool = pceOtnNodeSrc.getAvailableTribSlots().get(linkSrcTp);
            List<Uint16> destTsPool = pceOtnNodeDest.getAvailableTribSlots().get(linkDestTp);
            List<Uint16> commonEdgeTsPool = new ArrayList<>();
            List<Uint16> tribSlotList = new ArrayList<>();
            for (Uint16 integer : srcTsPool) {
                if (destTsPool.contains(integer)) {
                    commonEdgeTsPool.add(integer);
                }
            }
            Collections.sort(commonEdgeTsPool);
            boolean discontinue = true;
            int index = 0;
            while (discontinue && (commonEdgeTsPool.size() - index >= nbSlot)) {
                discontinue = false;
                Integer val = commonEdgeTsPool.get(index).toJava();
                for (int i = 0; i < nbSlot; i++) {
                    if (commonEdgeTsPool.get(index + i).equals(Uint16.valueOf(val + i))) {
                        tribSlotList.add(commonEdgeTsPool.get(index + i));
                    } else {
                        discontinue = true;
                        tribSlotList.clear();
                        index += i;
                        break;
                    }
                }
            }
            tribSlotMap.put(edge.link().getLinkId().getValue(), tribSlotList);
        }
        tribSlotMap.forEach((k,v) -> LOG.info("TribSlotMap : k = {}, v = {}", k, v));
        return tribSlotMap;
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
        double osnrDb;
        osnrDb = 10 * Math.log10(osnrLu);
        return osnrDb;
    }

    private double getInverseOsnrLinkLu(double linkOsnrDb) {
        // 1 over the link OSNR, in linear units
        double linkOsnrLu;
        linkOsnrLu = Math.pow(10, (linkOsnrDb / 10.0));
        LOG.debug("In retrieveosnr: the inverse of link osnr is {} (Linear Unit)", linkOsnrLu);
        return (CONST_OSNR / linkOsnrLu);
    }

}
