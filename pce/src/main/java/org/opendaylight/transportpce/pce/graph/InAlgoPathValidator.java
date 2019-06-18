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

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InAlgoPathValidator implements PathValidator<String, PceGraphEdge> {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceGraph.class);

    private PceConstraints pceHardConstraints = null;
    private PceNode zendNode = null;

    public InAlgoPathValidator(PceConstraints pceHardConstraints, PceNode zendNode) {
        super();
        this.pceHardConstraints = pceHardConstraints;
        this.zendNode = zendNode;
    }

    @Override
    public boolean isValidPath(GraphPath<String, PceGraphEdge> partialPath, PceGraphEdge edge) {
        int size = partialPath.getEdgeList().size();
        if (size == 0) {
            return true;
        }
        LOG.debug("InAlgoPathValidator: partialPath size: {} prev edge {} new edge {}",
            size, edge.link().getlinkType(), partialPath.getEdgeList().get(size - 1).link().getlinkType());

        if (!checkTurn(partialPath.getEdgeList().get(size - 1).link().getlinkType(), edge.link().getlinkType())) {
            return false;
        }
        if (!checkLimits(partialPath, edge, pceHardConstraints)) {
            return false;
        }
        if (!checkInclude(partialPath, edge, zendNode, pceHardConstraints)) {
            return false;
        }

        return true;
    }

    private boolean checkTurn(OpenroadmLinkType prevType, OpenroadmLinkType nextType) {

        if (nextType == OpenroadmLinkType.ADDLINK && prevType != OpenroadmLinkType.XPONDEROUTPUT) {
            LOG.debug("in checkPath dropped {} {} ", prevType, nextType);
            return false;
        }

        if (nextType == OpenroadmLinkType.EXPRESSLINK && prevType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug("in checkPath dropped {} {} ", prevType, nextType);
            return false;
        }

        if (nextType == OpenroadmLinkType.DROPLINK && prevType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug("in checkPath dropped {} {} ", prevType, nextType);
            return false;
        }

        if (nextType == OpenroadmLinkType.XPONDERINPUT && prevType != OpenroadmLinkType.DROPLINK) {
            LOG.debug("in checkPath dropped {} {} ", prevType, nextType);
            return false;
        }

        if (prevType == OpenroadmLinkType.EXPRESSLINK && nextType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug("in checkPath dropped {} {} ", prevType, nextType);
            return false;
        }

        if (prevType == OpenroadmLinkType.ADDLINK && nextType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug("in checkPath dropped {} {} ", prevType, nextType);
            return false;
        }

        return true;
    }

    /*
     * this method should be added to JgraphT as accumulated values inside path
     * (RankingPathElementList)
     */
    private boolean checkLimits(GraphPath<String, PceGraphEdge> partialPath,
                                PceGraphEdge edge, PceConstraints pceHardConstraintsInput) {

        Long latencyConstraint = pceHardConstraintsInput.getMaxLatency();
        if (latencyConstraint > 0) {
            long newLatency = Math.round(calcLatency(partialPath) + edge.link().getLatency());
            if (newLatency > latencyConstraint) {
                LOG.warn("In validateLatency: AtoZ path is dropped because of MAX LATENCY {} > {}",
                    newLatency, latencyConstraint);
                return false;
            }
        }

        return true;
    }

    private double calcLatency(GraphPath<String, PceGraphEdge> path) {
        double latency = 0;
        for (PceGraphEdge edge : path.getEdgeList()) {
            latency = latency + edge.link().getLatency();
        }
        return latency;
    }

    /*
     * checkInclude this method ensures the path is going over path elements
     * to be included, alway check target node in the new edge
     *
     */
    private boolean checkInclude(GraphPath<String, PceGraphEdge> partialPath,
                                 PceGraphEdge edge, PceNode zendNodeInput,
                                 PceConstraints pceHardConstraintsInput) {

        List<ResourcePair> listToInclude = pceHardConstraintsInput.getListToInclude();
        if (listToInclude.isEmpty()) {
            return true;
        }

        // run this check only for the last edge of path
        if (!edge.link().getDestId().getValue().equals(zendNodeInput.getNodeId().getValue())) {
            return true;
        }
        List<PceGraphEdge> pathEdges = partialPath.getEdgeList();
        pathEdges.add(edge);
        LOG.info(" in checkInclude vertex list: [{}]", partialPath.getVertexList());

        List<String> listOfElementsSubNode = new ArrayList<String>();
        listOfElementsSubNode.add(pathEdges.get(0).link().getsourceSupNodeId());
        listOfElementsSubNode.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.NODE));

        List<String> listOfElementsCLLI = new ArrayList<String>();
        listOfElementsCLLI.add(pathEdges.get(0).link().getsourceCLLI());
        listOfElementsCLLI.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.CLLI));

        List<String> listOfElementsSRLG = new ArrayList<String>();
        listOfElementsSRLG.add("NONE"); // first link is XPONDEROUTPUT, no SRLG for it
        listOfElementsSRLG.addAll(listOfElementsBuild(pathEdges, PceConstraints.ResourceType.SRLG));

        // validation: check each type for each element
        for (ResourcePair next : listToInclude) {
            int indx = -1;
            switch (next.type) {
                case NODE:
                    if (listOfElementsSubNode.contains(next.name)) {
                        indx = listOfElementsSubNode.indexOf(next.name);
                    }
                    break;
                case SRLG:
                    if (listOfElementsSRLG.contains(next.name)) {
                        indx = listOfElementsSRLG.indexOf(next.name);
                    }
                    break;
                case CLLI:
                    if (listOfElementsCLLI.contains(next.name)) {
                        indx = listOfElementsCLLI.indexOf(next.name);
                    }
                    break;
                default:
                    LOG.warn(" in checkInclude vertex list unsupported resource type: [{}]", next.type);
            }

            if (indx < 0) {
                LOG.debug(" in checkInclude stopped : {} ", next.name);
                return false;
            }

            LOG.debug(" in checkInclude next found {} in {}", next.name, partialPath.getVertexList());

            listOfElementsSubNode.subList(0, indx).clear();
            listOfElementsCLLI.subList(0, indx).clear();
            listOfElementsSRLG.subList(0, indx).clear();
        }

        LOG.info(" in checkInclude passed : {} ", partialPath.getVertexList());
        return true;
    }

    private List<String> listOfElementsBuild(List<PceGraphEdge> pathEdges, PceConstraints.ResourceType type) {
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
                    if (found == false) {
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
}
