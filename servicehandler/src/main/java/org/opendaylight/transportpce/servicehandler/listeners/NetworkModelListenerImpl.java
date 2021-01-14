/*
 * Copyright © 2020 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.OrdTopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.OrdTopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelListenerImpl implements TransportpceNetworkmodelListener {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelListenerImpl.class);
    private final NotificationPublishService notificationPublishService; // to be used for T-API notification
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private TopologyUpdateResult topologyUpdateResult;

    public NetworkModelListenerImpl(NotificationPublishService notificationPublishService,
                                    ServiceDataStoreOperations serviceDataStoreOperations) {
        this.notificationPublishService = notificationPublishService;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
    }

    @Override
    public void onTopologyUpdateResult(TopologyUpdateResult notification) {
        LOG.info("Topology update notification: {}", notification.toString());
        if (compareTopologyUpdateResult(notification)) {
            LOG.warn("TopologyUpdateResult already wired !");
            return;
        }
        topologyUpdateResult = notification;
        switch (topologyUpdateResult.getNotificationType().getIntValue()) {
            case 1:
                // Update service datastore and service path description
                LOG.info("openroadm-topology update");
                updateServicePaths(notification);
                break;
            case 2:
                LOG.info("openroadm-network update");
                break;
            case 3:
                LOG.info("clli-network update");
                break;
            case 4:
                LOG.info("otn-topology update");
                break;
            default:
                break;
        }
    }

    /**
     * Process topology update result.
     * @param notification the result notification.
     */
    private void updateServicePaths(TopologyUpdateResult notification) {
        Map<OrdTopologyChangesKey, OrdTopologyChanges> ordTopologyChanges = notification.getOrdTopologyChanges();
        Optional<ServicePathList> servicePathListOptional = this.serviceDataStoreOperations.getServicePaths();
        ServicePathList servicePathList = null;
        if (!servicePathListOptional.isPresent()) {
            LOG.error("Couldn't retrieve service path list");
            return;
        }
        servicePathList = servicePathListOptional.get();
        if (servicePathList.getServicePaths().isEmpty()) {
            return;
        }
        for (ServicePaths servicePaths : servicePathList.getServicePaths().values()) {
            String serviceName = servicePaths.getServicePathName();
            PathDescription pathDescription = servicePaths.getPathDescription();
            // update path descriptions in the datastore
            Map<AToZKey, AToZ> updatedAtoZ = changePathElementStateAZ(ordTopologyChanges, pathDescription);
            Map<ZToAKey, ZToA> updatedZtoA = changePathElementStateZA(ordTopologyChanges, pathDescription);
            OperationResult operationResult = this.serviceDataStoreOperations
                    .modifyServicePath(buildNewPathDescription(pathDescription, updatedAtoZ, updatedZtoA), serviceName);
            if (!operationResult.isSuccess()) {
                LOG.warn("Service Path not updated in datastore!");
                continue;
            }
            // update service in the datastore. Only path description with all elements in service can have a service
            // in service. Therefore we check if all the states of the path description resources are inService
            Optional<Services> serviceOptional = this.serviceDataStoreOperations.getService(serviceName);
            Services services = null;
            if (!serviceOptional.isPresent()) {
                LOG.error("Couldn't retrieve service");
                continue;
            }
            services = serviceOptional.get();
            OperationResult operationResult1 = null;
            if (State.InService.equals(services.getOperationalState())
                    && !allElementsinPathinService(updatedAtoZ, updatedZtoA)) {
                LOG.info("Service={} needs to be updated to outOfService", serviceName);
                operationResult1 = this.serviceDataStoreOperations.modifyService(serviceName, State.OutOfService,
                        AdminStates.OutOfService);
            } else if (State.OutOfService.equals(services.getOperationalState())
                    && allElementsinPathinService(updatedAtoZ, updatedZtoA)) {
                LOG.info("Service={} needs to be updated to inService", serviceName);
                operationResult1 = this.serviceDataStoreOperations.modifyService(serviceName, State.InService,
                        AdminStates.InService);
            } else {
                LOG.info("Service {} state doesnt need to be modified", serviceName);
            }
            if (operationResult1 != null && operationResult1.isSuccess()) {
                LOG.info("Service state correctly updated in datastore");
            }
        }
    }

    private Map<ZToAKey, ZToA> changePathElementStateZA(Map<OrdTopologyChangesKey,
            OrdTopologyChanges> ordTopologyChanges, PathDescription pathDescription) {
        Map<ZToAKey, ZToA> ztoaMap = pathDescription.getZToADirection().getZToA();
        // Needed as ztoaMap is immutable
        Map<ZToAKey, ZToA> newztoaMap = new HashMap<ZToAKey, ZToA>();
        for (ZToA element : ztoaMap.values()) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State elementState
                    = element.getResource().getState();
            String elementType = element.getResource().getResource().implementedInterface().getSimpleName();
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State updatedState = null;
            ZToAKey elementKey = null;
            Resource elementResource = null;
            switch (elementType) {
                case "TerminationPoint":
                    TerminationPoint tp = (TerminationPoint) element.getResource().getResource();
                    // for the tps we need to merge nodeId and tpId
                    String tpId = tp.getTpNodeId() + "-" + tp.getTpId();
                    if (!(ordTopologyChanges.containsKey(new OrdTopologyChangesKey(tpId))
                            && !ordTopologyChanges.get(new OrdTopologyChangesKey(tpId)).getState()
                            .equals(elementState))) {
                        newztoaMap.put(element.key(), element);
                        continue;
                    }
                    updatedState = ordTopologyChanges.get(new OrdTopologyChangesKey(tpId)).getState();
                    LOG.info("Updating path element {} to {}", tpId, updatedState.getName());
                    // Create new resource element and replace on map
                    elementKey = new ZToAKey(element.getId());
                    elementResource = new ResourceBuilder().setResource(tp).setState(updatedState).build();
                    ZToA tpResource = new ZToABuilder().setId(tp.getTpId()).withKey(elementKey)
                            .setResource(elementResource).build();
                    newztoaMap.put(elementKey, tpResource);
                    break;
                case "Link":
                    Link link = (Link) element.getResource().getResource();
                    if (!(ordTopologyChanges.containsKey(new OrdTopologyChangesKey(link.getLinkId()))
                            && !ordTopologyChanges.get(new OrdTopologyChangesKey(link.getLinkId())).getState()
                            .equals(elementState))) {
                        newztoaMap.put(element.key(), element);
                        continue;
                    }
                    updatedState = ordTopologyChanges.get(new OrdTopologyChangesKey(link.getLinkId())).getState();
                    LOG.info("Updating path element {} to {}", link.getLinkId(), updatedState.getName());
                    // Create new resource element and replace on map
                    elementKey = new ZToAKey(element.getId());
                    elementResource = new ResourceBuilder().setResource(link).setState(updatedState).build();
                    ZToA linkResource = new ZToABuilder().setId(link.getLinkId()).withKey(elementKey)
                            .setResource(elementResource).build();
                    newztoaMap.put(elementKey, linkResource);
                    break;
                case "Node":
                    Node node = (Node) element.getResource().getResource();
                    if (!(ordTopologyChanges.containsKey(new OrdTopologyChangesKey(node.getNodeId()))
                            && !ordTopologyChanges.get(new OrdTopologyChangesKey(node.getNodeId())).getState()
                            .equals(elementState))) {
                        newztoaMap.put(element.key(), element);
                        continue;
                    }
                    updatedState = ordTopologyChanges.get(new OrdTopologyChangesKey(node.getNodeId())).getState();
                    LOG.info("Updating path element {} to {}", node.getNodeId(), updatedState.getName());
                    // Create new resource element and replace on map
                    elementKey = new ZToAKey(element.getId());
                    elementResource = new ResourceBuilder().setResource(node).setState(updatedState).build();
                    ZToA nodeResource = new ZToABuilder().setId(node.getNodeId()).withKey(elementKey)
                            .setResource(elementResource).build();
                    newztoaMap.put(elementKey, nodeResource);
                    break;
                default:
                    LOG.warn("Element type {} not recognized", elementType);
                    break;
            }
        }
        return newztoaMap;
    }

    private Map<AToZKey, AToZ> changePathElementStateAZ(Map<OrdTopologyChangesKey,
            OrdTopologyChanges> ordTopologyChanges, PathDescription pathDescription) {
        Map<AToZKey, AToZ> atozMap = pathDescription.getAToZDirection().getAToZ();
        // Needed as atozMap is immutable
        Map<AToZKey, AToZ> newatozMap = new HashMap<AToZKey, AToZ>();
        for (AToZ element : atozMap.values()) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State elementState
                = element.getResource().getState();
            String elementType = element.getResource().getResource().implementedInterface().getSimpleName();
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State updatedState = null;
            AToZKey elementKey = null;
            Resource elementResource = null;
            switch (elementType) {
                case "TerminationPoint":
                    TerminationPoint tp = (TerminationPoint) element.getResource().getResource();
                    // for the tps we need to merge nodeId and tpId
                    String tpId = tp.getTpNodeId() + "-" + tp.getTpId();
                    if (!(ordTopologyChanges.containsKey(new OrdTopologyChangesKey(tpId))
                            && !ordTopologyChanges.get(new OrdTopologyChangesKey(tpId)).getState()
                            .equals(elementState))) {
                        newatozMap.put(element.key(), element);
                        continue;
                    }
                    updatedState = ordTopologyChanges.get(new OrdTopologyChangesKey(tpId)).getState();
                    LOG.info("Updating path element {} to {}", tpId, updatedState.getName());
                    // Create new resource element and replace on map
                    elementKey = new AToZKey(element.getId());
                    elementResource = new ResourceBuilder().setResource(tp).setState(updatedState).build();
                    AToZ tpResource = new AToZBuilder().setId(tp.getTpId()).withKey(elementKey)
                            .setResource(elementResource).build();
                    newatozMap.put(elementKey, tpResource);
                    break;
                case "Link":
                    Link link = (Link) element.getResource().getResource();
                    if (!(ordTopologyChanges.containsKey(new OrdTopologyChangesKey(link.getLinkId()))
                            && !ordTopologyChanges.get(new OrdTopologyChangesKey(link.getLinkId())).getState()
                            .equals(elementState))) {
                        newatozMap.put(element.key(), element);
                        continue;
                    }
                    updatedState = ordTopologyChanges.get(new OrdTopologyChangesKey(link.getLinkId()))
                            .getState();
                    LOG.info("Updating path element {} to {}", link.getLinkId(), updatedState.getName());
                    // Create new resource element and replace on map
                    elementKey = new AToZKey(element.getId());
                    elementResource = new ResourceBuilder().setResource(link).setState(updatedState).build();
                    AToZ linkResource = new AToZBuilder().setId(link.getLinkId()).withKey(elementKey)
                            .setResource(elementResource).build();
                    newatozMap.put(elementKey, linkResource);
                    break;
                case "Node":
                    Node node = (Node) element.getResource().getResource();
                    if (!(ordTopologyChanges.containsKey(new OrdTopologyChangesKey(node.getNodeId()))
                            && !ordTopologyChanges.get(new OrdTopologyChangesKey(node.getNodeId())).getState()
                            .equals(elementState))) {
                        newatozMap.put(element.key(), element);
                        continue;
                    }
                    updatedState = ordTopologyChanges.get(new OrdTopologyChangesKey(node.getNodeId())).getState();
                    LOG.info("Updating path element {} to {}", node.getNodeId(), updatedState.getName());
                    // Create new resource element and replace on map
                    elementKey = new AToZKey(element.getId());
                    elementResource = new ResourceBuilder().setResource(node).setState(updatedState).build();
                    AToZ nodeResource = new AToZBuilder().setId(node.getNodeId()).withKey(elementKey)
                            .setResource(elementResource).build();
                    newatozMap.put(elementKey, nodeResource);
                    break;
                default:
                    LOG.warn("Element type {} not recognized", elementType);
                    break;
            }
        }
        return newatozMap;
    }

    private PathDescription buildNewPathDescription(PathDescription pathDescription, Map<AToZKey, AToZ> updatedAtoZ,
                                                    Map<ZToAKey, ZToA> updatedZtoA) {
        // A to Z
        AToZDirection atozDir = new AToZDirectionBuilder()
                .setAToZ(updatedAtoZ)
                .setAToZWavelengthNumber(pathDescription.getAToZDirection().getAToZWavelengthNumber())
                .setModulationFormat(pathDescription.getAToZDirection().getModulationFormat())
                .setRate(pathDescription.getAToZDirection().getRate())
                .setTribPortNumber(pathDescription.getAToZDirection().getTribPortNumber())
                .setTribSlotNumber(pathDescription.getAToZDirection().getTribSlotNumber())
                .build();
        // Z to A
        ZToADirection ztoaDir = new ZToADirectionBuilder()
                .setZToA(updatedZtoA)
                .setZToAWavelengthNumber(pathDescription.getZToADirection().getZToAWavelengthNumber())
                .setModulationFormat(pathDescription.getZToADirection().getModulationFormat())
                .setRate(pathDescription.getAToZDirection().getRate())
                .setTribPortNumber(pathDescription.getAToZDirection().getTribPortNumber())
                .setTribSlotNumber(pathDescription.getAToZDirection().getTribSlotNumber())
                .build();
        return new PathDescriptionBuilder().setAToZDirection(atozDir).setZToADirection(ztoaDir).build();
    }

    private boolean allElementsinPathinService(Map<AToZKey, AToZ> updatedAtoZ, Map<ZToAKey, ZToA> updatedZtoA) {
        boolean allEleminService = true;
        Iterator<AToZ> i1 = updatedAtoZ.values().iterator();
        Iterator<ZToA> i2 = updatedZtoA.values().iterator();
        // TODO: both directions have same length?
        while (i1.hasNext() && i2.hasNext()) {
            if (State.OutOfService.getIntValue() == i1.next().getResource().getState().getIntValue()
                    || State.OutOfService.getIntValue() == i2.next().getResource().getState().getIntValue()) {
                allEleminService = false;
                break;
            }
        }

        return allEleminService;
    }

    @SuppressFBWarnings(
            value = "ES_COMPARING_STRINGS_WITH_EQ",
            justification = "false positives, not strings but real object references comparisons")
    private boolean compareTopologyUpdateResult(TopologyUpdateResult notification) {
        if (topologyUpdateResult == null) {
            return false;
        }
        if (topologyUpdateResult.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (topologyUpdateResult.getOrdTopologyChanges().values()
                .equals(notification.getOrdTopologyChanges().values())) {
            return false;
        }
        return true;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }
}
