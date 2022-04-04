/*
 * Copyright © 2018 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.DiversityConstraints.DiversityType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.common.constraints.LinkIdentifier;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.common.constraints.LinkIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Distance;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.DistanceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Diversity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.HopCount;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.HopCountBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Latency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.LatencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.TEMetric;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.TEMetricBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service.constraints.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.equipment.EquipmentBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.service.applicability.g.ServiceApplicabilityBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Utility Class to Build Hard Constraints and Soft Constraints.
 * @author Ahmed Helmy ( ahmad.helmy@orange.com )
 * @author Gilles Thouenon (gilles.thouenon@orange.com)
 */
public final class ConstraintsUtils {

    private ConstraintsUtils() {
    }

    /**
     * Build a rather configurable soft-constraint.
     * @param  customerCode         set the list of customer-code provided
     * @param  operationalMode      set list of operational-mode
     * @param  diversityServiceList set diversity constraints from serviceListId
     *                              provided
     * @param  exclude              set exclude constraints
     * @param  include              set include constraints
     * @param  maxLatency           set a maxLatency value, null otherwise
     * @param  hopCount             set a max-wdm-hop-count value
     * @param  teMetric             set a max-wdm-TE-metric
     * @param  maxDistance          set a max-distance value, null otherwise
     * @param  coRoutingServiceId   set co-routing constraints
     * @return                      the hard-constraints
     */
    public static SoftConstraints buildSoftConstraint(List<String> customerCode, boolean operationalMode,
                                                      List<String> diversityServiceList, String exclude, String include,
                                                      Double maxLatency, boolean hopCount, boolean teMetric,
                                                      String maxDistance, String coRoutingServiceId) {

        HardConstraints baseConstraint = buildHardConstraint(customerCode, operationalMode, diversityServiceList,
            exclude, include, maxLatency, hopCount, teMetric, maxDistance, coRoutingServiceId);
        return new SoftConstraintsBuilder()
            .setCustomerCode(baseConstraint.getCustomerCode())
            .setOperationalMode(baseConstraint.getOperationalMode())
            .setDiversity(baseConstraint.getDiversity())
            .setExclude(baseConstraint.getExclude())
            .setInclude(baseConstraint.getInclude())
            .setLatency(baseConstraint.getLatency())
            .setHopCount(baseConstraint.getHopCount())
            .setTEMetric(baseConstraint.getTEMetric())
            .setDistance(baseConstraint.getDistance())
            .setCoRouting(baseConstraint.getCoRouting())
            .build();
    }

    /**
     * Build a rather configurable hard-constraint.
     * @param  customerCode         set the list of customer-code provided
     * @param  operationalMode      set list of operational-mode
     * @param  diversityServiceList set diversity constraints from serviceListId
     *                              provided
     * @param  exclude              set exclude constraints
     * @param  include              set include constraints
     * @param  maxLatency           set a maxLatency value, null otherwise
     * @param  hopCount             set a max-wdm-hop-count value
     * @param  teMetric             set a max-wdm-TE-metric
     * @param  maxDistance          set a max-distance value, null otherwise
     * @param  coRoutingServiceId   set co-routing constraints
     * @return                      the hard-constraints
     */
    public static HardConstraints buildHardConstraint(List<String> customerCode, boolean operationalMode,
                                                      List<String> diversityServiceList, String exclude, String include,
                                                      Double maxLatency, boolean hopCount, boolean teMetric,
                                                      String maxDistance, String coRoutingServiceId) {

        List<String> operationalModeList = null;
        if (operationalMode) {
            operationalModeList = Arrays.asList("operational-mode 1", "operational-mode 2");
        }
        Diversity diversity = null;
        if (diversityServiceList != null && !diversityServiceList.isEmpty()) {
            Map<
                ServiceIdentifierListKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                        .constraints.ServiceIdentifierList> serviceIdList = new HashMap<>();
            for (String serviceId : diversityServiceList) {
                org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                        .constraints.ServiceIdentifierList sil = createServiceIdentifierListForDiversity(serviceId);
                serviceIdList.put(sil.key(), sil);
            }
            diversity = new DiversityBuilder()
                .setDiversityType(DiversityType.Serial)
                .setServiceIdentifierList(serviceIdList)
                .build();
        }

        Latency latency = null;
        if (maxLatency != null) {
            latency = new LatencyBuilder()
                .setMaxLatency(new BigDecimal(maxLatency))
                .build();
        }
        HopCount hc = null;
        if (hopCount) {
            hc = new HopCountBuilder()
                .setMaxWdmHopCount(Uint8.valueOf(3))
                .setMaxOtnHopCount(Uint8.valueOf(5))
                .build();
        }
        Map<String, Exclude> excludeMap = initialiseExcludeMap();
        Exclude exclu = exclude == null || !excludeMap.containsKey(exclude)
            ? null
            : excludeMap.get(exclude);

        Map<String, Include> includeMap = initialiseIncludeMap();
        Include inclu = include == null || !includeMap.containsKey(include)
            ? null
            : includeMap.get(include);

        TEMetric tem = null;
        if (teMetric) {
            tem = new TEMetricBuilder()
                .setMaxWdmTEMetric(Uint32.valueOf(8))
                .setMaxOtnTEMetric(Uint32.valueOf(11))
                .build();
        }
        Distance distance = null;
        if (maxDistance != null) {
            distance = new DistanceBuilder()
                .setMaxDistance(new BigDecimal(maxDistance))
                .build();
        }
        Map<String, CoRouting> coRoutingMap = initialiseCoRoutingMap();
        CoRouting coRouting = coRoutingServiceId == null || !coRoutingMap.containsKey(coRoutingServiceId)
            ? null
            : coRoutingMap.get(coRoutingServiceId);
        return new HardConstraintsBuilder()
            .setCustomerCode(customerCode)
            .setOperationalMode(operationalModeList)
            .setDiversity(diversity)
            .setExclude(exclu)
            .setInclude(inclu)
            .setLatency(latency)
            .setHopCount(hc)
            .setTEMetric(tem)
            .setDistance(distance)
            .setCoRouting(coRouting)
            .build();
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing
            .service.constraints.ServiceIdentifierList createServiceIdentifierListForDiversity(String serviceId) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing
                    .service.constraints.ServiceIdentifierListBuilder()
            .setServiceIndentifier(serviceId)
            .setServiceApplicability(new ServiceApplicabilityBuilder()
                .setLink(true)
                .setNode(true)
                .setSite(false)
                .setSrlg(false)
                .build())
            .build();
    }

