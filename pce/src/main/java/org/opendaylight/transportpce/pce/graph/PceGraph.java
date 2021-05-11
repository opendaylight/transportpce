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
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult.LocalCause;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceGraph {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceGraph.class);

    ////////////////////////// for Graph ///////////////////////////
    // how many paths to bring
    private int kpathsToBring = 10;

    // max #hops
    private int mhopsPerPath = 50;

    // input
    private Map<NodeId, PceNode> allPceNodes = new HashMap<>();
    private PceNode apceNode = null;
    private PceNode zpceNode = null;
    private String serviceType = "";

    PceConstraints pceHardConstraints;
    PceConstraints pceSoftConstraints;

    // results
    private PceResult pceResult = null;
    private List<PceLink> shortestPathAtoZ = null;

    // for path calculation
    List<GraphPath<String, PceGraphEdge>> allWPaths = null;

    private List<PceLink> pathAtoZ = new ArrayList<>();

    public PceGraph(PceNode aendNode, PceNode zendNode, Map<NodeId, PceNode> allPceNodes,
            PceConstraints pceHardConstraints, PceConstraints pceSoftConstraints, PceResult pceResult,
            String serviceType) {
        super();
        this.apceNode = aendNode;
        this.zpceNode = zendNode;
        this.allPceNodes = allPceNodes;
        this.pceResult = pceResult;
        this.pceHardConstraints = pceHardConstraints;
        this.pceSoftConstraints = pceSoftConstraints;
        this.serviceType = serviceType;

        LOG.info("In GraphCalculator: A and Z = {} / {} ", aendNode, zendNode);
        LOG.debug("In GraphCalculator: allPceNodes size {}, nodes {} ", allPceNodes.size(), allPceNodes);
    }

    public boolean calcPath() {

        LOG.info(" In PCE GRAPH calcPath : K SHORT PATHS algorithm ");

        DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph =
                new DefaultDirectedWeightedGraph<>(PceGraphEdge.class);
        populateWithNodes(weightedGraph);
        populateWithLinks(weightedGraph);

        if (!runKgraphs(weightedGraph)) {
            LOG.info("In calcPath : pceResult {}", pceResult);
            return false;
        }

        // validate found paths
        pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
        for (GraphPath<String, PceGraphEdge> path : allWPaths) {
            PostAlgoPathValidator papv = new PostAlgoPathValidator();
            pceResult = papv.checkPath(path, allPceNodes, pceResult, pceHardConstraints, serviceType);
            LOG.info("In calcPath after PostAlgoPathValidator {} {}",
                    pceResult.getResponseCode(), ResponseCodes.RESPONSE_OK);

            if (!pceResult.getResponseCode().equals(ResponseCodes.RESPONSE_OK)) {
                LOG.warn("In calcPath: post algo validations DROPPED the path {}", path);
                continue;
            }

            // build pathAtoZ
            pathAtoZ.clear();
            for (PceGraphEdge edge : path.getEdgeList()) {
                pathAtoZ.add(edge.link());
            }

            shortestPathAtoZ = new ArrayList<>(pathAtoZ);
            switch (serviceType) {

                case StringConstants.SERVICE_TYPE_100GE_T:
                case StringConstants.SERVICE_TYPE_OTUC4:
                case StringConstants.SERVICE_TYPE_400GE:
                case StringConstants.SERVICE_TYPE_OTU4:
                    LOG.info(
                        "In calcPath Path FOUND path for wl [{}], min Freq assignment {}, max Freq assignment {},"
                        + " hops {}, distance per metrics {}, path AtoZ {}",
                        pceResult.getResultWavelength(), pceResult.getMinFreq(), pceResult.getMaxFreq(),
                        pathAtoZ.size(), path.getWeight(), pathAtoZ);
                    break;

                default:
                    LOG.info(
                        "In calcPath Path FOUND path for hops {}, distance per metrics {}, path AtoZ {}",
                        pathAtoZ.size(), path.getWeight(), pathAtoZ);
                    break;
            }
            break;

        }

        if (shortestPathAtoZ != null) {
            LOG.info("In calcPath CHOOSEN PATH for wl [{}], min freq {}, max freq {}, hops {}, path AtoZ {}",
                    pceResult.getResultWavelength(), pceResult.getMinFreq(), pceResult.getMaxFreq(),
                    shortestPathAtoZ.size(), shortestPathAtoZ);
        }
        LOG.info("In calcPath : pceResult {}", pceResult);
        return (pceResult.getStatus());
    }

    private boolean runKgraphs(DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph) {

        if (weightedGraph.edgeSet().isEmpty() || weightedGraph.vertexSet().isEmpty()) {
            return false;
        }
        PathValidator<String, PceGraphEdge> wpv = new InAlgoPathValidator();

        // KShortestPaths on weightedGraph
        KShortestSimplePaths<String, PceGraphEdge> swp =
            new KShortestSimplePaths<>(weightedGraph, mhopsPerPath, wpv);
        allWPaths = swp.getPaths(apceNode.getNodeId().getValue(), zpceNode.getNodeId().getValue(), kpathsToBring);

        if (allWPaths.isEmpty()) {
            LOG.info(" In runKgraphs : algorithm didn't find any path");
            pceResult.setLocalCause(LocalCause.NO_PATH_EXISTS);
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            return false;
        }

        // debug print
        for (GraphPath<String, PceGraphEdge> path : allWPaths) {
            LOG.debug("path Weight: {} : {}", path.getWeight(), path.getVertexList());
        }

        return true;
    }

    private boolean validateLinkforGraph(PceLink pcelink) {

        PceNode source = allPceNodes.get(pcelink.getSourceId());
        PceNode dest = allPceNodes.get(pcelink.getDestId());

        if (source == null) {
            LOG.error("In addLinkToGraph link source node is null : {}", pcelink);
            return false;
        }
        if (dest == null) {
            LOG.error("In addLinkToGraph link dest node is null : {}", pcelink);
            return false;
        }
        LOG.debug("In addLinkToGraph link to nodes : {}{} {}", pcelink, source, dest);
        return true;
    }

    private void populateWithNodes(DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph) {
        Iterator<Map.Entry<NodeId, PceNode>> nodes = allPceNodes.entrySet().iterator();
        while (nodes.hasNext()) {
            Map.Entry<NodeId, PceNode> node = nodes.next();
            if (State.InService.equals(node.getValue().getState())) {
                weightedGraph.addVertex(node.getValue().getNodeId().getValue());
                LOG.debug("In populateWithNodes in node :  {}", node.getValue());
            }
        }
    }

    private boolean populateWithLinks(DefaultDirectedWeightedGraph<String, PceGraphEdge> weightedGraph) {

        Iterator<Map.Entry<NodeId, PceNode>> nodes = allPceNodes.entrySet().iterator();
        while (nodes.hasNext()) {

            Map.Entry<NodeId, PceNode> node = nodes.next();

            PceNode pcenode = node.getValue();
            List<PceLink> links = pcenode.getOutgoingLinks();

            LOG.debug("In populateGraph: use node for graph {}", pcenode);

            for (PceLink link : links) {
                LOG.debug("In populateGraph node {} : add edge to graph {}", pcenode, link);

                if (!validateLinkforGraph(link)) {
                    continue;
                }
                if (State.InService.equals(link.getState())) {
                    PceGraphEdge graphLink = new PceGraphEdge(link);
                    weightedGraph.addEdge(link.getSourceId().getValue(), link.getDestId().getValue(), graphLink);

                    weightedGraph.setEdgeWeight(graphLink, chooseWeight(link));
                }
            }
        }
        return true;
    }

    private double chooseWeight(PceLink link) {
        // HopCount is default
        double weight = 1;
        switch (pceHardConstraints.getPceMetrics()) {
            case HopCount :
                weight = 1;
                LOG.debug("In PceGraph HopCount is used as a metrics. {}", link);
                break;
            case PropagationDelay :
                weight = link.getLatency();
                LOG.debug("In PceGraph PropagationDelay is used as a metrics. {}", link);
                if ((weight == 0)
                        && ("1GE".equals(serviceType) || "10GE".equals(serviceType) || "ODU4".equals(serviceType))) {
                    LOG.warn("PropagationDelay set as metric, but latency is null: is latency set for OTN link {}?",
                        link);
                }
                break;
            // TODO implement IGPMetric and TEMetric - low priority.
            case IGPMetric :
            case TEMetric :
            default:
                LOG.warn("In PceGraph {} not implemented. HopCount works as a default",
                    pceHardConstraints.getPceMetrics());
                break;
        }
        return weight;
    }

    public int getKpathsToBring() {
        return kpathsToBring;
    }

    public void setKpathsToBring(int kpathsToBring) {
        this.kpathsToBring = kpathsToBring;
    }

    public void setMhopsPerPath(int mhopsPerPath) {
        this.mhopsPerPath = mhopsPerPath;
    }

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
