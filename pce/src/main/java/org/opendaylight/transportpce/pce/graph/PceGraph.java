/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult.LocalCause;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceGraph {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceGraph.class);

    ////////////////////////// for Graph ///////////////////////////
    int kpathsToBring = 10; // how many paths to bring
    int mhopsPerPath = 50; // max #hops

    // input
    private Map<NodeId, PceNode> allPceNodes = new HashMap<NodeId, PceNode>();
    private PceNode apceNode = null;
    private PceNode zpceNode = null;

    PceConstraints pceHardConstraints;
    PceConstraints pceSoftConstraints;

    // results
    private PceResult pceResult = null;
    private List<PceLink> shortestPathAtoZ = null;

    // for path calculation
    List<GraphPath<String, PceGraphEdge>> allWPaths = null;

    private List<PceLink> pathAtoZ = new ArrayList<PceLink>();

    public PceGraph(PceNode aendNode, PceNode zendNode, Map<NodeId, PceNode> allPceNodes,
            PceConstraints pceHardConstraints, PceConstraints pceSoftConstraints, PceResult pceResult) {
        super();
        this.apceNode = aendNode;
        this.zpceNode = zendNode;
        this.allPceNodes = allPceNodes;
        this.pceResult = pceResult;
        this.pceHardConstraints = pceHardConstraints;
        this.pceSoftConstraints = pceSoftConstraints;

        LOG.info("In GraphCalculator: A and Z = {} / {} ", aendNode.toString(), zendNode.toString());
        LOG.debug("In GraphCalculator: allPceNodes size {}, nodes {} ", allPceNodes.size(), allPceNodes.toString());

        // PceCalculation.printNodesInfo(allPceNodes);

    }

    public boolean calcPath() {

        LOG.info(" In PCE GRAPH calcPath : K SHORT PATHS algorithm ");

        DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph =
                new DefaultDirectedWeightedGraph<String, PceGraphEdge>(PceGraphEdge.class);
        populateWithNodes(weightedGraph);
        populateWithLinks(weightedGraph);

        if (!runKgraphs(weightedGraph)) {
            LOG.info("In calcPath : pceResult {}", pceResult.toString());
            return false;
        }

        // validate found paths
        pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
        for (GraphPath<String, PceGraphEdge> path : allWPaths) {

            PostAlgoPathValidator papv = new PostAlgoPathValidator();
            pceResult = papv.checkPath(path, allPceNodes, pceResult);
            LOG.info("In calcPath after PostAlgoPathValidator {} {}",
                    pceResult.getResponseCode(), ResponseCodes.RESPONSE_OK);

            if (!pceResult.getResponseCode().equals(ResponseCodes.RESPONSE_OK)) {
                LOG.info("In calcPath: post algo validations DROPPED the path {}", path.toString());
                continue;
            }

            // build pathAtoZ
            pathAtoZ.clear();
            for (PceGraphEdge edge : path.getEdgeList()) {
                pathAtoZ.add(edge.link());
            }

            shortestPathAtoZ = new ArrayList<>(pathAtoZ);
            LOG.info("In calcPath Path FOUND path for wl [{}], hops {}, distance per metrics {}, path AtoZ {}",
                    pceResult.getResultWavelength(), pathAtoZ.size(), path.getWeight(), pathAtoZ.toString());
            break;
        }

        if (shortestPathAtoZ != null) {
            LOG.info("In calcPath CHOOSEN PATH for wl [{}], hops {}, path AtoZ {}",
                    pceResult.getResultWavelength(), shortestPathAtoZ.size(), shortestPathAtoZ.toString());
        }
        LOG.info("In calcPath : pceResult {}", pceResult.toString());
        return (pceResult.getStatus());
    }

    private boolean runKgraphs(DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph) {

        if (weightedGraph.edgeSet().isEmpty() || weightedGraph.vertexSet().isEmpty()) {
            return false;
        }

        PathValidator<String, PceGraphEdge> wpv = new InAlgoPathValidator(pceHardConstraints, zpceNode);

        // local optimization. if 'include' constraint exists then increase amount of paths to return.
        // it's because this constraint is checked at the last step when part of good paths
        // are dropped by other constraints
        if (!pceHardConstraints.getListToInclude().isEmpty()) {
            kpathsToBring = kpathsToBring * 10;
            LOG.info("k = {}",kpathsToBring);
        }

        // KShortestPaths on weightedGraph
        KShortestPaths<String, PceGraphEdge> swp =
            new KShortestPaths<String, PceGraphEdge>(weightedGraph, kpathsToBring, mhopsPerPath, wpv);

        allWPaths = swp.getPaths(apceNode.getNodeId().getValue(), zpceNode.getNodeId().getValue());

        if (allWPaths.isEmpty()) {
            LOG.info(" In runKgraphs : algorithm didn't find any path");
            pceResult.setLocalCause(LocalCause.NO_PATH_EXISTS);
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            return false;
        }

        // debug print
        for (GraphPath<String, PceGraphEdge> path : allWPaths) {
            LOG.info("path Weight: {} : {}", path.getWeight(), path.getVertexList().toString());
        }
        // debug print

        return true;
    }

    private boolean validateLinkforGraph(PceLink pcelink) {

        PceNode source = allPceNodes.get(pcelink.getSourceId());
        PceNode dest = allPceNodes.get(pcelink.getDestId());

        if (source == null) {
            LOG.error("In addLinkToGraph link source node is null : {}", pcelink.toString());
            return false;
        }
        if (dest == null) {
            LOG.error("In addLinkToGraph link dest node is null : {}", pcelink.toString());
            return false;
        }

        LOG.debug("In addLinkToGraph link to nodes : {}{} {}", pcelink.toString(), source.toString(), dest.toString());
        return true;

    }

    private void populateWithNodes(DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph) {
        Iterator<Map.Entry<NodeId, PceNode>> nodes = allPceNodes.entrySet().iterator();
        while (nodes.hasNext()) {
            Map.Entry<NodeId, PceNode> node = nodes.next();
            weightedGraph.addVertex(node.getValue().getNodeId().getValue());
            LOG.debug("In populateWithNodes in node :  {}", node.getValue().toString());
        }
    }

    private boolean populateWithLinks(DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph) {

        Iterator<Map.Entry<NodeId, PceNode>> nodes = allPceNodes.entrySet().iterator();
        while (nodes.hasNext()) {

            Map.Entry<NodeId, PceNode> node = nodes.next();

            PceNode pcenode = node.getValue();
            List<PceLink> links = pcenode.getOutgoingLinks();

            LOG.debug("In populateGraph: use node for graph {}", pcenode.toString());

            for (PceLink link : links) {
                LOG.debug("In populateGraph node {} : add edge to graph {}", pcenode.toString(), link.toString());

                if (!validateLinkforGraph(link)) {
                    continue;
                }

                PceGraphEdge graphLink = new PceGraphEdge(link);
                weightedGraph.addEdge(link.getSourceId().getValue(), link.getDestId().getValue(), graphLink);

                weightedGraph.setEdgeWeight(graphLink, chooseWeight(link));
            }
        }
        return true;
    }

    private double chooseWeight(PceLink link) {

        // HopCount is default
        double weight = 1;
        switch (pceHardConstraints.getPceMetrics()) {
            case IGPMetric :
                // TODO implement IGPMetric - low priority.
                LOG.warn("In PceGraph not implemented IGPMetric. HopCount works as a default");
                break;

            case TEMetric :
                // TODO implement TEMetric - low priority
                LOG.warn("In PceGraph not implemented TEMetric. HopCount works as a default");
                break;

            case HopCount :
                weight = 1;
                LOG.debug("In PceGraph HopCount is used as a metrics. {}", link.toString());
                break;

            case PropagationDelay :
                weight = link.getLatency();
                LOG.debug("In PceGraph PropagationDelay is used as a metrics. {}", link.toString());
                break;

            default:
                break;
        }

        return weight;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public List<PceLink> getPathAtoZ() {
        return shortestPathAtoZ;
    }

    public PceResult getReturnStructure() {
        return pceResult;
    }

    public void setConstrains(PceConstraints pceHardConstraintsInput, PceConstraints pceSoftConstraintsInput) {
        this.pceHardConstraints = pceHardConstraintsInput;
        this.pceSoftConstraints = pceSoftConstraintsInput;
    }

}
