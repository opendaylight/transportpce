/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAlgoPathValidator {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceGraph.class);

    // TODO hard-coded 96
    private static final int MAX_WAWELENGTH = 96;
    private static final double MIN_OSNR_W100G = 17;
    private static final double TRX_OSNR = 33;
    private static final double ADD_OSNR = 30;
    public static final Long CONST_OSNR = 1L;
    public static final double SYS_MARGIN = 0;

    public PceResult checkPath(GraphPath<String, PceGraphEdge> path,
            Map<NodeId, PceNode> allPceNodes, PceResult pceResult, PceConstraints pceHardConstraints) {

        //check if the path is empty
        if (path.getEdgeList().size() == 0) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            return pceResult;
        }

        //Choose wavelength available in all nodes of the path
        Long waveL = chooseWavelength(path, allPceNodes);
        if (waveL < 0) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            pceResult.setLocalCause(PceResult.LocalCause.NO_PATH_EXISTS);
            return pceResult;
        }
        pceResult.setResultWavelength(waveL);
        LOG.info("In PostAlgoPathValidator: chooseWavelength WL found {} {}", waveL, path.toString());

        // TODO here other post algo validations can be added
        // more data can be sent to PceGraph module via PceResult structure if required

        //Check the OSNR
        if (!checkOSNR(path)) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            pceResult.setLocalCause(PceResult.LocalCause.OUT_OF_SPEC_OSNR);
            return pceResult;
        }

        //Check if MaxLatency is defined in the hard constraints
        if (pceHardConstraints.getMaxLatency() != -1) {
            if (!checkLatency(pceHardConstraints.getMaxLatency(), path)) {
                pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
                pceResult.setLocalCause(PceResult.LocalCause.TOO_HIGH_LATENCY);
                return pceResult;
            }
        }

        //Check if nodes are included in the hard constraints
        if (!checkInclude(path, pceHardConstraints)) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            pceResult.setLocalCause(PceResult.LocalCause.HD_NODE_INCLUDE);
            return pceResult;
        }

        pceResult.setRC(ResponseCodes.RESPONSE_OK);
        return pceResult;
    }

    //Choose the first available wavelength from the source to the destination
    private Long chooseWavelength(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes) {
        Long wavelength = -1L;

        for (long i = 1; i <= MAX_WAWELENGTH; i++) {
            boolean completed = true;
            LOG.debug("In chooseWavelength: {} {}", path.getLength(), path.toString());
            for (PceGraphEdge edge : path.getEdgeList()) {
                LOG.debug("In chooseWavelength: source {} ", edge.link().getSourceId().toString());
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

    //Check the latency
    private boolean checkLatency(Long maxLatency, GraphPath<String, PceGraphEdge> path) {
        double latency = 0;

        for (PceGraphEdge edge : path.getEdgeList()) {
            try {
                latency += edge.link().getLatency();
                LOG.debug("- In checkLatency: latency of {} = {} units", edge.link().getLinkId().getValue(),latency);
            } catch (NullPointerException e) {
                LOG.warn("- In checkLatency: the link {} does not contain latency field",
                    edge.link().getLinkId().getValue());
            }
        }
        if (latency > maxLatency) {
            return false;
        }
        return true;
    }

    //Check the inclusion if it is defined in the hard constraints
    private boolean checkInclude(GraphPath<String, PceGraphEdge> path, PceConstraints pceHardConstraintsInput) {
        List<ResourcePair> listToInclude = pceHardConstraintsInput.getListToInclude();
        if (listToInclude.isEmpty()) {
            return true;
        }

        List<PceGraphEdge> pathEdges = path.getEdgeList();
        LOG.debug(" in checkInclude vertex list: [{}]", path.getVertexList());

        List<String> listOfElementsSubNode = new ArrayList<String>();
        listOfElementsSubNode.add(pathEdges.get(0).link().getsourceSupNodeId());
        listOfElementsSubNode.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.NODE,
            pceHardConstraintsInput));

        List<String> listOfElementsCLLI = new ArrayList<String>();
        listOfElementsCLLI.add(pathEdges.get(0).link().getsourceCLLI());
        listOfElementsCLLI.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.CLLI,
            pceHardConstraintsInput));

        List<String> listOfElementsSRLG = new ArrayList<String>();
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

        List<String> listOfElements = new ArrayList<String>();
        for (PceGraphEdge link: pathEdges) {
            switch (type) {
                case NODE:
                    listOfElements.add(link.link().getdestSupNodeId());
                    break;
                case CLLI:
                    listOfElements.add(link.link().getdestCLLI());
                    break;
                case SRLG:
                    if (link.link().getlinkType() != OpenroadmLinkType.ROADMTOROADM) {
                        listOfElements.add("NONE");
                        break;
                    }

                    // srlg of link is List<Long>. But in this algo we need string representation of one SRLG
                    // this should be any SRLG mentioned in include constraints if any of them if mentioned
                    boolean found = false;
                    for (Long srlg : link.link().getsrlgList()) {
                        String srlgStr = String.valueOf(srlg);
                        if (pceHardConstraints.getSRLGnames().contains(srlgStr)) {
                            listOfElements.add(srlgStr);
                            LOG.info("listOfElementsBuild. FOUND SRLG {} in link {}", srlgStr, link.link().toString());
                            found = true;
                            continue;
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

    //Check the path OSNR
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
        }
        catch (ArithmeticException e) {
            LOG.debug("In checkOSNR: OSNR is equal to 0 and the number of links is: {}", path.getEdgeList().size());
            return false;
        }
        LOG.info("In checkOSNR: OSNR of the path is {} dB", osnrDb);
        if ((osnrDb + SYS_MARGIN) < MIN_OSNR_W100G) {
            return false;
        }
        return true;
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