/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.common.constraints.LinkIdentifier;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.common.constraints.LinkIdentifierKey;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to Map Hard Constraints to Soft Constraints.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 * @author gilles Thouenon (gilles.thouenon@orange.com)
 */
public final class DowngradeConstraints {

    private static final Logger LOG = LoggerFactory.getLogger(DowngradeConstraints.class);

    private DowngradeConstraints() {
    }

    /**
     * Add Hard Constraints to Soft Constraints.
     * @param  hardConstraints to be added
     * @param  softConstraints to be modified
     * @return                 SoftConstraints modified
     */
    public static SoftConstraints updateSoftConstraints(HardConstraints hardConstraints,
                                                        SoftConstraints softConstraints) {
        SoftConstraintsBuilder softConstraintsBuilder = new SoftConstraintsBuilder(softConstraints);
        if (hardConstraints.getCustomerCode() != null && !hardConstraints.getCustomerCode().isEmpty()) {
            if (softConstraintsBuilder.getCustomerCode() == null
                    || softConstraintsBuilder.getCustomerCode().isEmpty()) {
                softConstraintsBuilder.setCustomerCode(hardConstraints.getCustomerCode());
            } else {
                List<String> updatedCustomerCode = new ArrayList<>(softConstraintsBuilder.getCustomerCode());
                updatedCustomerCode.addAll(hardConstraints.getCustomerCode());
                softConstraintsBuilder.setCustomerCode(updatedCustomerCode);
            }
        }
        if (hardConstraints.getOperationalMode() != null && !hardConstraints.getOperationalMode().isEmpty()) {
            if (softConstraintsBuilder.getOperationalMode() == null
                    || softConstraintsBuilder.getOperationalMode().isEmpty()) {
                softConstraintsBuilder.setOperationalMode(hardConstraints.getOperationalMode());
            } else {
                List<String> updatedOperationalMode = new ArrayList<>(softConstraintsBuilder.getOperationalMode());
                updatedOperationalMode.addAll(hardConstraints.getOperationalMode());
                softConstraintsBuilder.setOperationalMode(updatedOperationalMode);
            }
        }
        if (hardConstraints.getDiversity() != null) {
            softConstraintsBuilder
                .setDiversity(updateDiveristy(hardConstraints.getDiversity(), softConstraints.getDiversity()));
        }
        if (hardConstraints.getExclude() != null) {
            softConstraintsBuilder
                .setExclude(updateExclude(hardConstraints.getExclude(), softConstraints.getExclude()));
        }
        if (hardConstraints.getInclude() != null) {
            softConstraintsBuilder
                .setInclude(updateInclude(hardConstraints.getInclude(), softConstraints.getInclude()));
        }
        if (hardConstraints.getLatency() != null) {
            softConstraintsBuilder
                .setLatency(updateLatency(hardConstraints.getLatency(), softConstraints.getLatency()));
        }
        if (hardConstraints.getDistance() != null) {
            softConstraintsBuilder
                .setDistance(updateDistance(hardConstraints.getDistance(), softConstraints.getDistance()));
        }
        if (hardConstraints.getHopCount() != null) {
            softConstraintsBuilder
                .setHopCount(updateHopCount(hardConstraints.getHopCount(), softConstraints.getHopCount()));
        }
        if (hardConstraints.getTEMetric() != null) {
            softConstraintsBuilder
                .setTEMetric(updateTEMetric(hardConstraints.getTEMetric(), softConstraints.getTEMetric()));
        }

        if (hardConstraints.getCoRouting() != null) {
            softConstraintsBuilder
                .setCoRouting(updateCoRouting(hardConstraints.getCoRouting(), softConstraints.getCoRouting()));
        }
        return softConstraintsBuilder.build();
    }

