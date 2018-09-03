/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubpce;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.stubpce.topology.PathDescriptionsOrdered;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathDescriptionList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.description.list.PathDescriptions;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.list.ServicePathsKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.General;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.general.Diversity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.constraints.sp.co.routing.or.general.general.Exclude;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.path.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.path.ServiceZEnd;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for Sending
 * PCE requests :
 * - path-computation-request
 * - cancel-resource-reserve.
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on behalf of Orange
 *
 */
public class SendingPceRPCs {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(SendingPceRPCs.class);
    private Boolean success;
    private PathComputationRequestInput input;
    private CancelResourceReserveInput cancelInput;
    private PathDescriptionBuilder pathDescription;
    private DataBroker db;
    private String error;
    private final ListeningExecutorService executor;
    private List<ServicePaths> servicePathList;

    public SendingPceRPCs() {
        success = true;
        setPathDescription(null);
        setError("");
        executor = null;
        setServicePathList(new ArrayList<ServicePaths>());
    }

    public SendingPceRPCs(PathComputationRequestInput input,DataBroker databroker,ListeningExecutorService executor) {
        success = true;
        setPathDescription(null);
        setInput(input);
        setCancelInput(null);
        setDb(databroker);
        setError("");
        servicePathList = readServicePathList();
        this.executor = executor;
    }

    public SendingPceRPCs(CancelResourceReserveInput input,DataBroker databroker,ListeningExecutorService executor) {
        success = true;
        setPathDescription(null);
        setInput(null);
        setCancelInput(input);
        setDb(databroker);
        setError("");
        servicePathList = readServicePathList();
        this.executor = executor;
    }

