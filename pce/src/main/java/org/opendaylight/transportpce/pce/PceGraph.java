/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections15.Transformer;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.PceResult.LocalCause;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PceGraph {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);

    ////////////////////////// for Graph ///////////////////////////
    private DirectedSparseMultigraph<PceNode, PceLink> nwGraph = new DirectedSparseMultigraph<PceNode, PceLink>();
    private DijkstraShortestPath<PceNode, PceLink> shortestPath = null;

    // input
    private Map<NodeId, PceNode> allPceNodes = new HashMap<NodeId, PceNode>();
    private PceNode apceNode = null;
    private PceNode zpceNode = null;

    PceConstraints pceHardConstraints;
    PceConstraints pceSoftConstraints;

    // results
    private PceResult pceResult = null;
    private List<PceLink> shortestPathAtoZ = null;

    // TODO hard-coded 96
    private static final int MAX_WAWELENGTH = 96;

    // for path calculation
    private List<PceLink> pathAtoZ = null;
    private int minFoundDistance;
    private int tmpAtozDistance = 0;
    private int tmpAtozLatency = 0;
    private int bestDistance;
    private boolean noPathExists = false;

    private boolean foundButTooHighLatency = false;

    private List<ListOfNodes> listOfNodesPerWL = new ArrayList<ListOfNodes>();

    public PceGraph(PceNode aendNode, PceNode zendNode, Map<NodeId, PceNode> allPceNodes,
            PceConstraints pceHardConstraints, PceConstraints pceSoftConstraints, PceResult pceResult) {
        super();
        this.apceNode = aendNode;
        this.zpceNode = zendNode;
        this.allPceNodes = allPceNodes;
        this.pceResult = pceResult;
        this.pceHardConstraints = pceHardConstraints;
        this.pceSoftConstraints = pceSoftConstraints;

        // TODO - fix the assumption that wavelengths are from 1 to 96 and can be used
        // as index
        this.listOfNodesPerWL.add(new ListOfNodes());
        for (int i = 1; i <= MAX_WAWELENGTH; i++) {
            // create list of nodes per wavelength
            ListOfNodes wls = new ListOfNodes();
            this.listOfNodesPerWL.add(wls);
        }

        LOG.debug("In GraphCalculator: A and Z = {} / {}", aendNode.toString(), zendNode.toString());
        LOG.debug("In GraphCalculator: allPceNodes = {}", allPceNodes.toString());
    }

    public boolean calcPath() {

        LOG.info("In calcPath: metric {} is used ", this.pceHardConstraints.getPceMetrics());

        populateGraph(this.allPceNodes);

        LOG.info(" In PCE GRAPH : QUICK algorithm ");

        // quick algorithm
        if (runGraph()) {

            this.bestDistance = this.tmpAtozDistance;

            if (chooseWavelength()) {
                this.pceResult.setRC(ResponseCodes.RESPONSE_OK);
                this.shortestPathAtoZ = this.pathAtoZ;
                LOG.info("In GraphCalculator QUICK CalcPath: AtoZ {}", this.pathAtoZ.toString());
                LOG.info("In GraphCalculator QUICK CalcPath: pceResult {}", this.pceResult.toString());
                return true;
            }

            // continue work per wavelength
            LOG.warn(" In PCE GRAPH : QUICK algorithm didn't find shared wavelength over the shortest path");

        }

        LOG.warn(" In PCE GRAPH : QUICK algorithm didn't find shortest path with single wavelength");
        if (this.noPathExists) {
            // quick algo looks for path independently on wavelength. therefore no path
            // means fatal problem
            LOG.warn(" In PCE GRAPH : QUICK algorithm didn't find any path");
            this.pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            return false;
        }

        // rearrange all nodes per the relevant wavelength indexes
        extractWLs(this.allPceNodes);

        this.pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
        boolean firstPath = true;

        for (int i = 1; i <= MAX_WAWELENGTH; i++) {
            LOG.info(" In PCE GRAPH : FUll algorithm for WL {}", i);
            List<PceNode> nodes = this.listOfNodesPerWL.get(i).getNodes();
            populateGraph(nodes);

            if (!runGraph()) {
                continue;
            }

            if (firstPath) {
                // set minFoundDistance for the first time
                rememberPath(i);
                firstPath = false;
            }

            if (this.tmpAtozDistance < this.minFoundDistance) {
                rememberPath(i);
            }

            if (this.tmpAtozDistance == this.bestDistance) {
                // optimization: stop on the first WL with result == the best
                break;
            }
        }

        // return codes can come in different orders. this method fixes it a bit
        // TODO build it better
        analyzeResult();

        LOG.info("In GraphCalculator FUll CalcPath: pceResult {}", this.pceResult.toString());
        return (this.pceResult.getStatus());
    }

    private boolean populateGraph(Map<NodeId, PceNode> allNodes) {

        cleanupGraph();

        Iterator<Map.Entry<NodeId, PceNode>> nodes = allNodes.entrySet().iterator();
        while (nodes.hasNext()) {

            Map.Entry<NodeId, PceNode> node = nodes.next();

            PceNode pcenode = node.getValue();
            List<PceLink> links = pcenode.getOutgoingLinks();

            LOG.debug("In populateGraph: use node for graph {}", pcenode.toString());

            for (PceLink link : links) {
                LOG.debug("In populateGraph: add edge to graph {}", link.toString());
                addLinkToGraph(link);

            }

        }

        return true;
    }

    private boolean populateGraph(List<PceNode> allNodes) {

        cleanupGraph();

        for (PceNode node : allNodes) {
            List<PceLink> links = node.getOutgoingLinks();

            LOG.debug("In populateGraph: use node for graph {}", node.toString());

            for (PceLink link : links) {
                LOG.debug("In populateGraph: add edge to graph {}", link.toString());
                addLinkToGraph(link);

            }

        }

        return true;
    }

    private boolean runGraph() {
        LOG.info("In runGraph Vertices: {}; Eges: {} ", this.nwGraph.getVertexCount(), this.nwGraph.getEdgeCount());

        this.pathAtoZ = null;

        try {
            this.shortestPath = calcAlgo();

            if (this.shortestPath == null) {
                this.noPathExists = true;
                LOG.error("In runGraph: shortest path alg is null ");// ,
                return false;
            }

            this.pathAtoZ = this.shortestPath.getPath(this.apceNode, this.zpceNode);

            if ((this.pathAtoZ == null) || (this.pathAtoZ.size() == 0)) {
                LOG.info("In runGraph: AtoZ path is empty");
                this.pceResult.setLocalCause(LocalCause.NO_PATH_EXISTS);
                return false;
            }

            pathMetricsToCompare();

            return compareMaxLatency();

        } catch (IllegalArgumentException e) {
            LOG.error("In runGraph: can't calculate the path. A or Z node don't have any links {}", e);
            this.noPathExists = true;
            return false;

        }
    }

    private DijkstraShortestPath<PceNode, PceLink> calcAlgo() {

        Transformer<PceLink, Double> wtTransformer = new Transformer<PceLink, Double>() {
            @Override
            public Double transform(PceLink link) {
                return link.getLatency();
            }
        };

        this.shortestPath = null;

        switch (this.pceHardConstraints.getPceMetrics()) {
            case PropagationDelay:
                this.shortestPath = new DijkstraShortestPath<>(this.nwGraph, wtTransformer);
                LOG.debug("In calcShortestPath: PropagationDelay method run ");
                break;
            case HopCount:
                this.shortestPath = new DijkstraShortestPath<>(this.nwGraph);
                LOG.debug("In calcShortestPath: HopCount method run ");
                break;

            default:
                this.shortestPath = new DijkstraShortestPath<>(this.nwGraph);
                LOG.warn("In calcShortestPath: instead IGPMetric/TEMetric method Hop-Count runs as a default ");
                break;
        }

        return this.shortestPath;

    }

    private void addLinkToGraph(PceLink pcelink) {

        PceNode source = this.allPceNodes.get(pcelink.getSourceId());
        PceNode dest = this.allPceNodes.get(pcelink.getDestId());

        if (source == null) {
            LOG.error("In addLinkToGraph link source node is null  :   {}", pcelink.toString());
            return;
        }
        if (dest == null) {
            LOG.error("In addLinkToGraph link dest node is null  :   {}", pcelink.toString());
            return;
        }

        LOG.debug("In addLinkToGraph link and nodes :  {} ; {} / {}", pcelink.toString(), source.toString(),
                dest.toString());
        this.nwGraph.addEdge(pcelink, source, dest);

    }

    /*
     * "QUICK" approach build shortest path. and then look for a single wavelength
     * on it
     */
    private boolean chooseWavelength() {
        for (long i = 1; i <= MAX_WAWELENGTH; i++) {
            boolean completed = true;
            for (PceLink link : this.pathAtoZ) {
                PceNode pceNode = this.allPceNodes.get(link.getSourceId());
                if (!pceNode.checkWL(i)) {
                    completed = false;
                    break;
                }
            }
            if (completed) {
                this.pceResult.setResultWavelength(i);
                break;
            }
        }
        return (this.pceResult.getResultWavelength() > 0);
    }

    public List<PceLink> getPathAtoZ() {
        return this.shortestPathAtoZ;
    }

    public PceResult getReturnStructure() {
        return this.pceResult;
    }

    // TODO build ordered set ordered per the index. Current assumption is that
    // wavelenght serves as an index
    private class ListOfNodes {
        private List<PceNode> listOfNodes = new ArrayList<PceNode>();

        private void addNodetoWL(PceNode node) {
            this.listOfNodes.add(node);
        }

        private List<PceNode> getNodes() {
            return this.listOfNodes;
        }

    }

    private boolean extractWLs(Map<NodeId, PceNode> allNodes) {

        Iterator<Map.Entry<NodeId, PceNode>> nodes = allNodes.entrySet().iterator();
        while (nodes.hasNext()) {

            Map.Entry<NodeId, PceNode> node = nodes.next();

            PceNode pcenode = node.getValue();
            List<Long> wls = pcenode.getAvailableWLs();

            LOG.debug("In extractWLs wls in node : {} {}", pcenode.toString(), wls.size());
            LOG.debug("In extractWLs listOfWLs total :   {}", this.listOfNodesPerWL.size());
            for (Long i : wls) {
                LOG.debug("In extractWLs i in wls :  {}", i);
                ListOfNodes lwl = this.listOfNodesPerWL.get(i.intValue());
                lwl.addNodetoWL(pcenode);
            }
        }

        return true;
    }

    private void cleanupGraph() {
        LOG.debug("In cleanupGraph remove {} nodes ", this.nwGraph.getEdgeCount());
        Iterable<PceNode> toRemove = new ArrayList<PceNode>(this.nwGraph.getVertices());
        for (PceNode node : toRemove) {
            this.nwGraph.removeVertex(node);
        }
        LOG.debug("In cleanupGraph after {} removed ", this.nwGraph.getEdgeCount());
    }

    private void analyzeResult() {
        // very simple for the start

        if (this.pceResult.getStatus()) {
            return;
        }

        // if request is rejected but at least once there was path found, try to save
        // the real reason of reject
        if (this.foundButTooHighLatency) {
            this.pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            this.pceResult.setLocalCause(LocalCause.TOO_HIGH_LATENCY);
            this.pceResult.setCalcMessage("No path available due to constraint Hard/Latency");
        }
        return;
    }

    private boolean compareMaxLatency() {

        Long latencyConstraint = this.pceHardConstraints.getMaxLatency();

        if (latencyConstraint > 0) {
            if (this.tmpAtozLatency > latencyConstraint) {
                this.foundButTooHighLatency = true;
                this.pceResult.setLocalCause(LocalCause.TOO_HIGH_LATENCY);
                LOG.info("In validateLatency: AtoZ path has too high LATENCY {} > {}", this.tmpAtozLatency,
                        latencyConstraint);
                return false;
            }
        }
        LOG.info("In validateLatency: AtoZ path  is {}", this.pathAtoZ.toString());
        return true;
    }

    private void pathMetricsToCompare() {

        this.tmpAtozDistance = this.shortestPath.getDistance(this.apceNode, this.zpceNode).intValue();

        // TODO this code is for HopCount. excluded from switch for not implemented
        // IGPMetric and TEMetric
        this.tmpAtozLatency = 0;
        for (PceLink pcelink : this.pathAtoZ) {
            this.tmpAtozLatency = this.tmpAtozLatency + pcelink.getLatency().intValue();
        }

        switch (this.pceHardConstraints.getPceMetrics()) {
            case IGPMetric:
                // TODO implement IGPMetric - low priority
                LOG.error("In PceGraph not implemented IGPMetric. HopCount works as a default");
                break;

            case TEMetric:
                // TODO implement TEMetric - low priority
                LOG.error("In PceGraph not implemented TEMetric. HopCount works as a default");
                break;

            case HopCount:
                break;

            case PropagationDelay:
                this.tmpAtozLatency = this.tmpAtozDistance;
                break;

            default:
                LOG.error("In PceGraph {}: unknown metric. ", this.pceHardConstraints.getPceMetrics());
                break;
        }

        LOG.info("In runGraph: AtoZ size {}, distance {}, latency {} ", this.pathAtoZ.size(), this.tmpAtozDistance,
                this.tmpAtozLatency);
        LOG.debug("In runGraph: AtoZ {}", this.pathAtoZ.toString());

        return;
    }

    private void rememberPath(int index) {
        this.minFoundDistance = this.tmpAtozDistance;
        this.shortestPathAtoZ = this.pathAtoZ;
        this.pceResult.setResultWavelength(Long.valueOf(index));
        this.pceResult.setRC(ResponseCodes.RESPONSE_OK);
        LOG.info("In GraphCalculator FUll CalcPath for wl [{}]: found AtoZ {}", index, this.pathAtoZ.toString());

    }

    public void setConstrains(PceConstraints pceHardConstraintsIn, PceConstraints pceSoftConstraintsIn) {
        this.pceHardConstraints = pceHardConstraintsIn;
        this.pceSoftConstraints = pceSoftConstraintsIn;
    }

}
