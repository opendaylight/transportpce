/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.observer.Ignore;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
//import org.opendaylight.transportpce.pce.networkanalyzer.PceORLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult.LocalCause;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PceConstraintMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceGraph {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceGraph.class);

    ////////////////////////// for Graph ///////////////////////////
    // how many paths to bring
    private int kpathsToBring = 15;

    // input
    private Map<NodeId, PceNode> allPceNodes = new HashMap<>();
    private Map<LinkId, PceLink> allPceLinks = new HashMap<>();
    private PceNode apceNode = null;
    private PceNode zpceNode = null;
    private String serviceType = "";
    private Double margin = null;
    PceConstraints pceHardConstraints;
    private PceConstraintMode pceConstraintMode;
    private BitSet spectrumConstraint;
    private final ClientInput clientInput;

    // results
    private PceResult pceResult = null;
    private List<PceLink> shortestPathAtoZ = null;

    // for path calculation
    Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = null;

    private List<PceLink> pathAtoZ = new ArrayList<>();

    private final NetworkTransactionService networkTransactionService;

    public PceGraph(PceNode aendNode, PceNode zendNode, Map<NodeId, PceNode> allPceNodes,
            Map<LinkId, PceLink> allPceLinks, PceConstraints pceHardConstraints,PceResult pceResult,
            String serviceType, NetworkTransactionService networkTransactionService, PceConstraintMode mode,
            BitSet spectrumConstraint, ClientInput clientInput) {
        super();
        this.apceNode = aendNode;
        this.zpceNode = zendNode;
        this.allPceNodes = allPceNodes;
        this.allPceLinks = allPceLinks;
        this.pceResult = pceResult;
        this.pceHardConstraints = pceHardConstraints;
        this.serviceType = serviceType;
        this.networkTransactionService = networkTransactionService;
        this.pceConstraintMode = mode;
        this.spectrumConstraint = spectrumConstraint;
        this.clientInput = clientInput;

        LOG.info("In GraphCalculator: A and Z = {} / {} ", aendNode, zendNode);
        LOG.info("In PceGraph, serviceType is {} ", serviceType);
        LOG.debug("In GraphCalculator: allPceNodes size {}, nodes {} ", allPceNodes.size(), allPceNodes);
    }

    public boolean calcPath() {
        return calcPath(new Ignore());
    }

    public boolean calcPath(Subscriber errorSubscriber) {

        LOG.info(" In PCE GRAPH calcPath : K SHORT PATHS algorithm ");

        Graph<String, PceGraphEdge> weightedGraph =
                new DefaultDirectedWeightedGraph<>(PceGraphEdge.class);
        populateWithNodes(weightedGraph);
        populateWithLinks(weightedGraph);

        LOG.info(" InPCEGRAPHLine112 calcPath weightedGraph is {}", weightedGraph);
        if (!runKgraphs(weightedGraph)) {
            LOG.error("In calcPath : pceResult {}", pceResult);
            return false;
        }
        // validate found paths
        pceResult.error();
        for (Entry<Integer, GraphPath<String, PceGraphEdge>> entry : allWPaths.entrySet()) {
            GraphPath<String, PceGraphEdge> path = entry.getValue();
            LOG.info("validating path n° {} - {}", entry.getKey(), path.getVertexList());
            PostAlgoPathValidator papv = new PostAlgoPathValidator(
                    networkTransactionService,
                    spectrumConstraint,
                    clientInput);
            pceResult = papv.checkPath(
                    path, allPceNodes, allPceLinks, pceResult, pceHardConstraints, serviceType, pceConstraintMode);
            this.margin = papv.getTpceCalculatedMargin();
            if (ResponseCodes.RESPONSE_OK.equals(pceResult.getResponseCode())) {
                LOG.info("Path is validated");
            } else {
                errorSubscriber.error(pceResult.getMessage());
                LOG.warn("In calcPath: post algo validations DROPPED the path {}; for following cause: {}",
                    path, pceResult.getLocalCause());
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
                case StringConstants.SERVICE_TYPE_OTUC2:
                case StringConstants.SERVICE_TYPE_OTUC3:
                case StringConstants.SERVICE_TYPE_OTUC4:
                case StringConstants.SERVICE_TYPE_400GE:
                case StringConstants.SERVICE_TYPE_OTU4:
                case StringConstants.SERVICE_TYPE_OTHER:
                    LOG.debug(
                        "In calcPath Path FOUND path for wl [{}], min Freq assignment {}, max Freq assignment {},"
                        + " hops {}, distance per metrics {}, path AtoZ {}",
                        pceResult.getResultWavelength(), pceResult.getMinFreq(), pceResult.getMaxFreq(),
                        pathAtoZ.size(), path.getWeight(), pathAtoZ);
                    break;

                default:
                    LOG.debug(
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

    private boolean runKgraphs(Graph<String, PceGraphEdge> weightedGraph) {

        if (weightedGraph.edgeSet().isEmpty() || weightedGraph.vertexSet().isEmpty()) {
            pceResult.error("Unable to create a valid weighted graph to calculate the shortest path.");
            if (weightedGraph.edgeSet().isEmpty()) {
                LOG.info(" In runKgraphs : Edge of weighted graph is empty");
            }
            if (weightedGraph.vertexSet().isEmpty()) {
                LOG.info(" In runKgraphs : VertexSet of weighted graph is empty");
            }
            return false;
        }
        LOG.info(" In runKgraphs : weighted graph is : {}", weightedGraph);
        PathValidator<String, PceGraphEdge> wpv = new InAlgoPathValidator();

        // YenShortestPath on weightedGraph
        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);
        List<GraphPath<String, PceGraphEdge>> weightedPathList;
        LOG.info("kpathsToBring : {}", kpathsToBring);
        if (apceNode.getNodeUuid() == null && zpceNode.getNodeUuid() == null) {
            weightedPathList = swp
                .getPaths(apceNode.getNodeId().getValue(), zpceNode.getNodeId().getValue(), kpathsToBring);
        } else {
            LOG.info("in Pce Graph RunKGraph line201, search for a path between :{} AND {}",
                apceNode.getNodeUuid().getValue(), zpceNode.getNodeUuid().getValue());
            weightedPathList = swp
                .getPaths(apceNode.getNodeUuid().getValue(), zpceNode.getNodeUuid().getValue(), kpathsToBring);
            LOG.info("in Pce Graph RunKGraph line204, weighted path list :{} ", weightedPathList);
        }
        allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        if (allWPaths.isEmpty()) {
            LOG.info(" In runKgraphs : algorithm didn't find any path");
            pceResult.setLocalCause(LocalCause.NO_PATH_EXISTS);
            pceResult.error("No path found by algorithm.");
            return false;
        }

        // debug print
        allWPaths
            .forEach((k, v) -> LOG.info("path n° {} - weight: {} - path: {}", k, v.getWeight(), v.getVertexList()));
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
        LOG.info("In addLinkToGraphLine 237 validated link between nodes : {} & {} of type {} and Uuid {}",
            source.getNodeId(), dest.getNodeId(), pcelink.getlinkType(), pcelink.getLinkUuid());
        return true;
    }

    private void populateWithNodes(Graph<String, PceGraphEdge> weightedGraph) {
        Iterator<Map.Entry<NodeId, PceNode>> nodes = allPceNodes.entrySet().iterator();
        while (nodes.hasNext()) {
            Map.Entry<NodeId, PceNode> node = nodes.next();
            if (node.getValue().getState() != null && State.InService.equals(node.getValue().getState())) {
                weightedGraph.addVertex(node.getValue().getNodeId().getValue());
                LOG.info("In populateWithNodes add to Vertices node :  {}", node.getValue().getNodeId());
            } else if (node.getValue().getOperationalState() != null
                && OperationalState.ENABLED.equals(node.getValue().getOperationalState())) {
                weightedGraph.addVertex(node.getValue().getNodeUuid().getValue());
                LOG.info("In populateWithNodes add to Vertices tapi node :  {} of Uuid {}",
                    node.getValue().getNodeId(), node.getValue().getNodeUuid());
            }
        }
    }

    private boolean populateWithLinks(Graph<String, PceGraphEdge> weightedGraph) {

        Iterator<Map.Entry<NodeId, PceNode>> nodes = allPceNodes.entrySet().iterator();
        while (nodes.hasNext()) {

            Map.Entry<NodeId, PceNode> node = nodes.next();

            PceNode pcenode = node.getValue();
            List<PceLink> links = new ArrayList<>();
            for (PceLink pcelink : pcenode.getOutgoingLinks()) {
                links.add((PceLink) pcelink);
            }
            //List<PceLink> links = pcenode.getOutgoingLinks();

            LOG.debug("In Graph populateWithLinks: use node for graph {}", pcenode);

            for (PceLink link : links) {
                LOG.info("In Graph populateWithLinks node {} : add edge to graph {}",
                    pcenode.getNodeId(), link.getLinkId());

                if (!validateLinkforGraph(link)) {
                    LOG.info("PceGraph populateWithLinks Line279: Link {} of type {} is not valid",
                        link.getLinkId(), link.getlinkType());
                    continue;
                }
                PceGraphEdge graphLink = new PceGraphEdge(link);
                if (link.getState() != null && State.InService.equals(link.getState())) {
                    weightedGraph.addEdge(link.getSourceId().getValue(), link.getDestId().getValue(), graphLink);

                    weightedGraph.setEdgeWeight(graphLink, chooseWeight(link));
                    LOG.info("In Graph populateWithLinks added Edge :  {}", link.getLinkId());
                } else {
                    weightedGraph.addEdge(link.getSourceUuid().getValue(), link.getDestUuid().getValue(), graphLink);
                    weightedGraph.setEdgeWeight(graphLink, chooseWeight(link));
                    LOG.info("In Graph populateWithLinks added Edge :  {}", link.getLinkId());
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

    public List<PceLink> getPathAtoZ() {
        return shortestPathAtoZ;
    }

    public PceResult getReturnStructure() {
        return pceResult;
    }

    public Double getmargin() {
        return margin;
    }

    public void setConstrains(PceConstraints pceHardConstraintsInput) {
        this.pceHardConstraints = pceHardConstraintsInput;
    }
}