    /**
     * Compare AEnd and ZEnd resource Node to
     * AEnd and ZEnd String.
     *
     * @param pathAend AEnd resource Node
     * @param pathZend ZEnd resource Node
     * @param inputAend AEnd String Node
     * @param inputZend ZEnd String Node
     * @return Boolean result true if equal, false if not
     */
    private Boolean comp(Resource pathAend, Resource pathZend, String inputAend, String inputZend) {
        Boolean result = false;
        if ((pathAend != null) && (pathZend != null) && (inputAend != null) && (inputZend != null)) {
            if ((pathAend instanceof Node) && (pathZend instanceof Node)) {
                Node aend = (Node) pathAend;
                Node zend = (Node) pathZend;
                if (aend.getNodeIdentifier().getNodeId().compareToIgnoreCase(inputAend) == 0) {
                    if (zend.getNodeIdentifier().getNodeId().compareToIgnoreCase(inputZend) == 0) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Compare two resource.
     *
     * @param res1 first resource
     * @param res2 second resource
     * @return Boolean result true if equal, false if not
     */
    private Boolean egalResource(Resource res1, Resource res2) {
        LOG.info("comparing resource ...");
        Boolean result = false;
        LOG.info("{} - {}", res1.getClass().getName(), res2.getClass().getName());
        if (res1.getClass().getName().compareToIgnoreCase(res2.getClass().getName()) == 0) {
            if ((res1 instanceof Node) && (res2 instanceof Node)) {
                Node node1 = (Node)res1;
                Node node2 = (Node)res2;
                if (node1.getNodeIdentifier().getNodeId()
                        .compareTo(node2.getNodeIdentifier().getNodeId()) == 0) {
                    result = true;
                }
            }
            if ((res1 instanceof TerminationPoint) && (res2 instanceof TerminationPoint)) {
                TerminationPoint tp1 = (TerminationPoint)res1;
                TerminationPoint tp2 = (TerminationPoint)res2;
                if (tp1.getTerminationPointIdentifier().getNodeId()
                        .compareTo(tp2.getTerminationPointIdentifier().getNodeId()) == 0) {
                    if (tp1.getTerminationPointIdentifier().getTpId()
                            .compareTo(tp2.getTerminationPointIdentifier().getTpId()) == 0) {
                        result = true;
                    }
                }
            }
            if ((res1 instanceof Link) && (res2 instanceof Link)) {
                Link link1 = (Link)res1;
                Link link2 = (Link)res2;
                if (link1.getLinkIdentifier().getLinkId().compareTo(link2.getLinkIdentifier().getLinkId()) == 0) {
                    result = true;
                }

            }
        }
        return result;
    }

    /**
     *  compare two AtoZDirection.
     *
     * @param atoz1 first AToZDirection
     * @param atoz2 second AToZDirection
     * @return Boolean result true if equal, false if not
     */
    private Boolean egalAtoZDirection(AToZDirection atoz1, AToZDirection atoz2) {
        LOG.info("comparing AtoZDirection ...");
        Boolean result = true;
        if (atoz1.getAToZ().size() == atoz2.getAToZ().size()) {
            int index = 0;
            int size = atoz1.getAToZ().size();
            LOG.info("size : {}", size);
            String id1 = null;
            String id2 = null;
            while (index < size) {
                id1 = atoz1.getAToZ().get(index).getId();
                LOG.info("id : {}", id1);
                Resource res1 = atoz1.getAToZ().get(index).getResource().getResource();
                LOG.info("res1 : {}", res1.toString());
                Resource res2 = null;
                if (id1 != null) {
                    Boolean trouve = false;
                    for (int loop = 0;loop < size;loop++) {
                        id2 = atoz2.getAToZ().get(loop).getId();
                        if ((id2 != null) && (id2.compareTo(id1) == 0)) {
                            res2 = atoz2.getAToZ().get(loop).getResource().getResource();
                            LOG.info("res2 : {}", res2.toString());
                            trouve = true;
                            break;
                        }
                    }
                    if (trouve) {
                        if (!egalResource(res1, res2)) {
                            result = false;
                            break;
                        }
                    }
                } else {
                    result = false;
                    break;
                }
                index++;
            }
        } else {
            LOG.info("AToZDirection size is not equal !");
            result = false;
        }
        return result;
    }

    /**
     *  compare two ZtoZDirection.
     *
     * @param ztoa1 first ZToZDirection
     * @param ztoa2 second ZToZDirection
     * @return Boolean result true if equal, false if not
     */
    private Boolean egalZtoADirection(ZToADirection ztoa1, ZToADirection ztoa2) {
        LOG.info("comparing ZtoADirection ...");
        Boolean result = true;
        if (ztoa1.getZToA().size() == ztoa2.getZToA().size()) {
            int index = 0;
            int size = ztoa1.getZToA().size();
            LOG.info("size : {}", size);
            String id1 = null;
            String id2 = null;
            while (index < size) {
                id1 = ztoa1.getZToA().get(index).getId();
                LOG.info("id : {}", id1);
                Resource res1 = ztoa1.getZToA().get(index).getResource().getResource();
                LOG.info("res1 : {}", res1.toString());
                Resource res2 = null;
                if (id1 != null) {
                    Boolean trouve = false;
                    for (int loop = 0;loop < size;loop++) {
                        id2 = ztoa2.getZToA().get(loop).getId();
                        if ((id2 != null) && (id2.compareTo(id1) == 0)) {
                            res2 = ztoa2.getZToA().get(loop).getResource().getResource();
                            LOG.info("res2 : {}", res2.toString());
                            trouve = true;
                            break;
                        }
                    }
                    if (trouve) {
                        if (!egalResource(res1, res2)) {
                            result = false;
                            break;
                        }
                    }
                } else {
                    result = false;
                    break;
                }
                index++;
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Test if resources Nodes
     * not include in PathDescriptions.
     *
     * @param path PathDescriptions
     * @param nodes Nodes List
     * @return Boolean result true if found, false if not
     */
    private Boolean excludeNode(PathDescriptions path, List<String> nodes) {
        LOG.info("Testing exclude Nodes ...");
        Boolean result = false;
        if ((path != null) && !nodes.isEmpty()) {
            List<AToZ> list = path.getAToZDirection().getAToZ();
            if (!list.isEmpty()) {
                int index = 0;
                boolean found = false;
                while ((index < list.size()) && !found) {
                    Resource res = list.get(index).getResource().getResource();
                    if ((res != null) && (res instanceof Node)) {
                        Node node = (Node) res;
                        for (String exclude : nodes) {
                            if (exclude.compareToIgnoreCase(node.getNodeIdentifier().getNodeId()) == 0) {
                                LOG.info("Node not excluded !");
                                found = true;
                                break;
                            }
                        }
                    }
                    index++;
                }
                if (!found) {
                    result = true;
                }
            }
        } else {
            LOG.info("exclude parameters not corrrect !");
        }
        return result;
    }

    /**
     * check if existing services not
     * in pathDescriptions.
     *
     *
     * @param existingService existing service list
     * @param path PathDescriptions
     * @param choice 0:Nodes, 1:Clli, 2:Srlg
     * @return Boolean result true if found, false if not
     */
    private Boolean diversityService(List<String> existingService, PathDescriptions path, int choice) {
        LOG.info("Testing diversity ...");
        Boolean result = false;
        if ((path != null) && (choice >= 0) && !existingService.isEmpty()) {
            int index = 0;
            while (index < existingService.size()) {
                String tmp = existingService.get(index);
                if (tmp != null) {
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service
                        .types.rev170426.service.path.PathDescription pathDesc = null;
                    if ((servicePathList != null) && !servicePathList.isEmpty()) {
                        for (ServicePaths service : servicePathList) {
                            if (service.getServicePathName().compareTo(tmp) == 0) {
                                LOG.info("Existing Service '{}' found in ServicePathList ...", tmp);
                                pathDesc = service.getPathDescription();
                            }
                        }
                    }
                    if (pathDesc != null) {
                        switch (choice) {
                            case 0: //Nodes
                                LOG.info("Checking Node existing-service-applicability ...");
                                if (!egalAtoZDirection(path.getAToZDirection(), pathDesc.getAToZDirection())) {
                                    result = true;
                                    break;
                                }
                                break;
                            case 1: //Clli
                                LOG.info("Checking clli existing-service-applicability ...");
                                break;

                            case 2: //Srlg
                                LOG.info("Checking srlg existing-service-applicability ...");

                                break;
                            default:
                                break;
                        }
                    } else {
                        LOG.info("Existing Service '{}' not found in ServicePathList !", tmp);
                        result = true;
                    }
                    if (!result) {
                        break;
                    }
                }
                index++;
            }
        } else {
            LOG.info("Diversity parameters not coherent !");
        }
        return result;
    }

    /**
     * test if pathDescription
     * already exists in ServicePathList.
     *
     * @param path PathDescriptions
     * @return <code>Boolean</code> result
     */
    private Boolean testPathDescription(PathDescriptions path) {
        LOG.info("Testing pathDescription ...");
        Boolean result = false;
        if (path != null) {
            LOG.info("retrieving path from servicePath List ...");
            try {
                if (!servicePathList.isEmpty()) {
                    LOG.info("ServicePathList not empty, contains {} paths.", servicePathList.size());
                    for (ServicePaths service : servicePathList) {
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service
                            .types.rev170426.service.path.PathDescription tmp = service.getPathDescription();
                        if (tmp != null) {
                            if ((path.getAToZDirection() != null) && (tmp.getAToZDirection() != null)
                                    && egalAtoZDirection(path.getAToZDirection(), tmp.getAToZDirection())) {
                                if ((path.getZToADirection() != null) && (tmp.getZToADirection() != null)
                                        && egalZtoADirection(path.getZToADirection(), tmp.getZToADirection())) {
                                    result = true;
                                    LOG.info("Path already present in servicePath List");
                                    break;
                                } else {
                                    LOG.info("ZtoADirection not equal or one of ZtoADirection is null!");
                                    break;
                                }
                            } else {
                                LOG.info("AtoZDirection not equal !");
                                break;
                            }

                        }
                    }
                } else {
                    LOG.info("ServicePathList is empty");
                }
            } catch (NullPointerException e) {
                LOG.error("ServicePathList is empty");
            }
        } else {
            LOG.info("PathDescriptions is null !");
            result = true;
        }
        LOG.info("testPathDescription result : {}", result);
        return result;
    }

    /**
     * function to retrieve Paths based on
     * AEnd and ZEnd.
     *
     * @param aendNodeId Aend Node Id
     * @param zendNodeId Zend Node Id
     * @return result PathDescriptions List
     */
    private List<PathDescriptions> retrievePath(String aendNodeId, String zendNodeId) {
        List<PathDescriptions> result = new ArrayList<PathDescriptions>();
        List<PathDescriptions> paths = readPathDescriptionList();
        if (!paths.isEmpty() && (aendNodeId != null) && (zendNodeId != null)) {
            LOG.info("retrieving paths from pathDescription List for {} / {}", aendNodeId, zendNodeId);
            for (PathDescriptions tmp : paths) {
                Resource pathAend = null;
                Resource pathZend = null;
                String id = null;
                if (tmp != null) {
                    LOG.info("Getting Aend & ZEnd from path '{}'...",tmp.getPathName());
                    int index = 0;
                    int size = tmp.getAToZDirection().getAToZ().size();
                    while (index < size) {
                        id = tmp.getAToZDirection().getAToZ().get(index).getId();
                        if (id.compareToIgnoreCase("1") == 0) {
                            Resource resource = tmp.getAToZDirection().getAToZ().get(index).getResource()
                                    .getResource();
                            LOG.info("{} : {}", resource.getClass().toString(), resource.toString());
                            pathAend = resource;
                            break;
                        }
                        index++;
                    }
                    index = 0;
                    while (index < size) {
                        id = tmp.getZToADirection().getZToA().get(index).getId();
                        if (id.compareToIgnoreCase("1") == 0) {
                            Resource resource = tmp.getZToADirection().getZToA().get(index).getResource()
                                    .getResource();
                            LOG.info(resource.toString());
                            pathZend = resource;
                            break;
                        }
                        index++;
                    }
                    if ((pathAend != null) && (pathZend != null)) {
                        LOG.info("pathAend : {} - pathZend: {}",pathAend, pathZend);
                        LOG.info("aendNodeId : {} - zendNodeId : {}", aendNodeId, zendNodeId);
                        if (comp(pathAend, pathZend, aendNodeId, zendNodeId)) {
                            LOG.info("PathDescription found !");
                            result.add(tmp);
                        }
                    }
                }
            }
        }
        return result;
    }


    /**
     * found Pathdescriptions with
     * name containing an expression.
     *
     * @param pathdescr PathDescriptions List
     * @param contain String expression
     * @return PathDescriptionsOrdered List
     */
    private SortedSet<PathDescriptionsOrdered> foundpath(List<PathDescriptions> pathdescr, String contain) {
        SortedSet<PathDescriptionsOrdered> result = new TreeSet<PathDescriptionsOrdered>();
        ListIterator<PathDescriptions> it = pathdescr.listIterator();
        int odr = 0;
        while (it.hasNext()) {
            PathDescriptions path = it.next();
            String name = path.getPathName();
            LOG.info("path  : {}", name);
            if ((name != null) && name.contains(contain)) {
                LOG.info("    path gets : {}", name);
                String [] split = name.split("_");
                if (split.length == 3) {
                    odr = Integer.parseInt(split[2]);
                    result.add(new PathDescriptionsOrdered(path, odr));
                }
            }
        }
        return result;
    }

    /**
     * order PathDescriptions List
     * with first, ordered direct links
     * and secondly ordered indirect
     * links.
     *
     * @param pathdescr PathDescriptions List
     * @return PathDescriptions List ordered
     */
    private List<PathDescriptions> orderPathdescriptionsList(List<PathDescriptions> pathdescr) {
        SortedSet<PathDescriptionsOrdered> direct = new TreeSet<PathDescriptionsOrdered>();
        SortedSet<PathDescriptionsOrdered> indirect = new TreeSet<PathDescriptionsOrdered>();
        List<PathDescriptions> result = new ArrayList<PathDescriptions>();
        int size = pathdescr.size();
        if (size > 0) {
            LOG.info("getting direct path first ...");
            direct = foundpath(pathdescr, "_direct_");
            LOG.info("getting indirect path first ...");
            indirect = foundpath(pathdescr, "_indirect_");
        }
        if (direct.size() > 0) {
            Iterator<PathDescriptionsOrdered> itset = direct.iterator();
            while (itset.hasNext()) {
                result.add(itset.next().getPathDescriptions());
            }
            if (indirect.size() > 0) {
                Iterator<PathDescriptionsOrdered> itset2 = indirect.iterator();
                while (itset2.hasNext()) {
                    result.add(itset2.next().getPathDescriptions());
                }
            }

        } else if (indirect.size() > 0) {
            Iterator<PathDescriptionsOrdered> itset2 = indirect.iterator();
            while (itset2.hasNext()) {
                result.add(itset2.next().getPathDescriptions());
            }
        }
        if (result.size() == pathdescr.size()) {
            return result;
        } else {
            return null;
        }
    }

    public ListenableFuture<Boolean> cancelResourceReserve() {
        LOG.info("In cancelResourceReserve request ...");
        setSuccess(false);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = false;
                if (cancelInput != null) {
                    Boolean found = false;
                    String name = cancelInput.getServiceName();
                    if ((name != null) && !servicePathList.isEmpty()) {
                        for (ServicePaths service : servicePathList) {
                            if (name.compareTo(service.getServicePathName()) == 0) {
                                LOG.info("ServicePaths found in ServicePathList !!!");
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            LOG.info("removing servicePaths from datastore ...");
                            if (writeOrDeleteServicePathList(name,1)) {
                                LOG.info("Service deleted !");
                                setSuccess(true);
                                output = true;
                            } else {
                                LOG.info("service deletion failed !");
                            }
                        }
                    } else {
                        LOG.info("serviceName is null or servicePathList is empty !");
                    }
                } else {
                    LOG.info("cancelresourcereserveinput parameter not valid !");
                }
                return output;
            }
        });
    }

    public ListenableFuture<Boolean> pathComputation() {
        LOG.info("In pathComputation request ...");
        setSuccess(false);
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = false;
                List<PathDescriptions> pathsList = new ArrayList<PathDescriptions>();
                PathDescriptions path = null;
                int index ;
                Boolean constraints = false;
                if (input != null) {
                    HardConstraints inputHard = input.getHardConstraints();
                    SoftConstraints inputSoft = input.getSoftConstraints();
                    if ((inputHard != null) || (inputSoft != null)) {
                        constraints = true;
                    }
                    path = null;
                    pathsList =  retrievePath(input.getServiceAEnd().getNodeId(), input.getServiceZEnd()
                            .getNodeId());
                    index = 0;
                    output = false;
                    /** get pathList ordered. */
                    pathsList = orderPathdescriptionsList(pathsList);
                    if (!pathsList.isEmpty()) {
                        LOG.info("{} Paths get from Pathdescription List", pathsList.size());
                        index = 0;
                        output = false;
                        while (index < pathsList.size()) {
                            path = pathsList.get(index);
                            LOG.info("path n°{} gets : '{}'!", index, path.getPathName());
                            if (constraints) {
                                LOG.info("Calculating path with constraints ...");
                                if (inputHard.getCoRoutingOrGeneral() instanceof General) {
                                    General general = (General)inputHard.getCoRoutingOrGeneral();
                                    if (general != null) {
                                        Diversity diversity = general.getDiversity();
                                        if (diversity != null) {
                                            LOG.info("Getting diversity ...");
                                            List<String> existingService = diversity.getExistingService();
                                            if (existingService.size() > 0) {
                                                LOG.info("Getting existing service applicability ...");
                                                int choice = -1;
                                                if ((choice < 0)
                                                        && diversity.getExistingServiceApplicability().isNode()) {
                                                    LOG.info("existing-service-applicability : Node");
                                                    choice = 0;
                                                }
                                                if ((choice < 0)
                                                        && diversity.getExistingServiceApplicability().isClli()) {
                                                    LOG.info("existing-service-applicability : Clli");
                                                    choice = 1;
                                                }
                                                if ((choice < 0)
                                                        && diversity.getExistingServiceApplicability().isSrlg()) {
                                                    LOG.info("existing-service-applicability : Srlg");
                                                    choice = 2;
                                                }
                                                if (!diversityService(existingService, path, choice)) {
                                                    error = "existing service applicability not satisfied";
                                                    LOG.info(error);
                                                    path = null;
                                                }
                                            }
                                        }
                                        Exclude exclude = general.getExclude();
                                        if (exclude != null) {
                                            LOG.info("Getting exclude ...");
                                            if (!excludeNode(path, exclude.getNodeId())) {
                                                error = "Exclude node constraints not satisfied !";
                                                LOG.info(error);
                                                path = null;
                                            }
                                        }
                                    }
                                }
                            }
                            if (!testPathDescription(path)) {
                                LOG.info("Process finish !");
                                output = true;
                                break;
                            }
                            index++;
                        }
                    } else {
                        LOG.info("failed to retrieve path from PathDescription List !");
                    }
                } else {
                    LOG.info("pathComputationRequestInput parameter not valid !");
                }
                if (path != null) {
                    LOG.info("Path ok !");
                    pathDescription = new PathDescriptionBuilder()
                            .setAToZDirection(path.getAToZDirection())
                            .setZToADirection(path.getZToADirection());
                    if (input.isResourceReserve()) {
                        LOG.info("reserving pce resource ...");
                        setPathDescription(pathDescription);
                        if (writeOrDeleteServicePathList(input.getServiceName(), 0)) {
                            LOG.info("write ServicePaths to datastore");
                            setSuccess(true);
                        } else {
                            LOG.error("writing ServicePaths to datastore failed ! ");
                        }
                    } else {
                        LOG.info("no pce resource reserve !");
                        setSuccess(true);
                    }
                }
                return output;
            }
        });
    }


    /**
     * get all ServicePaths in ServicePathlist.
     *
     * @return <code>ServicePaths List</code>
     */
    private List<ServicePaths> readServicePathList() {
        LOG.info("Reading ServicePathList ...");
        List<ServicePaths> result = null;
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        InstanceIdentifier<ServicePathList> iid = InstanceIdentifier.create(ServicePathList.class);
        Future<Optional<ServicePathList>> future = readTx.read(LogicalDatastoreType.OPERATIONAL,iid);
        Optional<ServicePathList> optional = Optional.absent();
        try {
            optional = Futures.getChecked(future, ExecutionException.class, 60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            LOG.error("Reading service failed:", e);
        }
        if (optional.isPresent()) {
            LOG.info("ServicePath List present !");
            result = optional.get().getServicePaths();
        }
        return result;
    }


    private List<PathDescriptions> readPathDescriptionList() {
        LOG.info("Reading PathDescriptionsList ...");
        List<PathDescriptions> result = null;
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        InstanceIdentifier<PathDescriptionList> iid = InstanceIdentifier.create(PathDescriptionList.class);
        Future<Optional<PathDescriptionList>> future = readTx.read(LogicalDatastoreType.OPERATIONAL,iid);
        Optional<PathDescriptionList> optional = Optional.absent();
        try {
            optional = Futures.getChecked(future, ExecutionException.class, 60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            LOG.error("Reading service failed:", e);
        }
        if (optional.isPresent()) {
            LOG.info("ServicePath List present !");
            result = optional.get().getPathDescriptions();
        }
        return result;

    }

    /**
     * Write or Delete ServicePaths
     * for ServicePathList.
     *
     * @param serviceName Service Name
     * @param choice 0 : write or 1 : delete
     * @return Boolean result true if deleted, false if not
     */
    private Boolean writeOrDeleteServicePathList(String serviceName, int choice) {
        Boolean result = null;
        if ((serviceName != null) && (serviceName.compareTo(" ") != 0) && (choice >= 0) && (choice < 2)) {
            LOG.info("WriteOrDeleting '{}' ServicePaths", serviceName);
            WriteTransaction writeTx = db.newWriteOnlyTransaction();
            result = true;
            String action = null;
            InstanceIdentifier<ServicePaths> iid = InstanceIdentifier.create(ServicePathList.class)
                    .child(ServicePaths.class,new ServicePathsKey(serviceName));
            Future<Void> future = null;
            switch (choice) {
                case 0: /** Write. */
                    LOG.info("Writing '{}' Service", serviceName);
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service
                        .path.PathDescriptionBuilder path = new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c
                        ._interface.service.types.rev170426.service.path.PathDescriptionBuilder();
                    if (pathDescription != null) {
                        if (pathDescription.getAToZDirection() != null) {
                            path.setAToZDirection(pathDescription.getAToZDirection());
                        }
                        if (pathDescription.getZToADirection() != null) {
                            path.setZToADirection(pathDescription.getZToADirection());
                        }
                        LOG.info("pathdescription gets");
                    }
                    ServiceAEnd aend = new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c
                            ._interface.service.types.rev170426.service.path.ServiceAEndBuilder(input.getServiceAEnd())
                            .build();
                    ServiceZEnd zend = new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c
                            ._interface.service.types.rev170426.service.path.ServiceZEndBuilder(input.getServiceZEnd())
                            .build();
                    ServicePaths service = new ServicePathsBuilder()
                            .setServicePathName(serviceName)
                            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                                    .setRequestId(input.getServiceHandlerHeader().getRequestId()).build())
                            .setServiceAEnd(aend)
                            .setServiceZEnd(zend)
                            .setHardConstraints(input.getHardConstraints())
                            .setSoftConstraints(input.getSoftConstraints())
                            .setPathDescription(path.build())
                            .build();
                    LOG.info("Servicepath build");
                    writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
                    action = "write";
                    //CheckedFuture<Void, OperationFailedException> future = transaction.submit();
                    future = writeTx.submit();
                    try {
                        LOG.info("Sending '{}' command to datastore !", action);
                        Futures.getChecked(future, ExecutionException.class);
                    } catch (ExecutionException e) {
                        LOG.error("Failed to {} service from Service List", action);
                        result = false;
                    }
                    break;

                case 1: /** Delete */
                    LOG.info("Deleting '{}' Service", serviceName);
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    action = "delete";
                    future = writeTx.submit();
                    try {
                        LOG.info("Sending '{}' command to datastore !", serviceName);
                        Futures.getChecked(future, ExecutionException.class);
                    } catch (ExecutionException e) {
                        LOG.error("Failed to {} service from Service List", serviceName);
                        result = false;
                    }
                    break;

                default:
                    LOG.info("No choice found");
                    break;

            }

        } else {
            LOG.info("Parameters not correct !");
        }
        return result;
    }

    public PathDescriptionBuilder getPathDescription() {
        return pathDescription;
    }

    public void setPathDescription(PathDescriptionBuilder pathDescription) {
        this.pathDescription = pathDescription;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public PathComputationRequestInput getInput() {
        return input;
    }

    public void setInput(PathComputationRequestInput pathComputationRequestInput) {
        this.input = pathComputationRequestInput;
    }

    public CancelResourceReserveInput getCancelInput() {
        return cancelInput;
    }

    public void setCancelInput(CancelResourceReserveInput cancelResourceReserveInput) {
        this.cancelInput = cancelResourceReserveInput;
    }

    public DataBroker getDb() {
        return db;
    }

    public void setDb(DataBroker db) {
        this.db = db;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<ServicePaths> getServicePathList() {
        return servicePathList;
    }

    public void setServicePathList(List<ServicePaths> servicePathList) {
        this.servicePathList = servicePathList;
    }
}
