/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.pce.graph.PceGraph.PathElement;
import org.opendaylight.transportpce.pce.graph.PceGraph.SubGraphPath;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.CdServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.ImpairmentParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.AToZImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.cross.domain.service.attributes.cd.services.impairment.parameters.ZToAImpairmentsBuilder;

public final class PceCrossDomainPathAggregator {

    private static PceCrossDomainPathAggregator instance;

    private List<PathElement> pathElementList = new ArrayList<>();
    private Map<Integer, CdServicesBuilder> crossDomainServiceBldr = new HashMap<>();
    // Storage of impairment result (OpenROADM domains) for each KorderPath (FirstKey)
    // and each domains (2NdKey is domain order).
    private Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> openRoadmAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> openRoadmZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> tapiAtoZSubPathImpairments = new HashMap<>();
    private Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> tapiZtoASubPathImpairments = new HashMap<>();
    private Map<Integer, Map<Integer, SubGraphPath>> subGraphPathMap = new HashMap<>();

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
    public void pruneOpenROADMimpairments() {
        // Calls checkOpenROADMimpairments to cleanup AtoZSubImpaiment & Subgraph  maps as well as pathElementList
        checkOpenRoadmImpairments(0);
    }

    public boolean checkE2EpathImpairments(int korder) {
        // When a path has been analyzed through checkPath in the TAPI PCE, we need to check E2E path consistency
        // and if the path is consistent return true; otherwise false, which makes Graph.calcPath iterate through next
        //path. Korder is used to retrieve the corresponding OpenROADM impairments in Maps.
        calculateE2Emargin();
        return true;
    }

    public boolean buildE2Epath(int korder) {
        // When a path has been analyzed through checkPath in the TAPI PCE, and E2E path consistency is succesfully
        // check, we need to build the the cross-domain service Container with its attributes and cleanup tables.
        // returns true if successful
        // calls populateCDServBlderWithOpticalParams(int cdServiceOrder,AToZImpairmentsBuilder atozBldr/ztoaBldr)
        return true;
    }


    private void checkOpenRoadmImpairments(Integer kpathorder) {

    }

    public void pruneTapiSbiABSpath() {
        // As most of the K E2E path will probably result in the same entry and exit point in the TAPI-SBI-ABS Node it
        // Makes sense to limit the number of PAth computation Request exercised through the TAPI PCE
    }

    private void calculateE2Emargin() {

    }

    public void setOpenRoadmAtoZSubPathImpairments(
            Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> orAtoZSubPathImp) {
        this.openRoadmAtoZSubPathImpairments.putAll(orAtoZSubPathImp);
    }

    public void setOpenRoadmZtoASubPathImpairments(
            Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> orZtoASubPathImp) {
        this.openRoadmZtoASubPathImpairments.putAll(orZtoASubPathImp);
    }

    public void setTapiAtoZSubPathImpairments(
            Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> tapiAtoZSubPathImp) {
        this.tapiAtoZSubPathImpairments.putAll(tapiAtoZSubPathImp);
    }

    public void setTapiZtoASubPathImpairments(
            Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> tapiZtoASubPathImp) {
        this.tapiZtoASubPathImpairments.putAll(tapiZtoASubPathImp);
    }

    public Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> getAtoZSubPathImpairments() {
        return this.openRoadmAtoZSubPathImpairments;
    }

    public Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> getZtoASubPathImpairments() {
        return this.openRoadmZtoASubPathImpairments;
    }

    public Map<Integer, List<Map<Integer, AToZImpairmentsBuilder>>> getTapiAtoZSubPathImpairments() {
        return this.tapiAtoZSubPathImpairments;
    }

    public Map<Integer, List<Map<Integer, ZToAImpairmentsBuilder>>> getTapiZtoASubPathImpairments() {
        return this.tapiZtoASubPathImpairments;
    }

    public void setPathElementList(List<PathElement> pathElementList) {
        this.pathElementList.addAll(pathElementList);
    }

    public void addSubGraphPathToMap(int korder, int norder, SubGraphPath subGraphPath) {
        // Adds a Subgrapth to SubGraphMaps according to its indexes k and n
        Map<Integer, SubGraphPath> msubGraphPath = new HashMap<>();
        msubGraphPath.put(norder, subGraphPath);
        this.subGraphPathMap.put(korder, msubGraphPath);
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
        this.pathElementList = new ArrayList<>();
        this.crossDomainServiceBldr = new HashMap<>();
        this.subGraphPathMap = new HashMap<>();
    }



}