    private static Include updateInclude(Include hard, Include soft) {
        IncludeBuilder includeBldr = soft == null ? new IncludeBuilder() : new IncludeBuilder(soft);

        if (hard.getFiberBundle() != null && !hard.getFiberBundle().isEmpty()) {
            if (includeBldr.getFiberBundle() == null) {
                includeBldr.setFiberBundle(hard.getFiberBundle());
            } else {
                Set<String> fiberList = new HashSet<>(includeBldr.getFiberBundle());
                fiberList.addAll(hard.getFiberBundle());
                includeBldr.setFiberBundle(new ArrayList<>(fiberList));
            }
        }
        if (hard.getNodeId() != null && !hard.getNodeId().isEmpty()) {
            if (includeBldr.getNodeId() == null) {
                includeBldr.setNodeId(hard.getNodeId());
            } else {
                Set<NodeIdType> nodeIdList = new HashSet<>(includeBldr.getNodeId());
                nodeIdList.addAll(hard.getNodeId());
                includeBldr.setNodeId(new ArrayList<>(nodeIdList));
            }
        }
        if (hard.getSite() != null && !hard.getSite().isEmpty()) {
            if (includeBldr.getSite() == null) {
                includeBldr.setSite(hard.getSite());
            } else {
                Set<String> siteList = new HashSet<>(includeBldr.getSite());
                siteList.addAll(hard.getSite());
                includeBldr.setSite(new ArrayList<>(siteList));
            }
        }
        if (hard.getSrlgId() != null && !hard.getSrlgId().isEmpty()) {
            if (includeBldr.getSrlgId() == null) {
                includeBldr.setSrlgId(hard.getSrlgId());
            } else {
                Set<Uint32> srlgList = new HashSet<>(includeBldr.getSrlgId());
                srlgList.addAll(hard.getSrlgId());
                includeBldr.setSrlgId(new ArrayList<>(srlgList));
            }
        }
        if (hard.getSupportingServiceName() != null && !hard.getSupportingServiceName().isEmpty()) {
            if (includeBldr.getSupportingServiceName() == null) {
                includeBldr.setSupportingServiceName(hard.getSupportingServiceName());
            } else {
                Set<String> serviceList = new HashSet<>(includeBldr.getSupportingServiceName());
                serviceList.addAll(hard.getSupportingServiceName());
                includeBldr.setSupportingServiceName(new ArrayList<>(serviceList));
            }
        }
        if (hard.getLinkIdentifier() != null && !hard.getLinkIdentifier().isEmpty()) {
            if (includeBldr.getLinkIdentifier() == null) {
                includeBldr.setLinkIdentifier(hard.getLinkIdentifier());
            } else {
                Map<LinkIdentifierKey, LinkIdentifier> linkList = new HashMap<>(includeBldr.getLinkIdentifier());
                linkList.putAll(hard.getLinkIdentifier());
                includeBldr.setLinkIdentifier(linkList);
            }
        }
        return includeBldr.build();
    }

