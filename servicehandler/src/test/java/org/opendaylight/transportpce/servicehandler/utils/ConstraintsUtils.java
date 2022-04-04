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
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.DistanceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.HopCountBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.LatencyBuilder;
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

        Map<ServiceIdentifierListKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                    .constraints.ServiceIdentifierList>
                serviceIdList = new HashMap<>();
        if (diversityServiceList != null) {
            for (String serviceId : diversityServiceList) {
                org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                        .constraints.ServiceIdentifierList sil = createServiceIdentifierListForDiversity(serviceId);
                serviceIdList.put(sil.key(), sil);
            }
        }

        Map<String, Exclude> excludeMap = initialiseExcludeMap();
        Map<String, Include> includeMap = initialiseIncludeMap();
        Map<String, CoRouting> coRoutingMap = initialiseCoRoutingMap();

        return new HardConstraintsBuilder()
            .setCustomerCode(customerCode)
            .setOperationalMode(
                operationalMode
                    ? Arrays.asList("operational-mode 1", "operational-mode 2")
                    : null)
            .setDiversity(
                serviceIdList.isEmpty()
                    ? null
                    : new DiversityBuilder()
                        .setDiversityType(DiversityType.Serial)
                        .setServiceIdentifierList(serviceIdList)
                        .build())
            .setExclude(
                exclude == null || !excludeMap.containsKey(exclude)
                    ? null
                    : excludeMap.get(exclude))
            .setInclude(
                include == null || !includeMap.containsKey(include)
                    ? null
                    : includeMap.get(include))
            .setLatency(
                maxLatency == null
                    ? null
                    : new LatencyBuilder().setMaxLatency(new BigDecimal(maxLatency)).build())
            .setHopCount(
                hopCount
                    ? new HopCountBuilder()
                        .setMaxWdmHopCount(Uint8.valueOf(3))
                        .setMaxOtnHopCount(Uint8.valueOf(5))
                        .build()
                    : null)
            .setTEMetric(
                teMetric
                    ? new TEMetricBuilder()
                        .setMaxWdmTEMetric(Uint32.valueOf(8))
                        .setMaxOtnTEMetric(Uint32.valueOf(11))
                        .build()
                    : null)
            .setDistance(
                maxDistance == null
                    ? null
                    : new DistanceBuilder().setMaxDistance(new BigDecimal(maxDistance)).build())
            .setCoRouting(
                coRoutingServiceId == null || !coRoutingMap.containsKey(coRoutingServiceId)
                    ? null
                    : coRoutingMap.get(coRoutingServiceId))
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
        HashMap<String, Exclude> excludeHashMap =  new HashMap<>();
        LinkIdentifier linkId1 = new LinkIdentifierBuilder()
            .setLinkId("link-id 1")
            .setLinkNetworkId("openroadm-topology")
            .build();
        excludeHashMap.put(
            "link1",
            new ExcludeBuilder()
                .setLinkIdentifier(Map.of(linkId1.key(), linkId1))
                .build());
        excludeHashMap.put(
            "node",
            new ExcludeBuilder()
                .setNodeId(List.of(new NodeIdType("node-id-2")))
                .build());
        excludeHashMap.put(
            "service",
            new ExcludeBuilder()
                .setSupportingServiceName(List.of("supported-service-1", "supported-service-5"))
                .build());
        excludeHashMap.put(
            "fiber1",
            new ExcludeBuilder()
                .setFiberBundle(List.of("fiber-1", "fiber-2"))
                .build());
        excludeHashMap.put(
            "fiber2",
            new ExcludeBuilder()
                .setFiberBundle(List.of("fiber-2", "fiber-3"))
                .build());
        LinkIdentifier linkId2 = new LinkIdentifierBuilder()
            .setLinkId("link-id 2")
            .setLinkNetworkId("openroadm-topology")
            .build();
        LinkIdentifier linkId3 = new LinkIdentifierBuilder()
            .setLinkId("link-id 3")
            .setLinkNetworkId("openroadm-topology")
            .build();
        excludeHashMap.put(
            "link2",
            new ExcludeBuilder()
                .setLinkIdentifier(Map.of(linkId2.key(), linkId2, linkId3.key(), linkId3))
                .build());
        return excludeHashMap;
    }

    private static Map<String, Include> initialiseIncludeMap() {
        HashMap<String, Include> includeHashMap =  new HashMap<>();
        LinkIdentifier linkId1 = new LinkIdentifierBuilder()
            .setLinkId("link-id 1")
            .setLinkNetworkId("openroadm-topology")
            .build();
        includeHashMap.put(
            "link1",
            new IncludeBuilder()
                .setLinkIdentifier(Map.of(linkId1.key(), linkId1))
                .build());
        includeHashMap.put(
            "node",
            new IncludeBuilder()
                .setNodeId(List.of(new NodeIdType("node-id-1"), new NodeIdType("node-id-3")))
                .build());
        includeHashMap.put(
            "service",
            new IncludeBuilder()
                .setSupportingServiceName(List.of("supported-service-1", "supported-service-5"))
                .build());
        includeHashMap.put(
            "fiber1",
            new IncludeBuilder()
                .setFiberBundle(List.of("fiber-1", "fiber-2"))
                .build());
        includeHashMap.put(
            "fiber2",
            new IncludeBuilder()
                .setFiberBundle(List.of("fiber-2", "fiber-3"))
                .build());
        LinkIdentifier linkId2 = new LinkIdentifierBuilder()
            .setLinkId("link-id 2")
            .setLinkNetworkId("openroadm-topology")
            .build();
        LinkIdentifier linkId3 = new LinkIdentifierBuilder()
            .setLinkId("link-id 3")
            .setLinkNetworkId("openroadm-topology")
            .build();
        includeHashMap.put(
            "link2",
            new IncludeBuilder()
                .setLinkIdentifier(Map.of(linkId2.key(), linkId2, linkId3.key(), linkId3))
                .build());
        return includeHashMap;
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
        return Map.of(
            "coRouting1",
            new CoRoutingBuilder().setServiceIdentifierList(Map.of(sil1.key(), sil1, sil2.key(), sil2)).build(),
            "coRouting2",
            new CoRoutingBuilder().setServiceIdentifierList(Map.of(sil3.key(), sil3)).build());
    }
}
