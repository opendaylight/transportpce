/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// internal type after parsing
public class PceConstraints {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);

    // TODO. for now metrics are set into hard structure
    private RoutingConstraintsSp.PceMetric pceMetrics = RoutingConstraintsSp.PceMetric.HopCount;
    private Long maxLatency = (long) -1;

    private List<String> nodesToExclude = new ArrayList<String>();
    private List<String> srlgToExclude = new ArrayList<String>();
    private List<String> nodesToInclude = new ArrayList<String>();
    private List<PceNode> pceNodesToInclude = new ArrayList<PceNode>();
    public static Long constOSNR = 1L;
    private double maxOSNR = (constOSNR / (Math.pow(10, (24 / 10.0))));
//  List<String> srlgToInclude = new ArrayList<String>();


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

    // Exclude nodes / SRLD /
    public List<String> getExcludeNodes() {
        LOG.debug("in Pceconstraints getExcludeNodes size = {}", nodesToExclude.size());
        return nodesToExclude;
    }

    public void setExcludeNodes(List<String> nodes) {
        LOG.debug("in Pceconstraints setExcludeNodes size = {}", nodes.size());
        nodesToExclude.addAll(nodes);
    }

    public List<String> getExcludeSRLG() {
        LOG.debug("in Pceconstraints getExcludeSRLG size = {}", srlgToExclude.size());
        return srlgToExclude;
    }

    public void setExcludeSRLG(List<String> srlg) {
        LOG.debug("in Pceconstraints setExcludeSRLG size = {}", srlg.size());
        srlgToExclude.addAll(srlg);
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


}
