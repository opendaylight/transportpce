/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Diversity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.Latency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.LatencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.constraints.co.routing.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to Map Hard Constraints to Soft Constraints.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public final class DowngradeConstraints {

    private static final Logger LOG = LoggerFactory.getLogger(DowngradeConstraints.class);

    private DowngradeConstraints() {
    }

    /**
     * Add Hard Constraints to Soft Constraints.
     *
     *
     * @param hardConstraints to be added
     * @param softConstraints to be modified
     * @return SoftConstraints modified
     */
    public static SoftConstraints updateSoftConstraints(HardConstraints hardConstraints,
            SoftConstraints softConstraints) {
        SoftConstraintsBuilder softConstraintsBuilder = new SoftConstraintsBuilder(softConstraints);
        if (hardConstraints.getCustomerCode() != null) {
            if (!hardConstraints.getCustomerCode().isEmpty()) {
                softConstraintsBuilder.getCustomerCode().addAll(hardConstraints.getCustomerCode());
            }
        }
        if (hardConstraints.getOperationalMode() != null) {
            if (!hardConstraints.getOperationalMode().isEmpty()) {
                softConstraintsBuilder.getOperationalMode().addAll(hardConstraints.getOperationalMode());
            }
        }
        if (hardConstraints.getDiversity() != null) {
            if (softConstraints.getDiversity() != null) {
                softConstraintsBuilder
                    .setDiversity(updateDiveristy(hardConstraints.getDiversity(), softConstraints.getDiversity()));
            }
        }
        if (hardConstraints.getExclude() != null) {
            if (softConstraints.getExclude() != null) {
                softConstraintsBuilder
                    .setExclude(updateExclude(hardConstraints.getExclude(), softConstraints.getExclude()));
            }
        }
        if (hardConstraints.getInclude() != null) {
            if (softConstraints.getInclude() != null) {
                softConstraintsBuilder
                    .setInclude(updateInclude(hardConstraints.getInclude(), softConstraints.getInclude()));
            }
        }
        if (hardConstraints.getLatency() != null) {
            if (softConstraints.getLatency() != null) {
                softConstraintsBuilder
                    .setLatency(updateLatency(hardConstraints.getLatency(), softConstraints.getLatency()));
            }
        }
        if (hardConstraints.getCoRouting() != null) {
            if (softConstraints.getCoRouting() != null) {
                softConstraintsBuilder
                    .setCoRouting(updateCoRouting(hardConstraints.getCoRouting(), softConstraints.getCoRouting()));
            }
        }
        return softConstraintsBuilder.build();
    }

    private static Include updateInclude(Include hard, Include soft) {
        IncludeBuilder includeBldr = new IncludeBuilder(soft);
        includeBldr.getFiberBundle().addAll(hard.getFiberBundle());
        includeBldr.getNodeId().addAll(hard.getNodeId());
        includeBldr.getSite().addAll(hard.getSite());
        includeBldr.getSupportingServiceName().addAll(hard.getSupportingServiceName());
        return includeBldr.build();
    }

    private static Exclude updateExclude(Exclude hard, Exclude soft) {
        ExcludeBuilder excludeBldr = new ExcludeBuilder(soft);
        excludeBldr.getFiberBundle().addAll(hard.getFiberBundle());
        excludeBldr.getNodeId().addAll(hard.getNodeId());
        excludeBldr.getSite().addAll(hard.getSite());
        excludeBldr.getSupportingServiceName().addAll(hard.getSupportingServiceName());
        return excludeBldr.build();
    }

    private static Diversity updateDiveristy(Diversity hard, Diversity soft) {
        DiversityBuilder diversityBldr = new DiversityBuilder(soft);
        if (!hard.getServiceIdentifierList().isEmpty()) {
            Map<org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                .constraints.ServiceIdentifierListKey,
                org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.diversity.existing.service
                    .constraints.ServiceIdentifierList> sil = new HashMap<>(diversityBldr.getServiceIdentifierList());
            sil.putAll(hard.getServiceIdentifierList());
            diversityBldr.setServiceIdentifierList(sil);
        }
        return diversityBldr.build();
    }

    private static Latency updateLatency(Latency hard, Latency soft) {
        LatencyBuilder latencyBldr = new LatencyBuilder(soft);
        if (hard.getMaxLatency() != null) {
            latencyBldr.setMaxLatency(hard.getMaxLatency());
        }
        return latencyBldr.build();
    }

    private static CoRouting updateCoRouting(CoRouting hard, CoRouting soft) {
        CoRoutingBuilder coRoutingBldr = new CoRoutingBuilder(soft);
        Map<ServiceIdentifierListKey, ServiceIdentifierList> serviceIdentifierList
            = new HashMap<ServiceIdentifierListKey, ServiceIdentifierList>(coRoutingBldr.getServiceIdentifierList());
        serviceIdentifierList.putAll(hard.getServiceIdentifierList());
        return coRoutingBldr
            .setServiceIdentifierList(serviceIdentifierList)
            .build();
    }

    /**
     * Remove all hard constraints except latency.
     *
     * @param hardConstraints HardConstarints to be downgraded
     * @return HardConstraints downgraded
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
     *
     * @param hardConstraints to be converted.
     * @return SoftConstraints converted.
     */
    public static SoftConstraints convertToSoftConstraints(HardConstraints hardConstraints) {
        SoftConstraintsBuilder softConstraintsBuilder = new SoftConstraintsBuilder(hardConstraints);
        return softConstraintsBuilder.build();
    }
}
