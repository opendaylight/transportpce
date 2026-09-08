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
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.observer.Ignore;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.PceSendingPceRPCs;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult.LocalCause;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev250902.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev250902.TerminationPoint2;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PceConstraintMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.DomainTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.CdServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.AToZImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.ZToAImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint8;
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
    private String serviceLayer = "";
    private Double margin = null;
    PceConstraints pceHardConstraints;
    private PceConstraintMode pceConstraintMode;
    private String pceOperMode;
    private BitSet spectrumConstraint;
    private final ClientInput clientInput;
    private String aendOperationalMode ;
    private String zendOperationalMode;
    private int tapiSbiAbsNodeOrderInPath = -1;
    private int pathOrderInHybidPathComputation = 0;
    private List<PathElement> pathElementList = new ArrayList<>();
    private Map<Integer, CdServicesBuilder> crossDomainServiceBldr = new HashMap<>();
    private boolean isSecondStepHybridPC;
    private int npathorder;
    // Storage of impairment result (OpenROADM domains) for each KorderPath (FirstKey)
    // and each domains (2NdKey is domain order).
    private Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> openRoadmAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> openRoadmZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> tapiAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> tapiZtoASubPathImpairments = new HashMap<>();

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
            BitSet spectrumConstraint, ClientInput clientInput, String serviceLayer,
            boolean isSecondStepHybridPC, int npathorder) {
        super();
        this.apceNode = aendNode;
        this.zpceNode = zendNode;
        this.allPceNodes = allPceNodes;
        this.allPceLinks = allPceLinks;
        this.pceResult = pceResult;
        this.pceHardConstraints = pceHardConstraints;
        this.serviceType = serviceType;
        this.serviceLayer = serviceLayer;
        this.networkTransactionService = networkTransactionService;
        this.pceConstraintMode = mode;
        this.spectrumConstraint = spectrumConstraint;
        this.clientInput = clientInput;
        this.isSecondStepHybridPC = isSecondStepHybridPC;
        this.npathorder = npathorder;
        this.pceOperMode = PceSendingPceRPCs.OR_PCE_OPER_MODE;

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
        Integer kpathorder = 0;
        Map<Integer, List<SubGraphPath>> subGraphPathMap = new HashMap<>();
        for (Entry<Integer, GraphPath<String, PceGraphEdge>> entry : allWPaths.entrySet()) {
            GraphPath<String, PceGraphEdge> path = entry.getValue();
            LOG.info("validating path n° {} - {}", entry.getKey(), path.getVertexList());
            if (!isPathHybrid(path, allPceNodes)) {
                // This is the regular path computation process going through either the OR PCE
                // or TAPI PCE when Service creation is triggered through TAPI API.
                PostAlgoPathValidator papv = new PostAlgoPathValidator(
                        networkTransactionService,
                        spectrumConstraint,
                        clientInput);
                papv.setPceOperMode(pceOperMode);
                pceResult = papv.checkPath(ModifiedGraphPath.fromGraphPath(path),
                        allPceNodes, allPceLinks, pceResult, pceHardConstraints, serviceType, pceConstraintMode,
                        false, null, null, 0);
                this.margin = papv.getTpceCalculatedMargin();
                this.aendOperationalMode = papv.getAendOperationalMode();
                this.zendOperationalMode = papv.getZendOperationalMode();
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
                switch (serviceLayer) {

                    case PceSendingPceRPCs.SERVICE_LAYER_PHOTONIC:
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
            } else if (!isPathHybrid(path, allPceNodes) && isSecondStepHybridPC) {
                // This is the path computation process going through the TAPI PCE in the second step of cross-domain
                // service creation where TAPI PCE is used to compute impairments of the path between 2 end-points of
                // the TAPI-SBI-ABS-NODE relying on detailed TAPI topology.
                // In this case, as one or the 2 transponders maybe outside the TAPI-Domain, it might not be possible to
                // compute margin (only accumulated impairments).
                PostAlgoPathValidator papv = new PostAlgoPathValidator(
                        networkTransactionService,
                        spectrumConstraint,
                        clientInput);
                papv.setPceOperMode(pceOperMode);
                PceCrossDomainPathAggregator pcdpa = PceCrossDomainPathAggregator.getInstance();
                pceResult = papv.checkPath(ModifiedGraphPath.fromGraphPath(path),
                    allPceNodes, allPceLinks, pceResult, pceHardConstraints, serviceType, pceConstraintMode,
                    true, pcdpa.getPrecedingEdge(pathOrderInHybidPathComputation, npathorder),
                    pcdpa.getSucceedingEdge(pathOrderInHybidPathComputation, npathorder), 0);
                this.tapiAtoZSubPathImpairments.put(pathOrderInHybidPathComputation, papv.getAtoZSubPathImpairments());
                this.tapiZtoASubPathImpairments.put(pathOrderInHybidPathComputation, papv.getZtoASubPathImpairments());
                pcdpa.setTapiAtoZSubPathImpairments(tapiAtoZSubPathImpairments);
                pcdpa.setTapiZtoASubPathImpairments(tapiZtoASubPathImpairments);
                boolean successfulE2EPathComputation = pcdpa.checkE2EpathImpairments(pathOrderInHybidPathComputation);
                if (successfulE2EPathComputation) {
                    pcdpa.buildE2Epath(pathOrderInHybidPathComputation);
                } else {
                    // Tapi portion of the path not validated, goes to next iteration of the Graph
                    continue;
                }
                // Path has been validated build pathAtoZ
                pathAtoZ.clear();
                for (PceGraphEdge edge : path.getEdgeList()) {
                    pathAtoZ.add(edge.link());
                // TODO: complete implementation of this use case
                }
                shortestPathAtoZ = new ArrayList<>(pathAtoZ);
                break;
            } else {
                 // This is the path computation process for cross-domain-service creation
                // TODO: Provide implementation when a TAPI-SBI-ABS-NODE was detected on the path
                // Calls splitPath that for the kpathorder path is going to split the path into several sub-path,
                // each of them corresponding to a specific domain.
                // PathElementList is built if the path is hybrid (isPathHybrid), meaning TAPI-SBI-ABS-NODE is part
                // of the path; and includes information (high/low Index) on the boundaries of the sub-paths as well
                // as its corresponding domain. SubGraphPathMap Map<kpathorder, List<SubGraphPath>> is updated through
                // this call to contain through its list of SubGraphPath, notably, a Map<VerticeNames, PceGraphEdge>
                // for each of the domains.
                LOG.info("Detected TAPI-SBI-ABS-NODE in the path, entering splitting process of PathK {} ",
                    entry.getKey());
                PostAlgoPathValidator papv = new PostAlgoPathValidator(
                        networkTransactionService,
                        spectrumConstraint,
                        clientInput);
                papv.setPceOperMode(pceOperMode);
                PceCrossDomainPathAggregator pcdpa = PceCrossDomainPathAggregator.getInstance();
                pcdpa.resetInstance();
                //pathElementList has been filled calling isPathHybrid
                pcdpa.setPathElementList(pathElementList);
                // splitPath populates SubGraphPathMap
                splitPath(kpathorder, pathElementList, path, subGraphPathMap);

                int subPathPointer = 0;
                for (PathElement pathelement : pathElementList) {
                    pcdpa.addSubGraphPathToMap(
                        kpathorder, subPathPointer, subGraphPathMap.get(kpathorder).get(subPathPointer));
                    if (pathelement.domainType.equals(DomainTypeEnum.Openroadm)) {
                        LOG.debug("ScanPath elt number {} of OR type", subPathPointer);

                        ModifiedGraphPath subpath = (ModifiedGraphPath) subGraphPathMap
                            .get(kpathorder).get(subPathPointer).intermediateGraphPath();

                        papv.checkPath(
                                subpath,
                                allPceNodes, allPceLinks, pceResult, pceHardConstraints, serviceType,
                                pceConstraintMode, true,
                                pcdpa.getPrecedingEdge(kpathorder, subPathPointer),
                                pcdpa.getSucceedingEdge(kpathorder, subPathPointer),
                                subPathPointer);
                        this.openRoadmAtoZSubPathImpairments.put(kpathorder, papv.getAtoZSubPathImpairments());
                        this.openRoadmZtoASubPathImpairments.put(kpathorder, papv.getZtoASubPathImpairments());
                        pcdpa.setOpenRoadmAtoZSubPathImpairments(openRoadmAtoZSubPathImpairments);
                        pcdpa.setOpenRoadmZtoASubPathImpairments(openRoadmZtoASubPathImpairments);


                    } else {
                        LOG.debug("ScanPath elt number {} of non OR type", subPathPointer);
                        // do nothing (need to fill the cross-domain service completely before we call the TAPI PCE)
                        //except increasing NumberOfOccurence of TAPI-SBI-ABS-NODE
                    }
                    subPathPointer ++;
                }
                // do a fist check to see if degradations on dif domains do not exceed RX OSNR(MARGIN calculation)
                pcdpa.pruneOpenROADMimpairments();
                // Try to aggregate calculation
                // Envisage a rationalization looking an start and end tp on TAPI-SBI ABS node to minimize the number
                // of call of TAPI PCE
                pcdpa.pruneTapiSbiABSpath();
                // If NumberOfOccurence > 1 -> continue (next Graph)
                // else -> what follows
                LOG.debug("TAPI-SBI-ABS-NODE identified as node at position {} in the calculated path",
                    this.tapiSbiAbsNodeOrderInPath);
                for (PathElement pathelement : pathElementList) {

                    if (pathelement.domainType.equals(DomainTypeEnum.TapiSbi)) {
                        LOG.info("Calculated path includes TAPI-Domain");
                     // TODO: provide implementation of this use case
                        // Build tapi-sbi PCRI
                        // new PceSendingRPC (PceOperationalMode = TAPI)
                        // Trigger Path computation through TAPI-PCE
                        // Temporized until we get the result of TAPI PCE Path computation
                        // If negative result : continue (next Graph)
                        // if positive result, call new function that creates LOG
                        // Launch rendering of optical tunnel
                        // on positive results compute PCeREsult
                    }
                }
             // Fill this.CrossDomainServiceBldr from output parameters of PapV

                // Compute PceResult
            }
            kpathorder++;
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

        PceNode source = allPceNodes.get(new NodeId(pcelink.getSourceId()));
        PceNode dest = allPceNodes.get(new NodeId(pcelink.getDestId()));

        if (source == null) {
            LOG.error("In addLinkToGraph link source node is null : {}", pcelink);
            return false;
        }
        if (dest == null) {
            LOG.error("In addLinkToGraph link dest node is null : {}", pcelink);
            return false;
        }
        LOG.info("In addLinkToGraphLine 237 validated link between nodes : {} & {} of type {} and Uuid {}",
            source.getNodeId(), dest.getNodeId(), pcelink.getlinkType(), pcelink.getLinkId());
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

                weightedGraph.addEdge(link.getSourceId(), link.getDestId(), graphLink);

                weightedGraph.setEdgeWeight(graphLink, chooseWeight(link));
                LOG.info("In Graph populateWithLinks added Edge :  {}, source {}, dest {}", link.getLinkId(),
                    link.getSourceId(), link.getDestId());
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

    private boolean isPathHybrid(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodesList) {
        List<String> vertices = path.getVertexList();
        List<PceGraphEdge> edges = path.getEdgeList();
        // LOOP that scans the different Nodes/Links of the path, checks whether the path includes TAPI-SBI-ABS-NODE
        // or not. If this the case,...

        int cdServiceOrder = 0;
        int lowIndex = 0;

        PceNode firstNode = allPceNodesList.get(new NodeId(vertices.get(0)));
        String firstTpId = edges.get(0).link().getClientA();
        CdServicesBuilder cdServiceBldr = new CdServicesBuilder();
        for (int pathElement = 0; pathElement < vertices.size(); pathElement++) {
            PceNode currentNode = allPceNodesList.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = null;
            if (pathElement != vertices.size() - 1) {
                nextNode = allPceNodesList.get(new NodeId(vertices.get(pathElement + 1)));
            }
            String destTpId = "";
            String srcTpId = "";
            if ((pathElement != vertices.size() - 1) && nextNode.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE")) {
                SourceBuilder sourceBldr = new SourceBuilder()
                    .setSrcNodeId(firstNode.getNodeId().getValue())
                    .setSrcTpId(firstTpId);
                DestinationBuilder destBldr = new DestinationBuilder()
                    .setDestNodeId(currentNode.getNodeId().getValue())
                    .setDestTpId(edges.get(pathElement).link().getSourceTP());
                cdServiceBldr.setCdServiceId(Uint8.valueOf(cdServiceOrder))
                    .setSource(sourceBldr.build())
                    .setDestination(destBldr.build());
                this.crossDomainServiceBldr.put(cdServiceOrder, cdServiceBldr);
                this.pathElementList.add(new PathElement(lowIndex, pathElement, DomainTypeEnum.Openroadm,
                    cdServiceOrder));
                cdServiceBldr = new CdServicesBuilder();
                cdServiceOrder++;
                lowIndex = pathElement + 1;
            } else if (currentNode.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE")) {
                if (pathElement == 0) {
                    //First node is TAPI-SBI-ABS-NODE & the Source TpId is retrieved from first link ClientA
                    srcTpId = edges.get(pathElement).link().getClientA();
                    destTpId = edges.get(pathElement).link().getSourceTP();
                } else if (pathElement == vertices.size() - 1) {
                  //Last node is TAPI-SBI-ABS-NODE & the Destination TpId is retrieved from from first link ClientZ
                    destTpId = edges.get(pathElement - 1).link().getClientZ();
                    srcTpId = edges.get(pathElement - 1).link().getDestTP();
                } else {
                    destTpId = edges.get(pathElement).link().getSourceTP();
                    srcTpId = edges.get(pathElement - 1).link().getDestTP();
                }
                cdServiceBldr = retrieveSBINodeParams(srcTpId, destTpId, cdServiceOrder, serviceLayer);
                this.crossDomainServiceBldr.put(cdServiceOrder, cdServiceBldr);
                this.pathElementList.add(
                    new PathElement(lowIndex, pathElement, DomainTypeEnum.TapiSbi, cdServiceOrder));
                cdServiceOrder++;
                lowIndex = pathElement + 1;
                this.tapiSbiAbsNodeOrderInPath = pathElement;
                firstNode = nextNode;
                firstTpId = edges.get(pathElement).link().getDestTP();
            } else if (cdServiceOrder > 0 && pathElement == vertices.size() - 1) {
                // This is the case of last node on the path, when TAPI-SBI-ABS-NODE is present on the path
                SourceBuilder sourceBldr = new SourceBuilder()
                    .setSrcNodeId(firstNode.getNodeId().getValue())
                    .setSrcTpId(firstTpId);
                DestinationBuilder destBldr = new DestinationBuilder()
                    .setDestNodeId(currentNode.getNodeId().getValue())
                    .setDestTpId(edges.get(pathElement - 1).link().getClientZ());
                cdServiceBldr.setCdServiceId(Uint8.valueOf(cdServiceOrder))
                    .setSource(sourceBldr.build())
                    .setDestination(destBldr.build());
                this.crossDomainServiceBldr.put(cdServiceOrder, cdServiceBldr);
                this.pathElementList.add(new PathElement(lowIndex, pathElement, DomainTypeEnum.Openroadm,
                    cdServiceOrder));
            }
        }
        if (cdServiceOrder > 0) {
            return true;
        }
        return false;
    }

    private void splitPath(Integer korder, List<PathElement> pathEltList,
        GraphPath<String, PceGraphEdge> path, Map<Integer, List<SubGraphPath>> subGraphPathMap) {
        // Add to the subGraphMap, for the Korder PATH a new list of SubGraphPath that results from the decomposition of
        // the End to End graphPath from A to Z.
        // The record SubGraphPath include preceding and following node (if relevant), so that we can get information
        // on the edges that are required to compute impairment parameters.
        Map<String, PceGraphEdge> splitPathMap = new HashMap<>();
        List<String> vertices = path.getVertexList();
        List<PceGraphEdge> edges = path.getEdgeList();
        Integer edgeListSize = path.getEdgeList().size();
        Integer verticeListSize = path.getVertexList().size();
        Integer norder = 0;
        List<SubGraphPath> subGraphPathList = new ArrayList<>();
        // LOOP that scans the different elements of the path and stores in SplitPathMap the partial paths.
        for (PathElement pathElt : pathEltList) {
            int lowIndex = pathElt.lowIndex;
            int highIndex = pathElt.highIndex;
            //SubGraphPath subGraphPath = new SubGraphPath();
            for (int pathIndex = lowIndex; pathIndex < highIndex + 1; pathIndex++) {
                splitPathMap.put(vertices.get(pathIndex),
                    pathIndex < edgeListSize ? edges.get(pathIndex) : null);
            }
            ModifiedGraphPath splitMdgp =  new ModifiedGraphPath(splitPathMap);
            String precedingNodeId = lowIndex == 0 ? null : vertices.get(lowIndex - 1);
            String followingNodeId = highIndex < (verticeListSize - 1) ? null : vertices.get(highIndex + 1);
            PceGraphEdge precedingEdge = lowIndex == 0 ? null : edges.get(lowIndex - 1);
            PceGraphEdge followingEdge = highIndex < (edgeListSize - 1) ? null : edges.get(highIndex + 1);
            SubGraphPath subGraphPath = new SubGraphPath(norder,pathElt.domainType,
                pathElt.cdServiceOrder, splitMdgp, precedingNodeId, followingNodeId,
                precedingEdge, followingEdge);
            subGraphPathList.add(subGraphPath);
            norder++;
        }
        subGraphPathMap.put(korder, subGraphPathList);

    }

    private CdServicesBuilder retrieveSBINodeParams(String srcTpId, String dstTpId,
            int bdServiceOrder, String servLayer) {

        TerminationPoint srcTp = getTpFromId(servLayer, srcTpId);
        SourceBuilder sourceBldr = new SourceBuilder();
        if (srcTp.augmentation(TerminationPoint1.class) != null) {
            sourceBldr
                .setSrcSupNodeUuid(srcTp.augmentation(TerminationPoint1.class).getSupportingNodeUuid())
                .setSrcTopoUuid(srcTp.augmentation(TerminationPoint1.class).getSupportingNodeTopologyUuid())
                .setSrcTpUuid(srcTp.augmentation(TerminationPoint1.class).getTpUuid());
        }
        if (srcTp.augmentation(TerminationPoint2.class) != null) {
            sourceBldr.setSrcSupportingNodeName(srcTp.augmentation(TerminationPoint2.class).getSupportingNodeName());
        }

        TerminationPoint dstTp = getTpFromId(servLayer, dstTpId);
        DestinationBuilder destBldr = new DestinationBuilder();
        if (dstTp.augmentation(TerminationPoint1.class) != null) {
            destBldr
                .setDestSupNodeUuid(srcTp.augmentation(TerminationPoint1.class).getSupportingNodeUuid())
                .setDestTopoUuid(srcTp.augmentation(TerminationPoint1.class).getSupportingNodeTopologyUuid())
                .setDestTpUuid(srcTp.augmentation(TerminationPoint1.class).getTpUuid());
        }
        if (dstTp.augmentation(TerminationPoint2.class) != null) {
            destBldr.setDestSupportingNodeName(srcTp.augmentation(TerminationPoint2.class).getSupportingNodeName());
        }

        return
            new CdServicesBuilder()
                .setCdServiceId(Uint8.valueOf(bdServiceOrder))
                .setDestination(destBldr.build())
                .setSource(sourceBldr.build());
    }

    private TerminationPoint getTpFromId(String servLayer, String tpId) {
        TerminationPoint tp = null;
        DataObjectIdentifier<TerminationPoint> tpIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(
                PceSendingPceRPCs.SERVICE_LAYER_PHOTONIC.equals(servLayer)
                    ? StringConstants.OPENROADM_TOPOLOGY
                    : StringConstants.OTN_NETWORK)))
            .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
            .augmentation(Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
            .build();
        try {
            tp = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, tpIID).get().orElseThrow();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error when trying to read TP : {} of TAPI-SBI-ABS-NODE", tpId, e);
        }
        return tp;
    }

    public record PathElement(int lowIndex, int highIndex, DomainTypeEnum domainType, int cdServiceOrder) {}

    public record SubGraphPath(
        int norder,
        DomainTypeEnum domainType,
        int cdServiceOrder,
        ModifiedGraphPath intermediateGraphPath,
        String precedingNode,
        String succeedingNode,
        PceGraphEdge precedingEdge,
        PceGraphEdge succeedingEdge) {
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

    public String getAendOperationalMode() {
        return aendOperationalMode;
    }

    public String getZendOperationalMode() {
        return zendOperationalMode;
    }

    public boolean get2ndStepHybrid() {
        return isSecondStepHybridPC;
    }

    public void setConstrains(PceConstraints pceHardConstraintsInput) {
        this.pceHardConstraints = pceHardConstraintsInput;
    }

    public void setPceOperMode(String pceOperationalMode) {
        this.pceOperMode = pceOperationalMode;
    }

    public void setPathOrderInHybridPath(int kpathorder) {
        this.pathOrderInHybidPathComputation = kpathorder;
    }

}
