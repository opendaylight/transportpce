/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.constraints;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// internal type after parsing
public class PceConstraints {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceConstraints.class);

    // TODO. for now metrics are set into hard structure
    private RoutingConstraintsSp.PceMetric pceMetrics = RoutingConstraintsSp.PceMetric.HopCount;
    private Long maxLatency = (long) -1;

    /////////////// EXCLUDE ///////////////////
    // Nodes/CLLI/SRLG lists might consist of two types of elements : (1)from diversity constraints (2)from exclude list
    // e.g.: nodesToExclude - topo-level nodes IDs - comes from diversity constraints
    //     : supNodesToExclude - supporting nodes IDs - comes from exclude list
    // "mapConstraints" class converts diversity elements into correct names
    private List<String> nodesToExclude = new ArrayList<String>();
    private List<String> supNodesToExclude = new ArrayList<String>();

    private List<Long> srlgToExclude = new ArrayList<Long>();
    private List<String> srlgLinksToExclude = new ArrayList<String>();

    private List<String> clliToExclude = new ArrayList<String>();
    private List<String> clliNodesToExclude = new ArrayList<String>();
    private List<String> nodesToInclude = new ArrayList<String>();
    private List<PceNode> pceNodesToInclude = new ArrayList<PceNode>();

    public static final Long CONST_OSNR = 1L;
    private double maxOSNR = (CONST_OSNR / (Math.pow(10, (24 / 10.0))));

    /**.
     * /////////////// INCLUDE CONSTRAINTS.///////////////////
     */
    private List<ResourcePair> listToInclude = new ArrayList<ResourcePair>();
    private List<String> srlgNames = new ArrayList<String>();

    public enum ResourceType {
        NONE, NODE, SRLG, CLLI;
    }

    public RoutingConstraintsSp.PceMetric getPceMetrics() {
        return pceMetrics;
    }

    public void setPceMetrics(RoutingConstraintsSp.PceMetric pceMetrics) {
        this.pceMetrics = pceMetrics;
    }

    // Latency of path is used by Graph to validate path
    // if -1 , latency is not applicable (not mentioned in constraints)
    public Long getMaxLatency() {
        LOG.debug("in Pceconstraints getMaxLatency = {}", maxLatency);
        return maxLatency;
    }

    public void setMaxLatency(Long maxLatency) {
        LOG.debug("in Pceconstraints setMaxLatency = {}", maxLatency);
        this.maxLatency = maxLatency;
    }

    // Exclude nodes / SRLD / CLLI

    public List<String> getExcludeSupNodes() {
        LOG.debug("in Pceconstraints getExcludeSupNodes size = {}", supNodesToExclude.size());
        return supNodesToExclude;
    }

    public void setExcludeSupNodes(List<String> supNodes) {
        LOG.debug("in Pceconstraints setExcludeSupNodes size = {}", supNodes.size());
        supNodesToExclude.addAll(supNodes);
    }

    public List<Long> getExcludeSRLG() {
        LOG.debug("in Pceconstraints getExcludeSRLG size = {}", srlgToExclude.size());
        return srlgToExclude;
    }

    public void setExcludeSRLG(List<Long> srlg) {
        LOG.info("in Pceconstraints setExcludeSRLG size = {}", srlg.size());
        srlgToExclude.addAll(srlg);
    }

    public List<String> getExcludeCLLI() {
        LOG.debug("in Pceconstraints getExcludeCLLI size = {}", clliToExclude.size());
        return clliToExclude;
    }

    public void setExcludeCLLI(List<String> clli) {
        LOG.debug("in Pceconstraints setExcludeCLLI size = {}", clli.size());
        clliToExclude.addAll(clli);
    }

    // CLLI nodes are defined as result of 'diversity 'node'' constraints
    // clliNodesToExclude are saved as nodes, during NW analysis the relevant
    // CLLI IDs are added to clliToExclude
    public List<String> getExcludeClliNodes() {
        LOG.info("in Pceconstraints getExcludeClliNodes size = {}", clliNodesToExclude.size());
        return clliNodesToExclude;
    }

    public void setExcludeClliNodes(List<String> clli) {
        LOG.debug("in Pceconstraints setExcludeCLLI size = {}", clli.size());
        clliNodesToExclude.addAll(clli);
    }

    public List<String> getExcludeSrlgLinks() {
        LOG.info("in Pceconstraints getExcludeSrlgNodes size = {}", srlgLinksToExclude.size());
        return srlgLinksToExclude;
    }

    public void setExcludeSrlgLinks(List<String> srlg) {
        LOG.debug("in Pceconstraints setExcludeSRLG size = {}", srlg.size());
        srlgLinksToExclude.addAll(srlg);
    }

    public List<String> getExcludeNodes() {
        LOG.info("in Pceconstraints getExcludeNodes size = {}", nodesToExclude.size());
        return nodesToExclude;
    }

    public void setExcludeNodes(List<String> nodes) {
        LOG.debug("in Pceconstraints setExcludeNodes size = {}", nodes.size());
        nodesToExclude.addAll(nodes);
    }

    // Include nodes
    public List<String> getIncludeNodes() {
        LOG.debug("in Pceconstraints getIncludeNodes size = {}", nodesToInclude.size());
        return nodesToInclude;
    }

    public void setIncludeNodes(List<String> nodes) {
        LOG.debug("in Pceconstraints setIncludeNodes size = {}", nodes.size());
        nodesToInclude.addAll(nodes);
    }

    public List<PceNode> getIncludePceNodes() {
        LOG.debug("in Pceconstraints getIncludePceNodes size = {}", pceNodesToInclude.size());
        return pceNodesToInclude;
    }

    public void setIncludePceNode(PceNode node) {
        LOG.info("in Pceconstraints setIncludePceNode new node = {}", node.toString());
        this.pceNodesToInclude.add(node);
    }

    public Double getMaxOSNR() {
        LOG.debug("in Pceconstraints getMaxOSNR = {}", maxOSNR);
        return maxOSNR;
    }

    public class ResourcePair {
        public ResourcePair(ResourceType type, String name) {
            super();
            this.type = type;
            this.name = name;
        }

        public ResourceType type = ResourceType.NODE;

        public String name = "";
    }

    public List<ResourcePair> getListToInclude() {
        return listToInclude;
    }

    public void setListToInclude(ResourcePair elementToInclude) {
        this.listToInclude.add(elementToInclude);
        switch (elementToInclude.type) {
            case SRLG:
                srlgNames.add(elementToInclude.name);
                break;
            default:
                break;
        }
    }

    public List<String> getSRLGnames() {
        return srlgNames;
    }
}
