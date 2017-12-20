/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;
//import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.node.NodeIdentifier;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.RoutingConstraintsSp.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.CoRoutingOrGeneral;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.General;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.general.Diversity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.general.Exclude;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.general.Latency;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceConstraintsCalc {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);

    private PceConstraints pceHardConstraints = new PceConstraints();
    private PceConstraints pceSoftConstraints = new PceConstraints();
    private PceMetric pceMetrics = PceMetric.HopCount;
    private DataBroker dataBroker;


    public PceConstraintsCalc(PathComputationRequestInput input, DataBroker dataBroker) {
        LOG.debug("In PceconstraintsCalc start");

        this.pceMetrics = input.getPceMetric();

        this.dataBroker = dataBroker;

        // TODO. for now metrics are set into hard structure
        LOG.info("In PceConstraintsCalc: read PceMetric {}", this.pceMetrics.toString());
        this.pceHardConstraints.setPceMetrics(this.pceMetrics);

        calcHardconstraints(input);
        calcSoftconstraints(input);
    }

    private void calcHardconstraints(PathComputationRequestInput input) {
        HardConstraints servicePathHardConstraints = input.getHardConstraints();
        if (servicePathHardConstraints == null) {
            LOG.info("In calcHardconstraints: no hard constraints.");
            return;
        }

        CoRoutingOrGeneral coRoutingOrGeneral = servicePathHardConstraints.getCoRoutingOrGeneral();
        readconstraints(coRoutingOrGeneral, this.pceHardConstraints);

    }

    private void calcSoftconstraints(PathComputationRequestInput input) {
        SoftConstraints servicePathSoftConstraints = input.getSoftConstraints();
        if (servicePathSoftConstraints == null) {
            LOG.info("In calcSoftconstraints: no soft constraints.");
            return;
        }

        CoRoutingOrGeneral coRoutingOrGeneral = servicePathSoftConstraints.getCoRoutingOrGeneral();
        readconstraints(coRoutingOrGeneral, this.pceSoftConstraints);

    }

    private void readconstraints(CoRoutingOrGeneral coRoutingOrGeneral, PceConstraints constraints) {
        LOG.debug("In readconstraints start");

        if (coRoutingOrGeneral == null) {
            LOG.info("In readHardconstraints: no CoRoutingOrGeneral constraints.");
            return;
        }

        General tmpGeneral = null;
        CoRouting tmpCoRouting = null;

        if (coRoutingOrGeneral instanceof General) {
            LOG.info("In readconstraints General {}", coRoutingOrGeneral.toString());
            tmpGeneral = (General) coRoutingOrGeneral;
            readGeneralContrains(tmpGeneral, constraints);
            return;
        }

        if (coRoutingOrGeneral instanceof CoRouting) {
            LOG.info("In readconstraints CoRouting {}", coRoutingOrGeneral.toString());
            tmpCoRouting = (CoRouting) coRoutingOrGeneral;
            readCoRoutingContrains(tmpCoRouting, constraints);
            return;
        }

        return;

    }

    private void readGeneralContrains(General tmpGeneral, PceConstraints constraints) {
        LOG.debug("In readGeneralContrains start");

        if (tmpGeneral  ==  null) {
            LOG.info("In readGeneralContrains: no General constraints.");
            return;
        }

        Latency latency = tmpGeneral.getLatency();
        if (latency != null) {
            constraints.setMaxLatency(latency.getMaxLatency());
            LOG.info("In readGeneralContrains: read latency {}", latency.toString());
        }

        Exclude exclude = tmpGeneral.getExclude();
        List<String> elementsToExclude = null;
        if (exclude != null) {
            elementsToExclude = exclude.getNodeId();
            if (elementsToExclude != null) {
                constraints.setExcludeNodes(elementsToExclude);
            }
            elementsToExclude = exclude.getSRLG();
            if (elementsToExclude != null) {
                constraints.setExcludeSRLG(elementsToExclude);
            }
        }

        Diversity diversity = tmpGeneral.getDiversity();
        if (diversity != null) {
            if (diversity.getExistingServiceApplicability().isNode()) {
                LOG.info("in readGeneralContrains {}", diversity.toString());
                readDiversityNodes(diversity.getExistingService(), constraints);
            }
        }

    }

    private void readDiversityNodes(List<String> srvList, PceConstraints constraints) {

        List<String> elementsToExclude = new ArrayList<String>();
        LOG.info("in readDiversityNodes {}", srvList.toString());

        for (String srv : srvList) {
            Optional<PathDescription> service = getPathDescriptionFromDatastore(srv);
            if (service.isPresent()) {
                elementsToExclude.addAll(getAToZNodeList(service.get()));
                LOG.info("readDiversityNodes : {}", elementsToExclude);

            } else {
                LOG.info("in readDiversityNodes srv={} is not present", srv);
            }
        }

        if (elementsToExclude != null) {
            constraints.setExcludeNodes(elementsToExclude);
        }
    }

    private List<String> getAToZNodeList(PathDescription pathDescription) {
        List<AToZ> atozList = pathDescription.getAToZDirection().getAToZ();
        return atozList.stream().filter(aToZ -> {
            if ((aToZ.getResource() == null) || (aToZ.getResource().getResource() == null)) {
                LOG.warn("Diversity constraint: Resource of AToZ node {} is null! Skipping this node!", aToZ.getId());
                return false;
            }
            return aToZ.getResource().getResource() instanceof Node;
        }).filter(aToZ -> {
            Node node = (Node) aToZ.getResource().getResource();
            if (node.getNodeIdentifier() == null) {
                LOG.warn("Node in AToZ node {} contains null! Skipping this node!", aToZ.getId());
                return false;
            }
            return true;
        }).map(aToZ -> {
            NodeIdentifier node = ((Node) aToZ.getResource().getResource()).getNodeIdentifier();
            return node.getNodeId().toString();
        }).collect(Collectors.toList());
    }

    private Optional<PathDescription> getPathDescriptionFromDatastore(String serviceName) {
        InstanceIdentifier<PathDescription> pathDescriptionIID = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName)).child(PathDescription.class);
        ReadOnlyTransaction pathDescReadTx = this.dataBroker.newReadOnlyTransaction();
        try {
            LOG.info("PCE diversity constraints: Getting path description for service {}", serviceName);
            return pathDescReadTx.read(LogicalDatastoreType.CONFIGURATION, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(
                "PCE diversity constraints: Exception while getting path description from datastore {} for service {}!",
                pathDescriptionIID,serviceName, e);
            return Optional.absent();
        }
    }

    private void readCoRoutingContrains(CoRouting tmpcoRouting, PceConstraints constraints) {
        LOG.info("In readCoRoutingContrains start");

        if (tmpcoRouting  ==  null) {
            LOG.info("In readCoRoutingContrains: no General constraints.");
            return;
        }

    }

    public PceConstraints getPceHardConstraints() {
        return this.pceHardConstraints;
    }

    public PceConstraints getPceSoftConstraints() {
        return this.pceSoftConstraints;
    }

    public PceMetric getPceMetrics() {
        return this.pceMetrics;
    }


}
