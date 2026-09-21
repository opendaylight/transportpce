/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.orchestrator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.pce.graph.PceGraph.PathElement;
import org.opendaylight.transportpce.pce.graph.PceGraph.SubGraphPath;
import org.opendaylight.transportpce.pce.graph.PceGraphEdge;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.AggregatedPathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.AggregatedPathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.CrossDomainService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.CrossDomainServiceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.CdServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.ImpairmentParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.AToZImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.ZToAImpairmentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Map<Integer, CdServicesBuilder> crossDomainServiceBldr = new HashMap<>();
    // Storage of impairment result (OpenROADM domains) for each KorderPath (FirstKey)
    // and each domains (2NdKey is domain order).
    private Map<Integer, Map<Integer, AToZImpairmentsBuilder>> openRoadmAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> openRoadmZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, AToZImpairmentsBuilder>> tapiAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, ZToAImpairmentsBuilder>> tapiZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, SubGraphPath>> subGraphPathMap = new HashMap<>();
    private List<Integer> prunedKorderList = new ArrayList<>();
    public AggregatedPathDescription aggPathDescription = new AggregatedPathDescriptionBuilder().build();
    public CrossDomainService crossDomainService = new CrossDomainServiceBuilder().build();
//    public int kpathorder = 0;
    private int kpathorderMax = 0;

    private PceCrossDomainPathAggregator() {

    }

    public static synchronized PceCrossDomainPathAggregator getInstance() {
        if (instance == null) {
            instance = new PceCrossDomainPathAggregator();
        }
        return instance;
    }

    // Checks that impairments calculated on OpenROADM sub-path do not exceed what the device performance allow.
    // If this is the case remove from all subPathImpairment maps the korder path
    // Allows to simplify path computation in TAPI domain to avoid computing unnecessary path in TAPI domains
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

    public PceResult checkE2EpathImpairments(int korder, CatalogUtils cu) {
        // When a path has been analyzed through checkPath in the TAPI PCE, we need to check E2E path consistency
        // and if the path is consistent return true; otherwise false, which makes Graph.calcPath iterate through next
        //path. Korder is used to retrieve the corresponding OpenROADM impairments in Maps.
        PceResult pceResult = new PceResult();
        double margin = calculateE2Emargin(korder, cu);
        if (margin >= 0) {
            pceResult.success();
            return pceResult;
        }
        pceResult.error("No path found spanning accross TapiDomain found for path of Korder = " + korder);
        pceResult.setLocalCause(PceResult.LocalCause.NO_PATH_EXISTS);
        return pceResult;
    }

    public boolean buildAggregatedPath(int korder) {
        // When a path has been analyzed through checkPath in the TAPI PCE, and E2E path consistency is succesfully
        // check, we need to build the the cross-domain service Container with its attributes and cleanup tables.
        // returns true if successful
        // calls populateCDServBlderWithOpticalParams(int cdServiceOrder,AToZImpairmentsBuilder atozBldr/ztoaBldr)
        return true;
    }

    public Map<Integer, List<PathElement>> prunePathElementMap(Map<Integer, List<PathElement>> pathelementMap) {
        // After pruneOpenROADMimpairments has been purging from the list of Path for which a path needs to be found
        // in TAPI domain, the path that are not good candidate need to be removed from the PathElemnt Map of PceGraph
        return pathelementMap.entrySet().stream()
                .filter(pathElt -> prunedKorderList.contains(pathElt.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public ServiceCreateInput buildServiceCreateInput(int korder, int norder) {
        // Builds the service create for TAPI service creation in the TAPI Domain
        SubGraphPath subgraphpath = getSubGraphPathFromMap(korder, norder);
        LOG.info("SubgraphPtah under analysis is {}", subgraphpath);

        return new ServiceCreateInputBuilder().build();
    }

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

    public void pruneTapiSbiABSpath() {
        // As most of the K E2E path will probably result in the same entry and exit point in the TAPI-SBI-ABS Node it
        // Makes sense to limit the number of PAth computation Request exercised through the TAPI PCE
    }

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

    public void setOpenRoadmAtoZSubPathImpairments(
            Map<Integer, Map<Integer, AToZImpairmentsBuilder>> orAtoZSubPathImp) {
        this.openRoadmAtoZSubPathImpairments.putAll(orAtoZSubPathImp);
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

    public void addPathElementListToMap(Integer kpathOrder, List<PathElement> pathElementList) {
        this.pathElementMap.put(kpathOrder, pathElementList);
    }

    public void addSubGraphPathToMap(int korder, int norder, SubGraphPath subGraphPath) {
        // Adds a Subgrapth to SubGraphMaps according to its indexes k and n
        Map<Integer, SubGraphPath> msubGraphPath = new HashMap<>();
        msubGraphPath.put(norder, subGraphPath);
        this.subGraphPathMap.put(korder, msubGraphPath);
    }

    public SubGraphPath getSubGraphPathFromMap(int korder, int norder) {
        // retrieves the Subgrapth according to its indexes k and n form subGraphPathMap
        return this.subGraphPathMap.entrySet().stream()
            .filter(map -> map.getKey() == korder).findAny().orElseThrow().getValue()
                .entrySet().stream().filter(map -> map.getKey() == norder).findAny().orElseThrow().getValue();
    }

    public Map<Integer, SubGraphPath> getkSubGraphPathMap(int korder) {
        // retrieves the SubgrapthMap according to its k index
        return this.subGraphPathMap.entrySet().stream()
            .filter(map -> map.getKey() == korder).findAny().orElseThrow().getValue();
    }

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

    private double getOsnrDbfromOnsrLin(double osnrLu) {
        return 10 * Math.log10(1 / osnrLu);
    }

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

    public void populateCDServBlderWithOpticalParams(int cdServiceOrder,
            AToZImpairmentsBuilder atozBldr, ZToAImpairmentsBuilder ztoaBldr) {
        ImpairmentParametersBuilder impairments = new ImpairmentParametersBuilder()
            .setAToZImpairments(atozBldr.build())
            .setZToAImpairments(ztoaBldr.build());
        this.crossDomainServiceBldr.get(cdServiceOrder)
            .setCalculatedImpairments(true)
            .setImpairmentParameters(impairments.build());
    }


    public void resetInstance() {
        this.openRoadmAtoZSubPathImpairments = new HashMap<>();
        this.openRoadmZtoASubPathImpairments = new HashMap<>();
        this.tapiAtoZSubPathImpairments = new HashMap<>();
        this.tapiZtoASubPathImpairments = new HashMap<>();
        this.pathElementMap = new HashMap<>();
        this.crossDomainServiceBldr = new HashMap<>();
        this.subGraphPathMap = new HashMap<>();
    }
}