    private static Map<String, Exclude> initialiseExcludeMap() {
        LinkIdentifier linkId1 = new LinkIdentifierBuilder()
            .setLinkId("link-id 1")
            .setLinkNetworkId("openroadm-topology")
            .build();
        Exclude exclude1 = new ExcludeBuilder()
            .setLinkIdentifier(Map.of(linkId1.key(), linkId1))
            .build();
        Exclude exclude2 = new ExcludeBuilder()
            .setNodeId(List.of(new NodeIdType("node-id-2")))
            .build();
        Exclude exclude3 = new ExcludeBuilder()
            .setSupportingServiceName(List.of("supported-service-1", "supported-service-5"))
            .build();
        Exclude exclude4 = new ExcludeBuilder()
            .setFiberBundle(List.of("fiber-1", "fiber-2"))
            .build();
        Exclude exclude5 = new ExcludeBuilder()
            .setFiberBundle(List.of("fiber-2", "fiber-3"))
            .build();
        LinkIdentifier linkId2 = new LinkIdentifierBuilder()
            .setLinkId("link-id 2")
            .setLinkNetworkId("openroadm-topology")
            .build();
        LinkIdentifier linkId3 = new LinkIdentifierBuilder()
            .setLinkId("link-id 3")
            .setLinkNetworkId("openroadm-topology")
            .build();
        Exclude exclude6 = new ExcludeBuilder()
            .setLinkIdentifier(Map.of(linkId2.key(), linkId2, linkId3.key(), linkId3))
            .build();
        return Map.of("link1", exclude1, "node", exclude2, "service", exclude3, "fiber1", exclude4, "fiber2", exclude5,
            "link2", exclude6);
    }

    private static Map<String, Include> initialiseIncludeMap() {
        LinkIdentifier linkId1 = new LinkIdentifierBuilder()
            .setLinkId("link-id 1")
            .setLinkNetworkId("openroadm-topology")
            .build();
        Include exclude1 = new IncludeBuilder()
            .setLinkIdentifier(Map.of(linkId1.key(), linkId1))
            .build();
        Include exclude2 = new IncludeBuilder()
            .setNodeId(List.of(new NodeIdType("node-id-1"), new NodeIdType("node-id-3")))
            .build();
        Include exclude3 = new IncludeBuilder()
            .setSupportingServiceName(List.of("supported-service-1", "supported-service-5"))
            .build();
        Include exclude4 = new IncludeBuilder()
            .setFiberBundle(List.of("fiber-1", "fiber-2"))
            .build();
        Include exclude5 = new IncludeBuilder()
            .setFiberBundle(List.of("fiber-2", "fiber-3"))
            .build();
        LinkIdentifier linkId2 = new LinkIdentifierBuilder()
            .setLinkId("link-id 2")
            .setLinkNetworkId("openroadm-topology")
            .build();
        LinkIdentifier linkId3 = new LinkIdentifierBuilder()
            .setLinkId("link-id 3")
            .setLinkNetworkId("openroadm-topology")
            .build();
        Include exclude6 = new IncludeBuilder()
            .setLinkIdentifier(Map.of(linkId2.key(), linkId2, linkId3.key(), linkId3))
            .build();
        return Map.of("link1", exclude1, "node", exclude2, "service", exclude3, "fiber1", exclude4, "fiber2", exclude5,
            "link2", exclude6);
    }

    private static Map<String, CoRouting> initialiseCoRoutingMap() {
        ServiceIdentifierList sil1 = new ServiceIdentifierListBuilder()
            .setServiceIdentifier("service 1")
            .setServiceApplicability(new ServiceApplicabilityBuilder()
                .setEquipment(new EquipmentBuilder()
                    .setRoadmSrg(true)
                    .build())
                .build())
            .build();
        ServiceIdentifierList sil2 = new ServiceIdentifierListBuilder()
            .setServiceIdentifier("service 2")
            .setServiceApplicability(new ServiceApplicabilityBuilder()
                .setLink(true)
                .build())
            .build();
        ServiceIdentifierList sil3 = new ServiceIdentifierListBuilder()
            .setServiceIdentifier("service 3")
            .setServiceApplicability(new ServiceApplicabilityBuilder()
                .setSite(true)
                .build())
            .build();
        CoRouting coRouting1 = new CoRoutingBuilder()
            .setServiceIdentifierList(Map.of(sil1.key(), sil1, sil2.key(), sil2))
            .build();
        CoRouting coRouting2 = new CoRoutingBuilder()
            .setServiceIdentifierList(Map.of(sil3.key(), sil3))
            .build();
        return Map.of("coRouting1", coRouting1, "coRouting2", coRouting2);
    }
}