    private static Exclude updateExclude(Exclude hard, Exclude soft) {
        ExcludeBuilder excludeBldr = soft == null ? new ExcludeBuilder() : new ExcludeBuilder(soft);

        if (hard.getFiberBundle() != null && !hard.getFiberBundle().isEmpty()) {
            if (excludeBldr.getFiberBundle() == null) {
                excludeBldr.setFiberBundle(hard.getFiberBundle());
            } else {
                Set<String> fiberList = new HashSet<>(excludeBldr.getFiberBundle());
                fiberList.addAll(hard.getFiberBundle());
                excludeBldr.setFiberBundle(new ArrayList<>(fiberList));
            }
        }
        if (hard.getNodeId() != null && !hard.getNodeId().isEmpty()) {
            if (excludeBldr.getNodeId() == null) {
                excludeBldr.setNodeId(hard.getNodeId());
            } else {
                Set<NodeIdType> nodeIdList = new HashSet<>(excludeBldr.getNodeId());
                nodeIdList.addAll(hard.getNodeId());
                excludeBldr.setNodeId(new ArrayList<>(nodeIdList));
            }
        }
        if (hard.getSite() != null && !hard.getSite().isEmpty()) {
            if (excludeBldr.getSite() == null) {
                excludeBldr.setSite(hard.getSite());
            } else {
                Set<String> siteList = new HashSet<>(excludeBldr.getSite());
                siteList.addAll(hard.getSite());
                excludeBldr.setSite(new ArrayList<>(siteList));
            }
        }
        if (hard.getSrlgId() != null && !hard.getSrlgId().isEmpty()) {
            if (excludeBldr.getSrlgId() == null) {
                excludeBldr.setSrlgId(hard.getSrlgId());
            } else {
                Set<Uint32> srlgList = new HashSet<>(excludeBldr.getSrlgId());
                srlgList.addAll(hard.getSrlgId());
                excludeBldr.setSrlgId(new ArrayList<>(srlgList));
            }
        }
        if (hard.getSupportingServiceName() != null && !hard.getSupportingServiceName().isEmpty()) {
            if (excludeBldr.getSupportingServiceName() == null) {
                excludeBldr.setSupportingServiceName(hard.getSupportingServiceName());
            } else {
                Set<String> serviceList = new HashSet<>(excludeBldr.getSupportingServiceName());
                serviceList.addAll(hard.getSupportingServiceName());
                excludeBldr.setSupportingServiceName(new ArrayList<>(serviceList));
            }
        }
        if (hard.getLinkIdentifier() != null && !hard.getLinkIdentifier().isEmpty()) {
            if (excludeBldr.getLinkIdentifier() == null) {
                excludeBldr.setLinkIdentifier(hard.getLinkIdentifier());
            } else {
                Map<LinkIdentifierKey, LinkIdentifier> linkList = new HashMap<>(excludeBldr.getLinkIdentifier());
                linkList.putAll(hard.getLinkIdentifier());
                excludeBldr.setLinkIdentifier(linkList);
            }
        }
        return excludeBldr.build();
    }

    private static Diversity updateDiveristy(Diversity hard, Diversity soft) {
        DiversityBuilder diversityBldr = soft == null ? new DiversityBuilder() : new DiversityBuilder(soft);

        Map<
            org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                    .constraints.ServiceIdentifierListKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                    .constraints.ServiceIdentifierList> sil =
                diversityBldr.getServiceIdentifierList() == null
                    ? new HashMap<>()
                    : new HashMap<>(diversityBldr.getServiceIdentifierList());
        if (!hard.getServiceIdentifierList().isEmpty()) {
            sil.putAll(hard.getServiceIdentifierList());
            diversityBldr.setServiceIdentifierList(sil);
        }
        if (hard.getDiversityType() != null) {
            diversityBldr.setDiversityType(hard.getDiversityType());
        }
        return diversityBldr.build();
    }

    private static Latency updateLatency(Latency hard, Latency soft) {
        return soft == null || hard.getMaxLatency().longValue() <= soft.getMaxLatency().longValue()
            ? new LatencyBuilder(hard).build()
            : new LatencyBuilder(soft).build();
    }

    private static Distance updateDistance(Distance hard, Distance soft) {
        return soft == null || hard.getMaxDistance().longValue() <= soft.getMaxDistance().longValue()
            ? new DistanceBuilder(hard).build()
            : new DistanceBuilder(soft).build();
    }

