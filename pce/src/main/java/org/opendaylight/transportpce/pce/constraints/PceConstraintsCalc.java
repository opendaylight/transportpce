/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints.ResourcePair;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.Constraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.CoRouting;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.Diversity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.Exclude;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.Include;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.diversity.existing.service.constraints.ServiceIdentifierList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.diversity.existing.service.constraints.ServiceIdentifierListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.service.applicability.g.ServiceApplicability;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
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
        readConstraints(servicePathHardConstraints, pceHardConstraints);
    }

    private void calcSoftconstraints(PathComputationRequestInput input) {
        SoftConstraints servicePathSoftConstraints = input.getSoftConstraints();
        if (servicePathSoftConstraints == null) {
            LOG.info("In calcSoftconstraints: no soft constraints.");
            return;
        }
        readConstraints(servicePathSoftConstraints, pceSoftConstraints);
    }

    private void readConstraints(Constraints hardConstraints, PceConstraints constraints) {
        LOG.debug("In readHardconstraints start");

        if (hardConstraints.getInclude() != null) {
            readInclude(hardConstraints.getInclude(), constraints);
        }
        if (hardConstraints.getExclude() != null) {
            readExclude(hardConstraints.getExclude(), constraints);
        }
        if (hardConstraints.getCoRouting() != null) {
            readCoRouting(hardConstraints.getCoRouting(), constraints);
        }
        if (hardConstraints.getDiversity() != null) {
            readDiversity(hardConstraints.getDiversity(), constraints);
        }
        if (hardConstraints.getLatency() != null) {
            constraints.setMaxLatency(hardConstraints.getLatency().getMaxLatency().longValue());
        }
    }

    private void readExclude(Exclude exclude, PceConstraints constraints) {
        //TODO: Implement other exclude constraints: fiber-bundle, link-identifier
        // and supporting-service-name
        if (exclude.getNodeId() != null) {
            List<String> elementsToExclude = new ArrayList<>();
            for (NodeIdType node : exclude.getNodeId()) {
                elementsToExclude.add(node.getValue());
            }
            constraints.setExcludeSupNodes(elementsToExclude);
        }
        if (exclude.getSrlgId() != null) {
            List<Long> elementsToExclude = new ArrayList<>();
            for (Uint32 srlg : exclude.getSrlgId()) {
                elementsToExclude.add(srlg.longValue());
            }
            constraints.setExcludeSRLG(elementsToExclude);
        }
        if (exclude.getSite() != null) {
            constraints.setExcludeCLLI(new ArrayList<>(exclude.getSite()));
        }
        if (exclude.getFiberBundle() != null || exclude.getLinkIdentifier() != null
            || exclude.getSupportingServiceName() != null) {
            LOG.warn("exclude constraints of type fiber-bundle, link-identifier"
                + "or supporting-service-name are not implemented yet");
        }
    }

    private void readInclude(Include include, PceConstraints constraints) {
        if (include.getNodeId() != null) {
            for (NodeIdType node : include.getNodeId()) {
                constraints.setListToInclude(new ResourcePair(PceConstraints.ResourceType.NODE, node.getValue()));
            }
        }
        if (include.getSrlgId() != null) {
            for (Uint32 srlg : include.getSrlgId()) {
                constraints.setListToInclude(new ResourcePair(PceConstraints.ResourceType.SRLG, srlg.toString()));
            }
        }
        if (include.getSite() != null) {
            for (String site : include.getSite()) {
                constraints.setListToInclude(new ResourcePair(PceConstraints.ResourceType.CLLI, site));
            }
        }
    }

    private void readCoRouting(CoRouting tmpcoRouting, PceConstraints constraints) {
        if (tmpcoRouting == null) {
            LOG.info("In readCoRoutingContrains: no CoRouting constraints.");
        } else {
            LOG.warn("CoRouting constraints handling not implemented yet");
        }
    }

    private void readDiversity(Diversity diversity, PceConstraints constraints) {
        //TODO: How to implement the DiversityType: serial or synchronous?
        Map<ServiceIdentifierListKey, ServiceIdentifierList> serviceIdList = diversity.getServiceIdentifierList();
        Collection<ServiceIdentifierList> services = serviceIdList.values();
        for (ServiceIdentifierList serviceIdentifier : services) {
            String serviceId = serviceIdentifier.getServiceIdentifier();
            ServiceApplicability serviceApplicability = serviceIdentifier.getServiceApplicability();
            Optional<PathDescription> serviceOpt = getPathDescriptionFromDatastore(serviceId);
            if (serviceOpt.isPresent()) {
                List<String> serviceNodes = getAToZNodeList(serviceOpt.orElseThrow());
                if (serviceApplicability.getNode() && !serviceNodes.isEmpty()) {
                    constraints.setExcludeNodes(serviceNodes);
                }
                List<String> serviceLinks = getSRLGList(serviceOpt.orElseThrow());
                if (serviceApplicability.getLink() && !serviceLinks.isEmpty()) {
                    constraints.setExcludeSrlgLinks(serviceLinks);
                }
                if (serviceApplicability.getSite() && !serviceNodes.isEmpty()) {
                    constraints.setExcludeClliNodes(serviceNodes);
                }
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
        DataObjectIdentifier<ServicePaths> pathDescriptionIID = DataObjectIdentifier.builder(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName))
                .build();
        try {
            LOG.info("PCE diversity constraints: Getting path description for service {}", serviceName);
            ServicePaths servicePaths =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS).orElseThrow();
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
