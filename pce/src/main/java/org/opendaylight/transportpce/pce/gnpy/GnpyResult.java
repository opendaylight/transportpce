/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.Result;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.explicit.route.hop.type.NumUnnumHop;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathMetric;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.Response;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.NoPathCase;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.PathCase;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for analyzing the result sent by GNPy.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyResult {

    private static final Logger LOG = LoggerFactory.getLogger(GnpyResult.class);
    private Response response = null;
    private Map<String, IpAddress> mapNodeRefIp = new HashMap<>();

    public GnpyResult(Result result, GnpyTopoImpl gnpyTopo) throws GnpyException {
        this.mapNodeRefIp = gnpyTopo.getMapNodeRefIp();
        List<Response> responses = new ArrayList<>(result.nonnullResponse().values());
        if (responses.isEmpty()) {
            throw new GnpyException("In GnpyResult: the response from GNpy is null!");
        }
        LOG.info("The response id is {}; ", responses.get(0).getResponseId());
        this.response = responses.get(0);
        analyzeResult();
    }

    public boolean getPathFeasibility() {
        boolean isFeasible = false;
        if (response != null) {
            if (response.getResponseType() instanceof NoPathCase) {
                LOG.info("In GnpyResult: The path is not feasible ");
            } else if (response.getResponseType() instanceof PathCase) {
                isFeasible = true;
                LOG.info("In GnpyResult: The path is feasible ");
            }
        }
        return isFeasible;
    }

    public List<PathRouteObjects> analyzeResult() {
        List<PathRouteObjects> pathRouteObjectList = null;
        if (response != null) {
            if (response.getResponseType() instanceof NoPathCase) {
                NoPathCase noPathCase = (NoPathCase) response.getResponseType();
                String noPathType = noPathCase.getNoPath().getNoPath();
                LOG.info("GNPy: No path - {}", noPathType);
                if (((noPathType.equals("NO_FEASIBLE_BAUDRATE_WITH_SPACING"))
                    && (noPathType.equals("NO_FEASIBLE_MODE"))) && ((noPathType.equals("MODE_NOT_FEASIBLE"))
                        && (noPathType.equals("NO_SPECTRUM")))) {
                    Collection<PathMetric> pathMetricList = noPathCase.getNoPath()
                            .getPathProperties().nonnullPathMetric().values();
                    LOG.info("GNPy : path is not feasible : {}", noPathType);
                    for (PathMetric pathMetric : pathMetricList) {
                        String metricType = pathMetric.getMetricType().getSimpleName();
                        BigDecimal accumulativeValue = pathMetric.getAccumulativeValue();
                        LOG.info("Metric type {} // AccumulatriveValue {}", metricType, accumulativeValue);
                    }
                }
            } else if (response.getResponseType() instanceof PathCase) {
                LOG.info("GNPy : path is feasible");
                PathCase pathCase = (PathCase) response.getResponseType();
                Collection<PathMetric> pathMetricList = pathCase
                        .getPathProperties().nonnullPathMetric().values();
                // Path metrics
                for (PathMetric pathMetric : pathMetricList) {
                    String metricType = pathMetric.getMetricType().getSimpleName();
                    BigDecimal accumulativeValue = pathMetric.getAccumulativeValue();
                    LOG.info("Metric type {} // AccumulatriveValue {}", metricType, accumulativeValue);
                }
                // Path route objects
                pathRouteObjectList = pathCase.getPathProperties().getPathRouteObjects();
                LOG.info("in GnpyResult: finishing the computation of pathRouteObjectList");
            }
        }
        return pathRouteObjectList;
    }

    public HardConstraints computeHardConstraintsFromGnpyPath(List<PathRouteObjects> pathRouteObjectList) {
        HardConstraints hardConstraints = null;
        // Includes the list of nodes in the GNPy computed path as constraints
        // for the PCE
        List<NodeIdType> nodeIdList = new ArrayList<>();
        for (PathRouteObjects pathRouteObjects : pathRouteObjectList) {
            if (pathRouteObjects.getPathRouteObject().getType() instanceof NumUnnumHop) {
                NumUnnumHop numUnnumHop = (org.opendaylight.yang.gen.v1.gnpy.path.rev200909.explicit.route.hop.type
                    .NumUnnumHop) pathRouteObjects.getPathRouteObject().getType();
                String nodeIp = numUnnumHop.getNumUnnumHop().getNodeId();
                try {
                    IpAddress nodeIpAddress = new IpAddress(new Ipv4Address(nodeIp));
                    // find the corresponding node-id (in ord-ntw) corresponding to nodeId (in gnpy response)
                    String nodeId = findOrdNetworkNodeId(nodeIpAddress);
                    if (nodeId != null) {
                        nodeIdList.add(new NodeIdType(nodeId));
                    }
                } catch (IllegalArgumentException e) {
                    LOG.error(" in GnpyResult: the element {} is not a ipv4Address ", nodeIp, e);
                }
            }
        }

        Include include = new IncludeBuilder()
            .setNodeId(nodeIdList)
             .build();
        hardConstraints = new HardConstraintsBuilder()
            .setInclude(include)
            .build();
        return hardConstraints;
    }

    private String findOrdNetworkNodeId(IpAddress nodeIpAddress) {
        Iterator<Map.Entry<String,IpAddress>> it = this.mapNodeRefIp.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, IpAddress> entry = it.next();
            if (entry.getValue().equals(nodeIpAddress)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Response getResponse() {
        return response;
    }
}
