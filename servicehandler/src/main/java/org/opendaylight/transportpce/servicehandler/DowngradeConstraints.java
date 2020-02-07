/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.CoRoutingOrGeneral;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.General;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.GeneralBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.Diversity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.general.Latency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.routing.constraints.SoftConstraintsBuilder;
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
        List<String> hardCustomerCode = hardConstraints.getCustomerCode();
        if (!hardCustomerCode.isEmpty()) {
            softConstraintsBuilder.getCustomerCode().addAll(hardCustomerCode);
        } else {
            LOG.warn("hard constraints customer code list is empty !");
        }
        CoRoutingOrGeneral coRoutingOrGeneral = hardConstraints.getCoRoutingOrGeneral();
        if (coRoutingOrGeneral != null) {
            if (coRoutingOrGeneral instanceof General) {
                General hardGeneral = (General) coRoutingOrGeneral;
                if (softConstraintsBuilder.getCoRoutingOrGeneral() instanceof General) {
                    General softGeneral = (General) softConstraintsBuilder.getCoRoutingOrGeneral();
                    updateGeneral(hardGeneral, softGeneral);
                } else {
                    softConstraintsBuilder.setCoRoutingOrGeneral(hardGeneral);
                }
            } else if (coRoutingOrGeneral instanceof CoRouting) {
                CoRouting hardCoRouting = (CoRouting) coRoutingOrGeneral;
                if (softConstraintsBuilder.getCoRoutingOrGeneral() instanceof CoRouting) {
                    CoRouting softCoRouting = (CoRouting) softConstraintsBuilder.getCoRoutingOrGeneral();
                    updateCoRouting(hardCoRouting, softCoRouting);
                } else {
                    softConstraintsBuilder.setCoRoutingOrGeneral(hardCoRouting);
                }
            }
        } else {
            LOG.warn("hard constraints CoRoutingOrGeneral is null !");
        }
        return softConstraintsBuilder.build();
    }

    private static General updateGeneral(General hard, General soft) {
        GeneralBuilder result = new GeneralBuilder(soft);
        try {
            result.setDiversity(updateDiveristy(hard.getDiversity(), soft.getDiversity()));
            result.setExclude(updateExclude(hard.getExclude(), soft.getExclude()));
            result.setInclude(updateInclude(hard.getInclude(), soft.getInclude()));
        } catch (NullPointerException e) {
            LOG.warn("failed to update some 'General' parameters !", e);
        }
        return result.build();
    }

    private static Include updateInclude(Include hard, Include soft) {
        IncludeBuilder result = new IncludeBuilder(soft);
        if (hard != null) {
            result.getFiberBundle().addAll(hard.getFiberBundle());
            result.getNodeId().addAll(hard.getNodeId());
            result.getSite().addAll(hard.getSite());
            result.getSupportingServiceName().addAll(hard.getSupportingServiceName());
        }
        return result.build();
    }

    private static Exclude updateExclude(Exclude hard, Exclude soft) {
        ExcludeBuilder result = new ExcludeBuilder(soft);
        if (hard != null) {
            result.getFiberBundle().addAll(hard.getFiberBundle());
            result.getNodeId().addAll(hard.getNodeId());
            result.getSite().addAll(hard.getSite());
            result.getSupportingServiceName().addAll(hard.getSupportingServiceName());
        }
        return result.build();
    }

    private static Diversity updateDiveristy(Diversity hard, Diversity soft) {
        DiversityBuilder result = new DiversityBuilder(soft);
        if (hard != null) {
            result.getExistingService().addAll(hard.getExistingService());
            result.setExistingServiceApplicability(hard.getExistingServiceApplicability());
        }
        return result.build();
    }

    private static CoRouting updateCoRouting(CoRouting hard, CoRouting soft) {
        CoRoutingBuilder result = new CoRoutingBuilder(soft);
        try {
            result.setCoRouting(
                    updateCoCoRouting(hard.getCoRouting(), soft.getCoRouting()));
        } catch (NullPointerException e) {
            LOG.warn("failed to update some 'CoRouting' parameters !", e);
        }
        return result.build();
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing
        .or.general.co.routing.CoRouting updateCoCoRouting(org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                .constrains.rev191129.constraints.co.routing.or.general.co.routing.CoRouting hard, org.opendaylight
                    .yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general.co
                        .routing.CoRouting soft) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev191129.constraints.co.routing.or.general
            .co.routing.CoRoutingBuilder result = new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                .constrains.rev191129.constraints.co.routing.or.general.co.routing.CoRoutingBuilder(soft);
        if (hard != null) {
            result.getExistingService().addAll(hard.getExistingService());
        }
        return result.build();
    }

    /**
     * Remove all hard constraints except latency.
     *
     * @param hardConstraints HardConstarints to be downgraded
     * @return HardConstraints downgraded
     */
    public static HardConstraints downgradeHardConstraints(HardConstraints hardConstraints) {
        HardConstraintsBuilder hardConstraintsBuilder = new HardConstraintsBuilder();
        CoRoutingOrGeneral coRoutingOrGeneral = hardConstraints.getCoRoutingOrGeneral();
        Latency latency = null;
        if (coRoutingOrGeneral instanceof General) {
            General general = (General) coRoutingOrGeneral;
            if (general != null) {
                latency = general.getLatency();
                if (latency != null) {
                    hardConstraintsBuilder.setCoRoutingOrGeneral(new GeneralBuilder().setLatency(latency).build());
                } else {
                    LOG.warn("latency value not found in HardContraints !");
                }
            }
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
