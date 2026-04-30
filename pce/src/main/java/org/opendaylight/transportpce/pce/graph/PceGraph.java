/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.math.RoundingMode;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.DomainTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.broken.down.service.attributes.BdServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.broken.down.service.attributes.bd.services.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.broken.down.service.attributes.bd.services.ImpairmentParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.broken.down.service.attributes.bd.services.SourceBuilder;
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
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
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
    private Map<Integer, PathElement> pathElementMap = new HashMap<>();
    private Map<Integer, BdServicesBuilder> brokenDownServiceBldr = new HashMap<>();

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
            BitSet spectrumConstraint, ClientInput clientInput, String serviceLayer) {
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
            if (!isPathHybrid(path, allPceNodes)) {
                PostAlgoPathValidator papv = new PostAlgoPathValidator(
                        networkTransactionService,
                        spectrumConstraint,
                        clientInput);
                papv.setPceOperMode(pceOperMode);
                pceResult = papv.checkPath(
                        path, allPceNodes, allPceLinks, pceResult, pceHardConstraints, serviceType, pceConstraintMode);
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
            } else {
                // TODO: Provide implementation when a TAPI-SBI-ABS-NODE was detected on the path
                for (Entry<Integer, PathElement> pathelement : pathElementMap.entrySet()) {
                    int loopIter = 0;
                    if (pathelement.getValue().domainType.equals(DomainTypeEnum.Openroadm)) {
                        LOG.debug("ScanPath elt number {} of OR type", loopIter);
                        // Compute extract of Path from lower to higher Boundary
                        // Call modified PAPV :  not proving margins but Absolute value
                        // Fill this.brokenDownServiceBldr form output parameters of PapV
                        populateBDServBlderWithOpticalParams(0, null, null, Uint32.valueOf(0), Uint32.valueOf(0),
                            Decimal64.valueOf(0.0, RoundingMode.UP), null);
                    } else {
                        LOG.debug("ScanPath elt number {} of non OR type", loopIter);
                        // do nothing (need to fill the brokendown service completely before we call the TAPI PCE
                        //except increasing NumberOfOccurence of TAPI-SBI-ABS-NODE
                    }
                    loopIter ++;
                }
                // do a fist check to see if degradations on dif domains do not exceed RX OSNR (MARGIN calculation)
                // If it is the case -> continue (next Graph)
                // If NumberOfOccurence > 1 -> continue (next Graph)
                // else -> what follows
                LOG.debug("TAPI-SBI-ABS-NODE identified as node at position {} in the calculated path",
                    this.tapiSbiAbsNodeOrderInPath);
                for (Entry<Integer, PathElement> pathelement : pathElementMap.entrySet()) {

                    if (pathelement.getValue().domainType.equals(DomainTypeEnum.TapiSbi)) {
                        LOG.info("Calculated path includes TAPI-Domain");
                        // Build tapi-sbi PCRI
                        // new PceSendingRPC (PceOperationalMode = TAPI)
                        // Trigger Path computation through TAPI-PCE
                        // Temporized until we get the result of TAPI PCE Path computation
                        // If negative result : continue (next Graph)
                        // if positive result, call new function that creates LOG
                        // Launch rendering of optical tunnel
                        // on positive results Service-implementation request
                    }
                }
            }
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
                LOG.info("In Graph populateWithLinks added Edge :  {}", link.getLinkId());
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

        int bdServiceOrder = 0;
        int lowIndex = 0;

        PceNode firstNode = allPceNodesList.get(new NodeId(vertices.get(0)));
        String firstTpId = edges.get(0).link().getClientA();
        BdServicesBuilder bdServiceBldr = new BdServicesBuilder();
        for (int pathElement = 0; pathElement < vertices.size(); pathElement++) {
            PceNode currentNode = allPceNodesList.get(new NodeId(vertices.get(pathElement)));
            PceNode nextNode = allPceNodesList.get(new NodeId(vertices.get(pathElement + 1)));
            String destTpId = "";
            String srcTpId = "";
            if (nextNode.getNodeId().getValue().equals("TAPI-SBI-ABS-NODE")) {
                SourceBuilder sourceBldr = new SourceBuilder()
                    .setSrcNodeId(firstNode.getNodeId().getValue())
                    .setSrcTpId(firstTpId);
                DestinationBuilder destBldr = new DestinationBuilder()
                    .setDestNodeId(currentNode.getNodeId().getValue())
                    .setDestTpId(edges.get(pathElement).link().getSourceTP());
                bdServiceBldr.setBdServiceId(Uint8.valueOf(bdServiceOrder))
                    .setSource(sourceBldr.build())
                    .setDestination(destBldr.build());
                this.brokenDownServiceBldr.put(bdServiceOrder, bdServiceBldr);
                this.pathElementMap.put(lowIndex, new PathElement(pathElement, DomainTypeEnum.Openroadm,
                    bdServiceOrder));
                bdServiceBldr = new BdServicesBuilder();
                bdServiceOrder++;
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
                bdServiceBldr = retrieveSBINodeParams(srcTpId, destTpId, bdServiceOrder, serviceLayer);
                this.brokenDownServiceBldr.put(bdServiceOrder, bdServiceBldr);
                this.pathElementMap.put(lowIndex, new PathElement(pathElement, DomainTypeEnum.TapiSbi, bdServiceOrder));
                bdServiceOrder++;
                lowIndex = pathElement + 1;
                this.tapiSbiAbsNodeOrderInPath = pathElement;
                firstNode = nextNode;
                firstTpId = edges.get(pathElement).link().getDestTP();
            } else if (bdServiceOrder > 0 && pathElement == vertices.size() - 1) {
                // This is the case of last node on the path, when TAPI-SBI-ABS-NODE is present on the path
                SourceBuilder sourceBldr = new SourceBuilder()
                    .setSrcNodeId(firstNode.getNodeId().getValue())
                    .setSrcTpId(firstTpId);
                DestinationBuilder destBldr = new DestinationBuilder()
                    .setDestNodeId(currentNode.getNodeId().getValue())
                    .setDestTpId(edges.get(pathElement - 1).link().getClientZ());
                bdServiceBldr.setBdServiceId(Uint8.valueOf(bdServiceOrder))
                    .setSource(sourceBldr.build())
                    .setDestination(destBldr.build());
                this.brokenDownServiceBldr.put(bdServiceOrder, bdServiceBldr);
                this.pathElementMap.put(lowIndex, new PathElement(pathElement, DomainTypeEnum.Openroadm,
                    bdServiceOrder));
            }
        }
        if (bdServiceOrder > 0) {
            return true;
        }
        return false;
    }

    private void populateBDServBlderWithOpticalParams(int bdServiceOrder, RatioDB osnrContrib, RatioDB targetRxOSNR,
            Uint32 cdContrib, Uint32 pmd2Contrib, Decimal64 latency, byte[] freqOccupation) {
        ImpairmentParametersBuilder impairments = new ImpairmentParametersBuilder()
            .setAccumulatedCd(cdContrib)
            .setAccumulatedPmd2(pmd2Contrib)
            .setLatency(latency)
            .setOsnrContribution(osnrContrib)
            .setSectionAvailableFreqMap(freqOccupation)
            .setTargetRxOsnr(targetRxOSNR);
        this.brokenDownServiceBldr.get(bdServiceOrder)
            .setCalculatedImpairments(true)
            .setImpairmentParameters(impairments.build());
    }

    private BdServicesBuilder retrieveSBINodeParams(String srcTpId, String dstTpId,
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
            new BdServicesBuilder()
                .setBdServiceId(Uint8.valueOf(bdServiceOrder))
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

    private record PathElement(int highIndex, DomainTypeEnum domainType, int bdServiceOrder) {}

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

    public void setConstrains(PceConstraints pceHardConstraintsInput) {
        this.pceHardConstraints = pceHardConstraintsInput;
    }

    public void setPceOperMode(String pceOperationalMode) {
        this.pceOperMode = pceOperationalMode;
    }
}
