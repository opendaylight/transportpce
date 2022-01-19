/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.CoRoutingOrGeneral;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.General;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Diversity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.constraints.co.routing.or.general.general.Latency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.diversity.existing.service.constraints.ExistingServiceApplicability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceConstraintsCalc {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceConstraintsCalc.class);

    private PceConstraints pceHardConstraints = new PceConstraints();
    private PceConstraints pceSoftConstraints = new PceConstraints();
    private PceMetric pceMetrics = PceMetric.HopCount;
    private NetworkTransactionService networkTransactionService;

    public PceConstraintsCalc(PathComputationRequestInput input, NetworkTransactionService networkTransactionService) {
        LOG.debug("In PceconstraintsCalc start");

        pceMetrics = input.getPceRoutingMetric();

        this.networkTransactionService = networkTransactionService;

        // TODO. for now metrics are set into hard structure
        LOG.info("In PceConstraintsCalc: read PceMetric {}", pceMetrics);
        pceHardConstraints.setPceMetrics(pceMetrics);

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
        readconstraints(coRoutingOrGeneral, pceHardConstraints);

    }

    private void calcSoftconstraints(PathComputationRequestInput input) {
        SoftConstraints servicePathSoftConstraints = input.getSoftConstraints();
        if (servicePathSoftConstraints == null) {
            LOG.info("In calcSoftconstraints: no soft constraints.");
            return;
        }

        CoRoutingOrGeneral coRoutingOrGeneral = servicePathSoftConstraints.getCoRoutingOrGeneral();
        readconstraints(coRoutingOrGeneral, pceSoftConstraints);

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
            LOG.info("In readconstraints General {}", coRoutingOrGeneral);
            tmpGeneral = (General) coRoutingOrGeneral;
            readGeneralContrains(tmpGeneral, constraints);
            return;
        }

        if (coRoutingOrGeneral instanceof CoRouting) {
            LOG.info("In readconstraints CoRouting {}", coRoutingOrGeneral);
            tmpCoRouting = (CoRouting) coRoutingOrGeneral;
            readCoRoutingContrains(tmpCoRouting, constraints);
        }
    }

    private void readGeneralContrains(General tmpGeneral, PceConstraints constraints) {
        LOG.debug("In readGeneralContrains start");

        if (tmpGeneral == null) {
            LOG.info("In readGeneralContrains: no General constraints.");
            return;
        }

        Latency latency = tmpGeneral.getLatency();
        if (latency != null) {
            constraints.setMaxLatency(latency.getMaxLatency().toJava());
            LOG.info("In readGeneralContrains: read latency {}", latency);
        }

        Exclude exclude = tmpGeneral.getExclude();
        if (exclude != null) {
            readExclude(exclude, constraints);
        }

        Include include = tmpGeneral.getInclude();
        if (include != null) {
            readInclude(include, constraints);
        }

        Diversity diversity = tmpGeneral.getDiversity();
        PceConstraints.ResourceType rt = PceConstraints.ResourceType.NONE;
        if (diversity != null) {
            ExistingServiceApplicability temp = diversity.getExistingServiceApplicability();
            if (temp == null) {
                return;
            }
            if (Boolean.TRUE.equals(temp.getNode())) {
                rt = PceConstraints.ResourceType.NODE;
            }
            if (Boolean.TRUE.equals(temp.getSrlg())) {
                rt = PceConstraints.ResourceType.SRLG;
            }
            if (Boolean.TRUE.equals(temp.getSite())) {
                rt = PceConstraints.ResourceType.CLLI;
            }
            LOG.info("in readGeneralContrains {} list is :{}", rt, diversity);
            readDiversity(diversity.getExistingService(), constraints, rt);
        }

    }

    private void readExclude(Exclude exclude, PceConstraints constraints) {
        List<NodeIdType> nodes = exclude.getNodeId();
        if (nodes != null) {
            List<String> elementsToExclude = new ArrayList<>();
            for (NodeIdType node : nodes) {
                elementsToExclude.add(node.getValue());
            }
            constraints.setExcludeSupNodes(elementsToExclude);
        }
        List<Uint32> srlgs = exclude.getSrlgId();
        if (srlgs != null) {
            List<Long> elementsToExclude = new ArrayList<>();
            for (Uint32 srlg : srlgs) {
                elementsToExclude.add(srlg.longValue());
            }
            constraints.setExcludeSRLG(elementsToExclude);
        }
        List<String> sites = exclude.getSite();
        if (sites != null) {
            constraints.setExcludeCLLI(exclude.getSite());
        }
    }

    private void readInclude(Include include, PceConstraints constraints) {
        List<NodeIdType> nodes = include.getNodeId();
        if (nodes != null) {
            for (NodeIdType node : nodes) {
                constraints.setListToInclude(new ResourcePair(PceConstraints.ResourceType.NODE, node.getValue()));
            }
        }
        List<Uint32> srlgs = include.getSrlgId();
        if (srlgs != null) {
            for (Uint32 srlg : srlgs) {
                constraints.setListToInclude(new ResourcePair(PceConstraints.ResourceType.SRLG, srlg.toString()));
            }
        }
        List<String> sites = include.getSite();
        if (sites != null) {
            for (String site : sites) {
                constraints.setListToInclude(new ResourcePair(PceConstraints.ResourceType.CLLI, site));
            }
        }
    }

    private void readDiversity(List<String> srvList, PceConstraints constraints, PceConstraints.ResourceType rt) {

        List<String> elementsToExclude = new ArrayList<>();
        LOG.info("in readDiversity {}", srvList);

        for (String srv : srvList) {
            Optional<PathDescription> service = getPathDescriptionFromDatastore(srv);
            if (service.isPresent()) {
                LOG.info("in readDiversity service list {}", service);
                switch (rt) {
                    case NODE:
                        elementsToExclude.addAll(getAToZNodeList(service.get()));
                        LOG.info("readDiversity NODE : {}", elementsToExclude);
                        if (elementsToExclude != null) {
                            constraints.setExcludeNodes(elementsToExclude);
                        }
                        break;
                    case SRLG:
                        elementsToExclude.addAll(getSRLGList(service.get()));
                        LOG.info("readDiversity SRLG : {}", elementsToExclude);
                        if (elementsToExclude != null) {
                            constraints.setExcludeSrlgLinks(elementsToExclude);
                        }
                        break;
                    case CLLI:
                        /// Retrieve nodes into dedicated CLLI list
                        /// during node validation check their CLLI and build CLLI exclude list
                        elementsToExclude.addAll(getAToZNodeList(service.get()));
                        LOG.info("readDiversity CLLI : {}", elementsToExclude);
                        if (elementsToExclude != null) {
                            constraints.setExcludeClliNodes(elementsToExclude);
                        }
                        break;
                    default:
                        LOG.info("in readDiversity unsupported divercity type {}", rt);
                }

            } else {
                LOG.info("in readDiversity srv={} is not present", srv);
            }
        }

    }

    private List<String> getAToZNodeList(PathDescription pathDescription) {
        List<AToZ> aendToZList = new ArrayList<>(pathDescription.getAToZDirection().nonnullAToZ().values());
        return aendToZList.stream().filter(aToZ -> {
            if (aToZ.getResource() == null || aToZ.getResource().getResource() == null) {
                LOG.warn("Diversity constraint: Resource of AToZ node {} is null! Skipping this node!", aToZ.getId());
                return false;
            }
            return aToZ.getResource().getResource() instanceof Node;
        }).filter(aToZ -> {
            Node node = (Node) aToZ.getResource().getResource();
            if (node.getNodeId() == null) {
                LOG.warn("Node in AToZ node {} contains null! Skipping this node!", aToZ.getId());
                return false;
            }
            return true;
        }).map(aToZ -> {
            Node node = ((Node) aToZ.getResource().getResource());
            return node.getNodeId();
        }).collect(Collectors.toList());
    }

    private List<String> getSRLGList(PathDescription pathDescription) {
        List<AToZ> aendToZList = new ArrayList<>(pathDescription.getAToZDirection().nonnullAToZ().values());
        return aendToZList.stream().filter(aToZ -> {
            if (aToZ.getResource() == null
                    || aToZ.getResource().getResource() == null) {
                LOG.warn("Diversity constraint: Resource of AToZ {} is null! Skipping this resource!", aToZ.getId());
                return false;
            }
            return aToZ.getResource().getResource() instanceof Link;
        }).filter(aToZ -> {
            Link link = (Link) aToZ.getResource().getResource();
            if (link.getLinkId() == null) {
                LOG.warn("Link in AToZ link {} contains null! Skipping this link!", aToZ.getId());
                return false;
            }
            return true;
        }).map(aToZ -> {
            return ((Link) aToZ.getResource().getResource()).getLinkId();
        }).collect(Collectors.toList());
    }

    private Optional<PathDescription> getPathDescriptionFromDatastore(String serviceName) {
        Optional<PathDescription> result = Optional.empty();
        InstanceIdentifier<ServicePaths> pathDescriptionIID = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName));
        try {
            LOG.info("PCE diversity constraints: Getting path description for service {}", serviceName);
            ServicePaths servicePaths =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS).get();
            if (servicePaths != null) {
                PathDescription path = servicePaths.getPathDescription();
                if (path != null) {
                    result = Optional.of(path);
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn(
                "PCE diversity constraints: Exception while getting path description from datastore {} for service {}!",
                pathDescriptionIID,serviceName, e);
            return result;
        }
        return result;
    }

    private void readCoRoutingContrains(CoRouting tmpcoRouting, PceConstraints constraints) {
        LOG.info("In readCoRoutingContrains start");

        if (tmpcoRouting == null) {
            LOG.info("In readCoRoutingContrains: no General constraints.");
        }

    }

    public PceConstraints getPceHardConstraints() {
        return pceHardConstraints;
    }

    public PceConstraints getPceSoftConstraints() {
        return pceSoftConstraints;
    }

    public PceMetric getPceMetrics() {
        return pceMetrics;
    }

}