    private static HopCount updateHopCount(HopCount hard, HopCount soft) {
        if (soft == null) {
            return new HopCountBuilder(hard).build();
        }
        HopCountBuilder hcBldr = new HopCountBuilder();
        if (soft.getMaxWdmHopCount() == null) {
            if (hard.getMaxWdmHopCount() != null) {
                hcBldr.setMaxWdmHopCount(hard.getMaxWdmHopCount());
            }
        } else {
            hcBldr.setMaxWdmHopCount(
                hard.getMaxWdmHopCount() == null
                        || soft.getMaxWdmHopCount().intValue() <= hard.getMaxWdmHopCount().intValue()
                    ? soft.getMaxWdmHopCount()
                    : hard.getMaxWdmHopCount());
        }
        if (soft.getMaxOtnHopCount() == null) {
            if (hard.getMaxOtnHopCount() != null) {
                hcBldr.setMaxOtnHopCount(hard.getMaxOtnHopCount());
            }
        } else {
            hcBldr.setMaxOtnHopCount(
                hard.getMaxOtnHopCount() == null
                        || soft.getMaxOtnHopCount().intValue() <= hard.getMaxOtnHopCount().intValue()
                    ? soft.getMaxOtnHopCount()
                    : hard.getMaxOtnHopCount());
        }
        return hcBldr.build();
    }

    private static TEMetric updateTEMetric(TEMetric hard, TEMetric soft) {
        if (soft == null) {
            return new TEMetricBuilder(hard).build();
        }
        TEMetricBuilder temBldr = new TEMetricBuilder();
        if (soft.getMaxWdmTEMetric() == null) {
            if (hard.getMaxWdmTEMetric() != null) {
                temBldr.setMaxWdmTEMetric(hard.getMaxWdmTEMetric());
            }
        } else {
            temBldr.setMaxWdmTEMetric(
                hard.getMaxWdmTEMetric() == null
                        || soft.getMaxWdmTEMetric().intValue() <= hard.getMaxWdmTEMetric().intValue()
                    ? soft.getMaxWdmTEMetric()
                    : hard.getMaxWdmTEMetric());
        }
        if (soft.getMaxOtnTEMetric() == null) {
            if (hard.getMaxOtnTEMetric() != null) {
                temBldr.setMaxOtnTEMetric(hard.getMaxWdmTEMetric());
            }
        } else {
            temBldr.setMaxOtnTEMetric(
                hard.getMaxOtnTEMetric() == null
                        || soft.getMaxOtnTEMetric().intValue() <= hard.getMaxOtnTEMetric().intValue()
                    ? soft.getMaxOtnTEMetric()
                    : hard.getMaxOtnTEMetric());
        }
        return temBldr.build();
    }

    private static CoRouting updateCoRouting(CoRouting hard, CoRouting soft) {
        CoRoutingBuilder coRoutingBldr = soft == null ? new CoRoutingBuilder() : new CoRoutingBuilder(soft);

        Map<
            ServiceIdentifierListKey,
            ServiceIdentifierList> serviceIdentifierList = coRoutingBldr.getServiceIdentifierList() == null
                ? new HashMap<>()
                : new HashMap<>(coRoutingBldr.getServiceIdentifierList());
        if (!hard.getServiceIdentifierList().isEmpty()) {
            serviceIdentifierList.putAll(hard.getServiceIdentifierList());
            coRoutingBldr.setServiceIdentifierList(serviceIdentifierList);
        }
        return coRoutingBldr.build();
    }

    /**
     * Remove all hard constraints except latency.
     * @param  hardConstraints HardConstarints to be downgraded
     * @return                 HardConstraints downgraded
     */
    public static HardConstraints downgradeHardConstraints(HardConstraints hardConstraints) {
        HardConstraintsBuilder hardConstraintsBuilder = new HardConstraintsBuilder();
        if (hardConstraints != null && hardConstraints.getLatency() != null) {
            hardConstraintsBuilder.setLatency(hardConstraints.getLatency());
        } else {
            LOG.warn("latency value not found in HardContraints !");
        }
        return hardConstraintsBuilder.build();
    }

    /**
     * Convert HardConstraints to SoftConstraints.
     * @param  hardConstraints to be converted.
     * @return                 SoftConstraints converted.
     */
    public static SoftConstraints convertToSoftConstraints(HardConstraints hardConstraints) {
        return hardConstraints == null
            ? new SoftConstraintsBuilder().build()
            : new SoftConstraintsBuilder(hardConstraints).build();
    }
}
