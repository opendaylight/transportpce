/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.orchestrator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.pce.graph.PceGraph.PathElement;
import org.opendaylight.transportpce.pce.graph.PceGraph.SubGraphPath;
import org.opendaylight.transportpce.pce.graph.PceGraphEdge;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.networkanalyzer.PceTapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev250902.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.AggregatedPathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.OlsServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.aggregated.path.description.CdServicePathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.aggregated.path.description.CdServicePathDescriptionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.DomainTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.CdServices;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.CdServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.CdServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.ImpairmentParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.AToZImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.ZToAImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Singleton class responsible for aggregating cross-domain path computationsresults.
* Results include impairments and path descriptions stored in different maps for OpenROADM and TAPI domains.
*/
public final class PceCrossDomainPathAggregator {

    private enum Direction {
        ATOZ, ZTOA;
    }

    private enum ImpairementType {
        OR, Total;
    }

    private static PceCrossDomainPathAggregator instance;
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCrossDomainPathAggregator.class);
    private Map<Integer, List<PathElement>> pathElementMap = new HashMap<>();
    // Storage of impairment results (OpenROADM domains) for each KorderPath (FirstKey)
    // and each domains (2NdKey is domain order).
    private Map<Integer, Map<Integer, AToZImpairmentsBuilder>> openRoadmAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> openRoadmZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, AToZImpairmentsBuilder>> tapiAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> tapiZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, SubGraphPath>> subGraphPathMap = new HashMap<>();
    private List<Integer> prunedKorderList = new ArrayList<>();
    private AggregatedPathDescriptionBuilder aggPathDescription = new AggregatedPathDescriptionBuilder();
    private PathComputationRequestInput pcri = null;
    private OlsServiceCreateInputBuilder olssciBldr = null;
    private int kpathorderMax = 0;

    private boolean isFirstStepHybrid = false;
    private boolean isSecondStepHybrid = false;
    private boolean isSecondStepSuccessfullyFinished = false;
    private boolean isSecondStepAborted = false;

    private static final String SUFFIXSERVICENAME = "-SUBSERVICE-";

    private PceCrossDomainPathAggregator() {

    }

    /**
    * Retrieves the singleton instance of the aggregator.
    *
    * @return the singleton instance of {@link PceCrossDomainPathAggregator}
    */
    public static synchronized PceCrossDomainPathAggregator getInstance() {
        if (instance == null) {
            instance = new PceCrossDomainPathAggregator();
        }
        return instance;
    }

    // Checks that impairments calculated on OpenROADM sub-path do not exceed what the device performance allow.
    // If this is the case remove from all subPathImpairment maps the korder path
    // Allows to simplify path computation in TAPI domain to avoid computing unnecessary path in TAPI domains
    /**
    * Prunes impairments in open ROADM domains based on device performance constraints.
    *   - Checks that impairments calculated on OpenROADM sub-path do not exceed what the device performance allow.
    *   - If this is the case remove from all subPathImpairment maps the korder path.
    * Allows to simplify path computation in TAPI domain to avoid computing unnecessary path in TAPI domains.
    *
    * @param cu the catalog utility used to retrieve devices' performances and to check impairment.
    */
    public void pruneOpenROADMimpairments(CatalogUtils cu) {
        // Calls checkImpairments to cleanup AtoZSubImpaiment & Subgraph  maps as well as pathElementList
        List<Integer> intPrunedKorderList = new ArrayList<>();
        for (int korder = 0; korder < openRoadmAtoZSubPathImpairments.size(); korder ++) {
            double margin1 = checkImpairments(korder, cu,
                PceCrossDomainPathAggregator.Direction.ATOZ, PceCrossDomainPathAggregator.ImpairementType.OR);
            double margin2 = checkImpairments(korder, cu,
                PceCrossDomainPathAggregator.Direction.ZTOA, PceCrossDomainPathAggregator.ImpairementType.OR);
            if (margin1 >= 0 && margin2 >= 0) {
                //Path is valid, and shall not be prune
                intPrunedKorderList.add(korder);
            } else {
              //Path is not valid, and shall be pruned, do nothing
            }
            korder++;
        }
        if (prunedKorderList.size() == openRoadmAtoZSubPathImpairments.size()) {
            return;
        }
        LOG.info("In PCDPA {} paths corresponding to Korder of the list {} have been validated",
            prunedKorderList.size(), prunedKorderList);
        openRoadmAtoZSubPathImpairments.entrySet().stream().filter(Imp -> intPrunedKorderList.contains(Imp.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        openRoadmZtoASubPathImpairments.entrySet().stream().filter(Imp -> intPrunedKorderList.contains(Imp.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tapiAtoZSubPathImpairments.entrySet().stream().filter(Imp -> intPrunedKorderList.contains(Imp.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tapiZtoASubPathImpairments.entrySet().stream().filter(Imp -> intPrunedKorderList.contains(Imp.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        pathElementMap.entrySet().stream().filter(Imp -> intPrunedKorderList.contains(Imp.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        kpathorderMax = openRoadmAtoZSubPathImpairments.size();
        prunedKorderList.clear();
        prunedKorderList.addAll(intPrunedKorderList);
    }

    /**
    * Checks the end-to-end path impairments for a given path order.
    *
    * @param korder the path order index
    * @param cu the catalog utility for impairment calculations
    * @return the result of the impairment check encapsulated in {@link PceResult}
    */
    public PceResult checkE2EpathImpairments(int korder, CatalogUtils cu) {
        // When a path has been analyzed through checkPath in the TAPI PCE, we need to check E2E path consistency
        // and if the path is consistent return true; otherwise false, which makes Graph.calcPath iterate through next
        //path. Korder is used to retrieve the corresponding OpenROADM impairments in Maps.
        PceResult pceResult = new PceResult();
        double margin = calculateE2Emargin(korder, cu);
        if (margin >= 0) {
            LOG.info("Path found spanning accross TapiDomain for path of Korder = {}", korder);
            pceResult.success();
            return pceResult;
        }
        pceResult.error("No path found spanning accross TapiDomain for path of Korder = " + korder);
        pceResult.setLocalCause(PceResult.LocalCause.NO_PATH_EXISTS);
        return pceResult;
    }

    /**
    * Retrieves the description of the open ROADM path for a given path order.
    *
    * @param korder the path order index
    * @return a map of domain identifiers (N) to their corresponding list of Pcelinks
    */
    public Map<Integer, List<PceLink>> getOpenRoadmPathDescription(int korder) {
        if (subGraphPathMap.get(korder) == null) {
            LOG.info("Error pruning SubgraphPathMap in PCDPA : SubgraphPathMap({}) is null", korder);
            return new HashMap<>();
        }
        Map<Integer, SubGraphPath> openRoadmSubgraphPathMap = subGraphPathMap.get(korder).entrySet()
            .stream().filter(entry -> entry.getValue().domainType().equals(DomainTypeEnum.Openroadm))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Integer, List<PceLink>> mapOfAtoZpath = new HashMap<>();
        for (Map.Entry<Integer, SubGraphPath> entryN : openRoadmSubgraphPathMap.entrySet()) {
            mapOfAtoZpath.put(
                entryN.getKey(),
                entryN.getValue().edgeList().stream().map(PceGraphEdge::link).toList());
        }
        LOG.info("IN PCDPA found A to Z OpenROADM Sub-Path {}", mapOfAtoZpath);
        return mapOfAtoZpath;
    }

    /**
    * Retrieves the description of the TAPI path for a given path order.
    *
    * @param korder the path order index
    * @return a map of domain identifiers (N)to their corresponding list of links, or null if multiple TAPI subdomains
    *         are detected : only one TAPI domain supported!
    */
    public Map<Integer, List<PceLink>> getTapiPathDescription(int korder) {
        if (subGraphPathMap.get(korder) == null) {
            LOG.info("Error pruning SubgraphPathMap in PCDPA : SubgraphPathMap({}) is null", korder);
            return new HashMap<>();
        }
        Map<Integer, SubGraphPath> tapiSubgraphPathMap = subGraphPathMap.get(korder).entrySet()
            .stream().filter(entry -> entry.getValue().domainType().equals(DomainTypeEnum.TapiSbi))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (tapiSubgraphPathMap.size() > 1) {
            LOG.error("Detection of Different TAPI subdomains interconnected: unsupported Feature");
            return null;
        }
        Map<Integer, List<PceLink>> mapOfAtoZpath = new HashMap<>();
        for (Map.Entry<Integer, SubGraphPath> entryN : tapiSubgraphPathMap.entrySet()) {
            mapOfAtoZpath.put(
                entryN.getKey(),
                entryN.getValue().edgeList().stream().map(PceGraphEdge::link).toList());
        }
        LOG.info("IN PCDPA found A to Z Tapi Sub-Path {}", mapOfAtoZpath);
        return mapOfAtoZpath;
    }

    /**
    * Builds cross-domain service descriptions for a given path order.
    *
    * @param korder the path order index
    * @return a map of service keys to cross-domain service descriptions
    */
    public Map<CdServicesKey, CdServices> buildCdService(int korder) {
        Map<CdServicesKey, CdServices> cdServiceMap = new HashMap<>();
        if (subGraphPathMap.get(korder) == null) {
            LOG.info("Error building CrossDomainService in PCDPA : SubgraphPathMap({}) is null", korder);
            return cdServiceMap;
        }
        for (Map.Entry<Integer, SubGraphPath> entryN : subGraphPathMap.get(korder).entrySet()) {
            if (entryN == null || entryN.getValue() == null) {
                continue;
            }
            DomainTypeEnum domaintype;
            AToZImpairmentsBuilder atozImpBldr = openRoadmAtoZSubPathImpairments.getOrDefault(korder, new HashMap<>())
                .get(entryN.getKey());
            ZToAImpairmentsBuilder ztoaImpBldr = openRoadmZtoASubPathImpairments.getOrDefault(korder, new HashMap<>())
                .get(entryN.getKey());
            SourceBuilder srcBldr = new SourceBuilder();
            DestinationBuilder dstBldr = new DestinationBuilder();
            if (atozImpBldr != null && ztoaImpBldr != null) {
                domaintype = DomainTypeEnum.Openroadm;
                srcBldr.setSrcNodeId(entryN.getValue().intermediateGraphPath().getStartVertex());
                srcBldr.setSrcTpId(entryN.getValue().intermediateGraphPath().getEdgeList().iterator().next()
                    .link().getSourceTP());
                dstBldr.setDestNodeId(entryN.getValue().intermediateGraphPath().getEndVertex());
                dstBldr.setDestTpId(entryN.getValue().intermediateGraphPath().getEdgeList().get(
                    entryN.getValue().intermediateGraphPath().getEdgeList().size() - 1).link().getDestTP());
            } else {
                if (tapiAtoZSubPathImpairments.getOrDefault(korder, new HashMap<>()).get(entryN.getKey()) == null
                            || tapiAtoZSubPathImpairments.getOrDefault(korder, new HashMap<>())
                                .get(entryN.getKey()) == null) {
                    return null;
                }
                if (tapiAtoZSubPathImpairments.get(korder).get(entryN.getKey()) == null) {
                    return null;
                }
                atozImpBldr = tapiAtoZSubPathImpairments.getOrDefault(korder, new HashMap<>()).get(entryN.getKey());
                ztoaImpBldr = tapiZtoASubPathImpairments.getOrDefault(korder, new HashMap<>()).get(entryN.getKey());
                domaintype = DomainTypeEnum.TapiSbi;
                //List<PceGraphEdge> edges = entryN.getValue().edgeList();
                List<PceGraphEdge> edges = entryN.getValue().intermediateGraphPath().getEdgeList();
                if (!edges.isEmpty()) {
                    PceLink firstLink = edges.iterator().next().link();
                    srcBldr.setSrcNodeId(entryN.getValue().intermediateGraphPath().getStartVertex());
                    srcBldr.setSrcSupNodeUuid(firstLink.getsourceNetworkSupNodeId());
                    srcBldr.setSrcTopoUuid(((PceTapiLink) firstLink).getTopologyUuid().getValue());
                    srcBldr.setSrcTpUuid(firstLink.getSourceTP());
                    srcBldr.setSrcTpId(entryN.getValue().intermediateGraphPath().getEdgeList().iterator().next()
                        .link().getSourceTP());
                    dstBldr.setDestNodeId(entryN.getValue().intermediateGraphPath().getEndVertex());
                    dstBldr.setDestTpId(entryN.getValue().intermediateGraphPath().getEdgeList().get(
                        entryN.getValue().intermediateGraphPath().getEdgeList().size() - 1).link().getDestTP());
                    PceLink lastLink = edges.get(entryN.getValue().intermediateGraphPath().getEdgeList().size() - 1)
                        .link();
                    dstBldr.setDestSupNodeUuid(lastLink.getdestNetworkSupNodeId());
                    dstBldr.setDestTopoUuid(((PceTapiLink) lastLink).getTopologyUuid().getValue());
                    dstBldr.setDestTpUuid(lastLink.getDestTP());
                }
            }

            CdServicesBuilder cdsbldr = new CdServicesBuilder()
                .setCdServiceId(Uint8.valueOf(entryN.getKey()))
                .setTopologyDomain(domaintype)
                .setCalculatedImpairments(true)
                .setSource(srcBldr.build())
                .setDestination(dstBldr.build())
                .setImpairmentParameters(new ImpairmentParametersBuilder()
                    .setAToZImpairments(atozImpBldr.build())
                    .setZToAImpairments(ztoaImpBldr.build())
                    .build());
            cdServiceMap.put(new CdServicesKey(Uint8.valueOf(entryN.getKey())), cdsbldr.build());
        }
        LOG.info("CD-DEBUG : in PCDPA build Cross-domain-service Container {}", cdServiceMap);
        return cdServiceMap;
    }

    /**
    * Prunes the path element map by removing entries that are not valid after impairment pruning.
    * - Impairment pruning consist in verifying accumulated degradations on the portion of the path that crosses
    *   OpenROADM domain for all K paths, and eliminate all of the Korder paths that exceed tolerated degradations.
    * @param pathelementMap the original map of path elements
    * @return the pruned map of path elements
    */
    public Map<Integer, List<PathElement>> prunePathElementMap(Map<Integer, List<PathElement>> pathelementMap) {
        // After pruneOpenROADMimpairments has been purging from the list of Path for which a path needs to be found
        // in TAPI domain, the path that are not good candidate need to be removed from the PathElemnt Map of PceGraph
        if (pathElementMap.size() == prunedKorderList.size()) {
            return pathelementMap;
        }
        LOG.info("In PCDPA remove {} invalid path (incompatible impairments on OpenROADM domain) from PathElementMap",
            pathElementMap.size() - prunedKorderList.size());
        return pathelementMap.entrySet().stream()
                .filter(pathElt -> prunedKorderList.contains(pathElt.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
    * Builds a path computation request input for a specific path order and node order.
    * The PathComputationRequestInput is used to exercise the Path computation in the second step of Cross-domain
    * hybrid path computation : this shall trigger path computation using TAPI PCE running on detailed TAPI topology.
    * @param korder the path order index
    * @param norder the sub-path order index in the list of sub-path
    * @return the constructed {@link PathComputationRequestInput} or null if construction fails
    */
    public PathComputationRequestInput buildPathComputationRequestCreateInput(int korder, int norder) {
        if (subGraphPathMap.get(korder) == null || subGraphPathMap.get(korder).isEmpty()) {
            LOG.error("IN PCDPA, Map of SubgraphPath is null or empty");
            return null;
        }
        Map<Integer, SubGraphPath> tapiSubgraphPathMap = subGraphPathMap.get(korder).entrySet()
            .stream().filter(entry -> entry.getValue().domainType().equals(DomainTypeEnum.TapiSbi))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (tapiSubgraphPathMap.size() > 1) {
            LOG.error("IN PCDPA, Detection of Different TAPI subdomains interconnected: unsupported Feature");
            return null;
        }
        String firstTpId = "";
        String lastTpId = "";
        SubGraphPath subGraphPath = tapiSubgraphPathMap.get(0);
        if (subGraphPath == null || subGraphPath.edgeList() == null || subGraphPath.edgeList().isEmpty()) {
            LOG.error("CD-DEBUG:In PCDPA, subGraphPath doesn't include any path for TAPI domain or has empty EdgeList");
            return null;
        }
        if (subGraphPath.precedingEdge() == null) {
            firstTpId = subGraphPath.edgeList().get(0).link().getSourceTP();
        } else {
            firstTpId = subGraphPath.precedingEdge().link().getDestTP();
        }
        if (subGraphPath.succeedingEdge() == null) {
            lastTpId = subGraphPath.edgeList().get(subGraphPath.edgeList().size() - 1).link().getDestTP();
        } else {
            lastTpId = subGraphPath.succeedingEdge().link().getSourceTP();
        }
        if (firstTpId == null || lastTpId == null) {
            LOG.error("In PCDPA, fail to find TpIds of TAPI Path's ends for path {}", korder);
            return null;
        }
        TerminationPoint tp = PceCrossDomainOrchestrator.getInstance(null, null, null)
            .getTapiSbiNodeTPinORTopology(new TpId(firstTpId), StringConstants.OPENROADM_TOPOLOGY);
        if (tp == null) {
            LOG.info("Fail to find first TP {} of PceGraphEdge in Datastore, abort 2nd Step of Hybrid Path Computation",
                firstTpId);
            return null;
        }
        String nodeAid = tp.augmentation(TerminationPoint1.class).getSupportingNodeUuid();
        String intermediateNodeAid = nodeAid.substring(2, nodeAid.length());
        if (getUuidFromInput(intermediateNodeAid).getValue().equals(intermediateNodeAid)) {
            // For request exercised through Tapi, the provided NodeId in the ServiceAend is a Uuid that has been
            // modified adding "aa" at the begining to fit with OR NodeIdType pattern : will use the initial Uuid
            // as the Node Id. Otherwise use as is.
            nodeAid = intermediateNodeAid;
        }
        ServiceAEndBuilder serviceAEndBldr = new ServiceAEndBuilder(pcri.getServiceAEnd());
        serviceAEndBldr.setNodeId(nodeAid);
        serviceAEndBldr.setTxDirection(new TxDirectionBuilder().setLogicalConnectionPoint(firstTpId).build());
        serviceAEndBldr.setRxDirection(new RxDirectionBuilder().setLogicalConnectionPoint(firstTpId).build());

        tp = PceCrossDomainOrchestrator.getInstance(null, null, null)
            .getTapiSbiNodeTPinORTopology(new TpId(lastTpId), StringConstants.OPENROADM_TOPOLOGY);
        if (tp == null) {
            LOG.info("Fail to find last TP {} of PceGraphEdge in Datastore, abort 2nd Step of Hybrid Path Computation",
                lastTpId);
            return null;
        }
        String nodeZid = tp.augmentation(TerminationPoint1.class).getSupportingNodeUuid();
        String intermediateNodeZid = nodeZid.substring(2, nodeZid.length());
        if (getUuidFromInput(intermediateNodeZid).getValue().equals(intermediateNodeZid)) {
            // For request exercised through Tapi, the provided NodeId in the ServiceAend is a Uuid that has been
            // modified adding "aa" at the begining to fit with OR NodeIdType pattern : will use the initial Uuid
            // as the Node Id. Otherwise use as is.
            nodeZid = intermediateNodeZid;
        }
        ServiceZEndBuilder serviceZEndBldr = new ServiceZEndBuilder(pcri.getServiceZEnd());
        serviceZEndBldr.setNodeId(nodeZid);
        serviceZEndBldr.setTxDirection(new TxDirectionBuilder().setLogicalConnectionPoint(lastTpId).build());
        serviceZEndBldr.setRxDirection(new RxDirectionBuilder().setLogicalConnectionPoint(lastTpId).build());

        String serviceName = getUuidFromInput(
            pcri.getServiceName() + SUFFIXSERVICENAME + norder).getValue();

        PathComputationRequestInput secondStepPcri = new PathComputationRequestInputBuilder()
            .setServiceName(serviceName)
            .setResourceReserve(pcri.getResourceReserve())
            .setServiceHandlerHeader(pcri.getServiceHandlerHeader())
            // For TAPI path computation ignore Constraint in a firts step (too complex to analyze whether the
            // expressed constraints apply to OR or TAPI domain!
            .setHardConstraints(null)
            .setSoftConstraints(null)
            .setPceRoutingMetric(PceMetric.TEMetric)
            .setCustomerName(pcri.getCustomerName())
            .setServiceAEnd(serviceAEndBldr.build())
            .setServiceZEnd(serviceZEndBldr.build())
            .build();


        return secondStepPcri;
    }

    /**
    * Checks impairments for a specific path, direction, and impairment type.
    *
    * @param kpathOrder the path order index
    * @param cu the catalog utility
    * @param direction the impairment check direction (ATOZ/ZTOA)
    * @param impType the impairment type specifying whether the impairments shall be computed only over the OpenROADM
    *                (OR -> used to prune paths) or all domains (Total -> use for final E2E impirments calculation.
    * @return the impairment margin value
    */
    @SuppressWarnings("fallthrough")
    @SuppressFBWarnings(
        value = "SF_SWITCH_FALLTHROUGH",
        justification = "intentional fallthrough")
    private double checkImpairments(Integer kpathOrder, CatalogUtils cu,
            PceCrossDomainPathAggregator.Direction direction, PceCrossDomainPathAggregator.ImpairementType impType) {
        double introducedCd = 0.0;
        double introducedPdl2 = 0.0;
        double introducedPmd2 = 0.0;
        double onsrLin = 0.0;
        String operationalMode = "";

        if (direction.equals(PceCrossDomainPathAggregator.Direction.ATOZ)) {
            switch (impType) {
                case Total:
                    for (Map.Entry<Integer, AToZImpairmentsBuilder> entryN :
                        tapiAtoZSubPathImpairments.get(kpathOrder).entrySet()) {
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedCd() != null) {
                            introducedCd += entryN.getValue().getAccumulatedCd().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPdl2() != null) {
                            introducedPdl2 += entryN.getValue().getAccumulatedPdl2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPmd2() != null) {
                            introducedPmd2 += entryN.getValue().getAccumulatedPmd2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getOsnrContribution() != null) {
                            onsrLin += Math.pow(10,
                                (entryN.getValue().getOsnrContribution().getValue().doubleValue() * -1.0 / 10));
                        }
                        String opMode = entryN.getValue().getXponderOperationalMode();
                        if (opMode != null && !opMode.isBlank()) {
                            operationalMode = opMode;
                        }
                    }
                    // No brake, in case of Total impairment calculation, also go through OR after it has calculated
                    // TAPI Impairments.
                case OR:
                    for (Map.Entry<Integer, AToZImpairmentsBuilder> entryN :
                        openRoadmAtoZSubPathImpairments.get(kpathOrder).entrySet()) {
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedCd() != null) {
                            introducedCd += entryN.getValue().getAccumulatedCd().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPdl2() != null) {
                            introducedPdl2 += entryN.getValue().getAccumulatedPdl2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPmd2() != null) {
                            introducedPmd2 += entryN.getValue().getAccumulatedPmd2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getOsnrContribution() != null) {
                            onsrLin += Math.pow(10,
                                (entryN.getValue().getOsnrContribution().getValue().doubleValue() * -1.0 / 10));
                        }
                        String opMode = entryN.getValue().getXponderOperationalMode();
                        if (opMode != null && !opMode.isBlank()) {
                            operationalMode = opMode;
                        }
                    }
                    break;
                default:
                    break;
            }
        } else {
            switch (impType) {
                case Total:
                    for (Map.Entry<Integer, ZToAImpairmentsBuilder> entryN :
                        tapiZtoASubPathImpairments.get(kpathOrder).entrySet()) {
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedCd() != null) {
                            introducedCd += entryN.getValue().getAccumulatedCd().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPdl2() != null) {
                            introducedPdl2 += entryN.getValue().getAccumulatedPdl2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPmd2() != null) {
                            introducedPmd2 += entryN.getValue().getAccumulatedPmd2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getOsnrContribution() != null) {
                            onsrLin += Math.pow(10,
                                (entryN.getValue().getOsnrContribution().getValue().doubleValue() * -1.0 / 10));
                        }
                        String opMode = entryN.getValue().getXponderOperationalMode();
                        if (opMode != null && !opMode.isBlank()) {
                            operationalMode = opMode;
                        }
                    }
                    // No brake, in case of Total impairment calculation, also go through OR after it has calculated
                    // TAPI Impairments.
                case OR:
                    for (Map.Entry<Integer, ZToAImpairmentsBuilder> entryN :
                        openRoadmZtoASubPathImpairments.get(kpathOrder).entrySet()) {
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedCd() != null) {
                            introducedCd += entryN.getValue().getAccumulatedCd().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPdl2() != null) {
                            introducedPdl2 += entryN.getValue().getAccumulatedPdl2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getAccumulatedPmd2() != null) {
                            introducedPmd2 += entryN.getValue().getAccumulatedPmd2().doubleValue();
                        }
                        if (entryN.getValue() != null && entryN.getValue().getOsnrContribution() != null) {
                            onsrLin += Math.pow(10,
                                (entryN.getValue().getOsnrContribution().getValue().doubleValue() * -1.0 / 10));
                        }
                        String opMode = entryN.getValue().getXponderOperationalMode();
                        if (opMode != null && !opMode.isBlank()) {
                            operationalMode = opMode;
                        }
                    }
                default:
                    break;
            }
        }
        // All OpenROADM sub-paths have been scanned, check whether impairments already exceed tolerable level
        if (operationalMode.isBlank()) {
            return -1.0;
        }
        double calcOnsrdB = getOsnrDbfromOnsrLin(onsrLin);
        double margin = cu.getPceRxTspParameters(operationalMode, introducedCd, Math.sqrt(introducedPmd2),
                Math.sqrt(introducedPdl2), calcOnsrdB);
        return margin;
    }

    /**
    * Calculates the impairment margin for the end-to-end path.
    *
    * @param korder the path order index
    * @param cu the catalog utility
    * @return the impairment margin value (minimum between margin calculated for AtoZ and ZtoA path :
    *                                       > 0 if the path is valid, -1 if the path is not valid)
    */
    private double calculateE2Emargin(int korder, CatalogUtils cu) {
        double margin1 = checkImpairments(korder, cu,
            PceCrossDomainPathAggregator.Direction.ATOZ, PceCrossDomainPathAggregator.ImpairementType.Total);
        double margin2 = checkImpairments(korder, cu,
            PceCrossDomainPathAggregator.Direction.ZTOA, PceCrossDomainPathAggregator.ImpairementType.Total);
        if (margin1 >= 0 && margin2 >= 0) {
            return Math.min(margin1, margin2);
        } else {
            return -1.0;
        }
    }

    /**
    * Builds a OLS Service Create input Builder from the path computation request input.
    * Uses the PathComputationRequestInput is used to exercise the Path computation in the second step of Cross-domain
    * hybrid path computation.
    * @param pcrinput The PathComputationRequestInput used to exercise the Path computation in the second step of Cross-
    *             domain hybrid path computation
    * @return the constructed {@link OlsServiceCreateInputBuilder} or null if construction fails
    */
    public OlsServiceCreateInputBuilder buildOlsServiceCreateInputBldr(PathComputationRequestInput pcrinput) {
        ServiceAEnd servAend = pcrinput.getServiceAEnd();
        if (servAend == null) {
            return null;
        }
        var txdirBldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530
                .service.endpoint.TxDirectionBuilder()
            .setIndex(Uint8.ZERO)
            .setPort(servAend.getTxDirection().getPort());
        var rxdirBldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530
                .service.endpoint.RxDirectionBuilder()
            .setIndex(Uint8.ZERO)
            .setPort(servAend.getRxDirection().getPort());
        Map<TxDirectionKey,TxDirection> txMap = new HashMap<>();
        txMap.put(new TxDirectionKey(Uint8.ZERO), txdirBldr.build());
        Map<RxDirectionKey,RxDirection> rxMap = new HashMap<>();
        rxMap.put(new RxDirectionKey(Uint8.ZERO), rxdirBldr.build());

        var servAendBldr = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910
                .second.step.hybrid.pc.result.ols.service.create.input.ServiceAEndBuilder()
            .setSourceRxTpId(pcri.getServiceAEnd().getRxDirection().getLogicalConnectionPoint())
            .setSourceTxTpId(pcri.getServiceAEnd().getTxDirection().getLogicalConnectionPoint())
            .setTxDirection(txMap)
            .setRxDirection(rxMap)
            .setServiceFormat(servAend.getServiceFormat())
            .setServiceRate(servAend.getServiceRate())
            .setOduServiceRate(servAend.getOduServiceRate())
            .setOtuServiceRate(servAend.getOtuServiceRate())
            .setOtherServiceFormatAndRate(servAend.getOtherServiceFormatAndRate())
            .setClli(servAend.getClli())
            .setNodeId(new NodeIdType(servAend.getNodeId()));
        LOG.debug("PCDPA, build serviceCreateInput, serviceAend = {}", servAendBldr);

        ServiceZEnd servZend = pcrinput.getServiceZEnd();
        if (servZend == null) {
            return null;
        }
        txdirBldr
            .setIndex(Uint8.ZERO)
            .setPort(servZend.getTxDirection().getPort());
        rxdirBldr
            .setIndex(Uint8.ZERO)
            .setPort(servAend.getRxDirection().getPort());
        txMap = new HashMap<>();
        txMap.put(new TxDirectionKey(Uint8.ZERO), txdirBldr.build());
        rxMap = new HashMap<>();
        rxMap.put(new RxDirectionKey(Uint8.ZERO), rxdirBldr.build());
        var servZendBldr = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910
                .second.step.hybrid.pc.result.ols.service.create.input.ServiceZEndBuilder()
            .setSourceRxTpId(pcri.getServiceZEnd().getRxDirection().getLogicalConnectionPoint())
            .setSourceTxTpId(pcri.getServiceZEnd().getTxDirection().getLogicalConnectionPoint())
            .setTxDirection(txMap)
            .setRxDirection(rxMap)
                .setServiceFormat(servZend.getServiceFormat())
                .setServiceRate(servZend.getServiceRate())
                .setOduServiceRate(servZend.getOduServiceRate())
                .setOtuServiceRate(servZend.getOtuServiceRate())
                .setOtherServiceFormatAndRate(servZend.getOtherServiceFormatAndRate())
                .setClli(servZend.getClli())
                .setNodeId(new NodeIdType(servZend.getNodeId()))
                .setTxDirection(txMap)
                .setRxDirection(rxMap);
        LOG.debug("PCDPA, build serviceCreateInput, serviceZend = {}", servZendBldr);

        OlsServiceCreateInputBuilder olsSciBldr = new OlsServiceCreateInputBuilder()
            .setCustomer(pcrinput.getCustomerName())
            .setRoutingMetric(pcrinput.getRoutingMetric())
            .setServiceName(pcrinput.getServiceName())
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("999").build())
            .setServiceAEnd(servAendBldr.build())
            .setServiceZEnd(servZendBldr.build())
            .setHardConstraints(null);

        this.olssciBldr = olsSciBldr;
        return olsSciBldr;
    }

    public void setPcri(PathComputationRequestInput pcri) {
        this.pcri = pcri;
    }

    public void setOpenRoadmAtoZSubPathImpairments(
            Map<Integer, Map<Integer, AToZImpairmentsBuilder>> orAtoZSubPathImp) {
        this.openRoadmAtoZSubPathImpairments.putAll(orAtoZSubPathImp);
    }

    public AggregatedPathDescriptionBuilder getAggregatedPathDescriptionBldr() {
        return this.aggPathDescription;
    }

    public void setOpenRoadmZtoASubPathImpairments(
            Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> orZtoASubPathImp) {
        this.openRoadmZtoASubPathImpairments.putAll(orZtoASubPathImp);
    }

    public void setTapiAtoZSubPathImpairments(
            Map<Integer, Map<Integer, AToZImpairmentsBuilder>> tapiAtoZSubPathImp) {
        this.tapiAtoZSubPathImpairments.putAll(tapiAtoZSubPathImp);
    }

    public void setTapiZtoASubPathImpairments(
            Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> tapiZtoASubPathImp) {
        this.tapiZtoASubPathImpairments.putAll(tapiZtoASubPathImp);
    }

    public Map<Integer, Map<Integer, AToZImpairmentsBuilder>> getAtoZSubPathImpairments() {
        return this.openRoadmAtoZSubPathImpairments;
    }

    public Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> getZtoASubPathImpairments() {
        return this.openRoadmZtoASubPathImpairments;
    }

    public Map<Integer, Map<Integer, AToZImpairmentsBuilder>> getTapiAtoZSubPathImpairments() {
        return this.tapiAtoZSubPathImpairments;
    }

    public Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> getTapiZtoASubPathImpairments() {
        return this.tapiZtoASubPathImpairments;
    }

    public int getKpathOrderMax() {
        return this.kpathorderMax;
    }

    public boolean getIsFirstStepHybrid() {
        return this.isFirstStepHybrid;
    }

    public void setIsFirstStepHybrid(boolean is1stStepHybrid) {
        this.isFirstStepHybrid = is1stStepHybrid;
    }

    public boolean getIsSecondStepHybrid() {
        return this.isSecondStepHybrid;
    }

    public void setIsSecondStepHybrid(boolean is2ndStepHybrid) {
        this.isSecondStepHybrid = is2ndStepHybrid;
    }

    public boolean getIsSecondStepSuccessfullyFinished() {
        return this.isSecondStepSuccessfullyFinished;
    }

    public void setIsSecondStepSuccessfullyFinished(boolean is2ndStepSuccess) {
        this.isSecondStepSuccessfullyFinished = is2ndStepSuccess;
    }

    public boolean hasSecondStepPCaborted() {
        return this.isSecondStepAborted;
    }

    public void setSecondStepPCtoAborted(boolean isaborted) {
        this.isSecondStepAborted = isaborted;
    }

    /**
    * Adds a list of path elements to the internal map for a specific path order.
    *
    * @param kpathOrder the path order index
    * @param pathElementList the list of path elements to add
    */
    public void addPathElementListToMap(Integer kpathOrder, List<PathElement> pathElementList) {
        this.pathElementMap.put(kpathOrder, pathElementList);
    }

    /**
    * Get the list of path elements in pathElementMap for a specific path order.
    *
    * @param kpathOrder the path order index
    * @return the corresponding list of path elements
    */
    public List<PathElement> getPathElementListK(Integer kpathOrder) {
        return this.pathElementMap.get(kpathOrder);
    }

    /**
    * Get the Service Create Input sci.
    *
    * @return the Service Create Input computed through buildServiceCreateInput, required for service creation
    *         in Data Store
    */
    public OlsServiceCreateInputBuilder getOlsServiceCreateInputBuilder() {
        return this.olssciBldr;
    }

    /**
    * Adds a subgraph path to the internal map with specified indexes.
    *
    * @param korder the path order index
    * @param norder the node order index
    * @param subGraphPath the subgraph path to add
    */
    public void addSubGraphPathToMap(int korder, int norder, SubGraphPath subGraphPath) {
        // Adds a Subgrapth to SubGraphMaps according to its indexes k and n
        Map<Integer, SubGraphPath> msubGraphPath = new HashMap<>();
        msubGraphPath.put(norder, subGraphPath);
        this.subGraphPathMap.put(korder, msubGraphPath);
    }


    /**
    * Adds a Map of Cross Domain Service Path to Aggregated Path Description Builder aggPathDescription.
    *
    * @param cdspMap the MapCdServicePathDescription to be added to this.aggPathDescription.
    */
    public void addCdServiceMapToAggregatedPath(Map<CdServicePathDescriptionKey, CdServicePathDescription> cdspMap) {
        Map<CdServicePathDescriptionKey, CdServicePathDescription> initialCdspMap = aggPathDescription
            .getCdServicePathDescription();
        initialCdspMap.putAll(cdspMap);
        aggPathDescription.setCdServicePathDescription(initialCdspMap);
    }


    /**
    * Retrieves a subgraph path from the map based on path and node order.
    *
    * @param korder the path order index
    * @param norder the node order index
    * @return the corresponding {@link SubGraphPath}
    * @throws NoSuchElementException if the path is not found
    */
    public SubGraphPath getSubGraphPathFromMap(int korder, int norder) {
        // retrieves the SubgraphPath according to its indexes k and n form subGraphPathMap
        try {
            return this.subGraphPathMap.entrySet().stream()
                .filter(map -> map.getKey() == korder).findAny().orElseThrow().getValue()
                    .entrySet().stream().filter(map -> map.getKey() == norder).findAny().orElseThrow().getValue();
        } catch (NoSuchElementException e) {
            LOG.info("SubgraphMap does not contain valid SubGraphPath for k = {} and n = {}", korder, norder, e);
        }
        return null;
    }

    /**
    * Builds a UUID from a string, generating a new UUID from the String, if the string is not a valid UUID.
    *
    * @param inString the input string
    * @return the UUID object
    */
    private Uuid getUuidFromInput(String inString) {
        if (inString == null) {
            return null;
        }
        Uuid outUuid;
        Pattern uuidRegex =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        if (uuidRegex.matcher(inString).matches()) {
            outUuid = new Uuid(inString);
        } else {
            outUuid = new Uuid(UUID.nameUUIDFromBytes(inString.getBytes(StandardCharsets.UTF_8)).toString());
        }
        return outUuid;
    }

    /**
    * Retrieves the subgraph path map for a specific path order.
    *
    * @param korder the path order index
    * @return the map of sub-path indexes to subgraph paths
    */
    public Map<Integer, SubGraphPath> getkSubGraphPathMap(int korder) {
        // retrieves the SubgrapthMap according to its k index
        return this.subGraphPathMap.entrySet().stream()
            .filter(map -> map.getKey() == korder).findAny().orElseThrow().getValue();
    }

    /**
    * Retrieves the preceding edge for a given path and node order.
    *
    * @param korder the path order index
    * @param norder the sub-path order index
    * @return the preceding {@link PceGraphEdge} or null if none exists
    */
    public PceGraphEdge getPrecedingEdge(int korder, int norder) {
        PceGraphEdge precedingEdge = null;
        for (Map.Entry<Integer, Map<Integer, SubGraphPath>> entry : this.subGraphPathMap.entrySet()) {
            if (entry.getKey() == korder) {
                for (Map.Entry<Integer, SubGraphPath> sgentry : entry.getValue().entrySet()) {
                    if (sgentry.getKey() == norder) {
                        precedingEdge = sgentry.getValue().precedingEdge();
                    }
                }
            }
        }
        return precedingEdge;
    }

    /**
    * Converts an OSNR linear value to dB.
    *
    * @param osnrLin the OSNR linear value
    * @return the corresponding dB value
    */
    private double getOsnrDbfromOnsrLin(double osnrLin) {
        return 10 * Math.log10(1 / osnrLin);
    }

    /**
    * Retrieves the succeeding edge for a given path of K order and sub-path of N order.
    *
    * @param korder the path order index
    * @param norder the sub-path order index
    * @return the succeeding {@link PceGraphEdge} or null if none exists
    */
    public PceGraphEdge  getSucceedingEdge(int korder, int norder) {
        PceGraphEdge  succeedingEdge = null;
        for (Map.Entry<Integer, Map<Integer, SubGraphPath>> entry : this.subGraphPathMap.entrySet()) {
            if (entry.getKey() == korder) {
                for (Map.Entry<Integer, SubGraphPath> sgentry : entry.getValue().entrySet()) {
                    if (sgentry.getKey() == norder) {
                        succeedingEdge = sgentry.getValue().succeedingEdge();
                    }
                }
            }
        }
        return succeedingEdge;
    }

    /**
    * Resets the singleton instance, clearing all internal data.
    */
    public void resetInstance() {
        this.openRoadmAtoZSubPathImpairments = new HashMap<>();
        this.openRoadmZtoASubPathImpairments = new HashMap<>();
        this.tapiAtoZSubPathImpairments = new HashMap<>();
        this.tapiZtoASubPathImpairments = new HashMap<>();
        this.pathElementMap = new HashMap<>();
        this.subGraphPathMap = new HashMap<>();
        this.aggPathDescription = new AggregatedPathDescriptionBuilder();
    }
}
