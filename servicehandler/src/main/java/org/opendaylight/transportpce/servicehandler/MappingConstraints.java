/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.servicehandler;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.Constraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.CoRoutingOrGeneral;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.General;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Diversity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Latency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.ConstraintsSp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.co.routing.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.DiversityBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.ExcludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.LatencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.diversity.existing.service.contraints.sp.ExistingServiceApplicabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.SoftConstraintsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for mapping
 * Hard/soft constraint from Service 1.2
 * to servicePath 1.4.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class MappingConstraints {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MappingConstraints.class);
    private HardConstraints serviceHardConstraints;
    private SoftConstraints serviceSoftConstraints;
    private org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
        .routing.constraints.rev171017.routing.constraints.sp.HardConstraints servicePathHardConstraints;
    private org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
        .routing.constraints.rev171017.routing.constraints.sp.SoftConstraints servicePathSoftConstraints;

    /**
     * MappingConstraints class constructor
     * for hard/soft from service 1.2.
     *
     * @param hard HardConstraints
     * @param soft SoftConstraints
     */
    public MappingConstraints(HardConstraints hard, SoftConstraints soft) {
        setServiceHardConstraints(hard);
        setServiceSoftConstraints(soft);
    }

    /**
     * MappingConstraints class constructor
     * for hard/soft from servicePath 1.4.
     *
     * @param hard HardConstraints
     * @param soft SoftConstraints
     */
    public MappingConstraints(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev171017.routing.constraints.sp.HardConstraints hard,
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev171017.routing.constraints.sp.SoftConstraints soft) {
        setServicePathHardConstraints(hard);
        setServicePathSoftConstraints(soft);
    }

    /**
     *get all constraints informations
     *from service constraints to map to
     *servicePath constraints.
     *
     * @param input HardConstraints or SoftConstraints
     * @return ConstraintsSp HardConstraintsSp or HardConstraintsSp
     */
    private <T> ConstraintsSp getConstraints(T input) {
        CoRoutingOrGeneral coRoutingOrGeneral = null;
        HardConstraintsBuilder tempHard = new HardConstraintsBuilder();
        SoftConstraintsBuilder tempSoft = new SoftConstraintsBuilder();
        if ((input  !=  null) && (input instanceof Constraints)) {
            Constraints constraints = (Constraints)input;
            coRoutingOrGeneral = constraints.getCoRoutingOrGeneral();
            General tmpGeneral = null;
            CoRouting tmpCoRouting = null;
            if (coRoutingOrGeneral  !=  null) {
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                    .rev171017.constraints.sp.co.routing.or.general.GeneralBuilder finalGeneral =
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                    .rev171017.constraints.sp.co.routing.or.general.GeneralBuilder();
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                    .rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder finalCoRouting =
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                    .rev171017.constraints.sp.co.routing.or.general.CoRoutingBuilder();
                if (coRoutingOrGeneral instanceof General) {
                    tmpGeneral = (General) coRoutingOrGeneral;
                    if (tmpGeneral  !=  null) {
                        Diversity tmpDiversity =  tmpGeneral.getDiversity();
                        if (tmpDiversity  !=  null) {
                            finalGeneral.setDiversity(
                                    new DiversityBuilder()
                                    .setExistingService(tmpDiversity.getExistingService())
                                    .setExistingServiceApplicability(
                                            new ExistingServiceApplicabilityBuilder()
                                            .setClli(tmpDiversity.getExistingServiceApplicability().isSite())
                                            .setNode(tmpDiversity.getExistingServiceApplicability().isNode())
                                            .setSrlg(tmpDiversity.getExistingServiceApplicability().isSrlg())
                                            .build())
                                    .build());
                        }
                        Exclude tmpExclude = tmpGeneral.getExclude();
                        if (tmpExclude  !=  null) {
                            List<String> nodeIdList = new ArrayList<>();
                            for (NodeIdType nodeId : tmpExclude.getNodeId()) {
                                nodeIdList.add(nodeId.getValue());
                            }
                            finalGeneral.setExclude(
                                    new ExcludeBuilder()
                                    .setSupportingServiceName(tmpExclude.getSupportingServiceName())
                                    .setClli(tmpExclude.getSite())
                                    .setNodeId(nodeIdList)
                                    //.setAffinity(value)
                                    //.setSRLG(value)
                                    .build());
                        }
                        Include tmpInclude = tmpGeneral.getInclude();
                        if (tmpInclude  !=  null) {
                            finalGeneral.setInclude(
                                    new IncludeBuilder()
                                    //.setOrderedHops()
                                    .build());
                        }
                        Latency tmpLatency = tmpGeneral.getLatency();
                        if (tmpLatency != null) {
                            finalGeneral.setLatency(
                                    new LatencyBuilder()
                                    .setMaxLatency(tmpLatency.getMaxLatency())
                                    .build());
                        }
                    }
                    tempHard.setCoRoutingOrGeneral(finalGeneral.build())
                        .setCustomerCode(constraints.getCustomerCode());
                    tempSoft.setCoRoutingOrGeneral(finalGeneral.build())
                        .setCustomerCode(constraints.getCustomerCode());
                } else if (coRoutingOrGeneral instanceof CoRouting) {
                    tmpCoRouting = (CoRouting)coRoutingOrGeneral;
                    if (tmpCoRouting  !=  null) {
                        org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329
                            .constraints.co.routing.or.general.co.routing.CoRouting tmpCoRoutingCoRouting =
                            tmpCoRouting.getCoRouting();
                        if (tmpCoRoutingCoRouting  !=  null) {
                            finalCoRouting.setCoRouting(
                                    new CoRoutingBuilder()
                                    .setExistingService(tmpCoRoutingCoRouting.getExistingService())
                                    .build());
                        }
                    }
                    tempHard.setCoRoutingOrGeneral(finalCoRouting.build())
                        .setCustomerCode(constraints.getCustomerCode());
                    tempSoft.setCoRoutingOrGeneral(finalCoRouting.build())
                        .setCustomerCode(constraints.getCustomerCode());
                }
            }
        }
        if (input instanceof HardConstraints) {
            return tempHard.build();
        } else if (input instanceof SoftConstraints) {
            return tempSoft.build();
        } else {
            return null;
        }
    }

    /**
     * map hard/soft constraints from Service 1.2
     * to ServicePath 1.4.
     */
    public void serviceToServicePathConstarints() {
        LOG.info("Mapping Service Constraints to ServicePath Constraints");
        if (serviceHardConstraints  !=  null) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                .rev171017.routing.constraints.sp.HardConstraints tempHard = (org.opendaylight.yang.gen
                    .v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing
                    .constraints.sp.HardConstraints) getConstraints(serviceHardConstraints);
            if (tempHard != null) {
                servicePathHardConstraints = tempHard;
            }
        } else if (serviceSoftConstraints  !=  null) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints
                .rev171017.routing.constraints.sp.SoftConstraints tempSoft = (org.opendaylight.yang.gen
                    .v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing
                    .constraints.sp.SoftConstraints) getConstraints(serviceSoftConstraints);
            if (tempSoft != null) {
                servicePathSoftConstraints = tempSoft;
            }
        }
    }


    public HardConstraints getServiceHardConstraints() {
        return serviceHardConstraints;
    }


    public void setServiceHardConstraints(HardConstraints serviceHardConstraints) {
        this.serviceHardConstraints = serviceHardConstraints;
    }


    public SoftConstraints getServiceSoftConstraints() {
        return serviceSoftConstraints;
    }


    public void setServiceSoftConstraints(SoftConstraints serviceSoftConstraints) {
        this.serviceSoftConstraints = serviceSoftConstraints;
    }


    public org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
        .routing.constraints.rev171017.routing.constraints.sp.HardConstraints getServicePathHardConstraints() {
        return servicePathHardConstraints;
    }


    public void setServicePathHardConstraints(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev171017.routing.constraints.sp.HardConstraints servicePathHardConstraints) {
        this.servicePathHardConstraints = servicePathHardConstraints;
    }


    public org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
        .routing.constraints.rev171017.routing.constraints.sp.SoftConstraints getServicePathSoftConstraints() {
        return servicePathSoftConstraints;
    }


    public void setServicePathSoftConstraints(org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
            .routing.constraints.rev171017.routing.constraints.sp.SoftConstraints servicePathSoftConstraints) {
        this.servicePathSoftConstraints = servicePathSoftConstraints;
    }

}
