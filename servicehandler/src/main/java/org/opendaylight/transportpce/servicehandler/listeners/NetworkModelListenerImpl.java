/*
 * Copyright © 2020 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPoint;
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
        LOG.debug("Topology update notification: {}", notification.toString());
        if (compareTopologyUpdateResult(notification)) {
            LOG.warn("TopologyUpdateResult already wired !");
            return;
        }
        topologyUpdateResult = notification;
        // Update service datastore and service path description
        updateServicePaths(notification);
    }

    /**
     * Process topology update result.
     * @param notification the result notification.
     */
    private void updateServicePaths(TopologyUpdateResult notification) {
        @Nullable
        Map<TopologyChangesKey, TopologyChanges> topologyChanges = notification.getTopologyChanges();
        Optional<ServicePathList> servicePathListOptional = this.serviceDataStoreOperations.getServicePaths();
        ServicePathList servicePathList = null;
        if (!servicePathListOptional.isPresent()) {
            LOG.warn("Enable to retrieve service path list");
            return;
        }
        servicePathList = servicePathListOptional.get();
        for (ServicePaths servicePaths : servicePathList.getServicePaths().values()) {
            String serviceName = servicePaths.getServicePathName();
            PathDescription pathDescription = servicePaths.getPathDescription();
            // update path descriptions in the datastore
            Map<AToZKey, AToZ> updatedAtoZ = changePathElementStateAZ(topologyChanges, pathDescription);
            Map<ZToAKey, ZToA> updatedZtoA = changePathElementStateZA(topologyChanges, pathDescription);
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
            if (org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State.InService
                    .equals(services.getOperationalState())
                && !allElementsinPathinService(updatedAtoZ, updatedZtoA)) {
                LOG.debug("Service={} needs to be updated to outOfService", serviceName);
                operationResult1 = this.serviceDataStoreOperations.modifyService(serviceName, State.OutOfService,
                        AdminStates.OutOfService);
            } else if (org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State.OutOfService
                    .equals(services.getOperationalState())
                && allElementsinPathinService(updatedAtoZ, updatedZtoA)) {
                LOG.debug("Service={} needs to be updated to inService", serviceName);
                operationResult1 = this.serviceDataStoreOperations.modifyService(serviceName, State.InService,
                        AdminStates.InService);
            } else {
                LOG.debug("Service {} state doesnt need to be modified", serviceName);
            }
            if (operationResult1 != null && operationResult1.isSuccess()) {
                LOG.info("Service state of {} correctly updated in datastore", serviceName);
            }
        }
    }

    private Map<ZToAKey, ZToA> changePathElementStateZA(Map<TopologyChangesKey, TopologyChanges> topologyChanges,
        PathDescription pathDescription) {

        Map<ZToAKey, ZToA> newztoaMap = new HashMap<>(pathDescription.getZToADirection().getZToA());
        List<ZToA> tpResources = pathDescription.getZToADirection().getZToA().values().stream()
            .filter(ele -> ele.getResource().getResource().implementedInterface().getSimpleName()
                .equals("TerminationPoint"))
            .collect(Collectors.toList());
        for (ZToA ztoA : tpResources) {
            String ztoAid = ztoA.getId();
            State ztoAState = ztoA.getResource().getState();
            TerminationPoint tp = (TerminationPoint) ztoA.getResource().getResource();
            if (topologyChanges.containsKey(new TopologyChangesKey(tp.getTpNodeId(), tp.getTpId()))
                && !topologyChanges.get(new TopologyChangesKey(tp.getTpNodeId(), tp.getTpId())).getState()
                .equals(ztoAState)) {
                LOG.debug("updating ztoa tp {}", ztoA);
                State updatedState = topologyChanges.get(new TopologyChangesKey(tp.getTpNodeId(), tp.getTpId()))
                    .getState();
                Resource updatedResource = new ResourceBuilder()
                    .setResource(tp)
                    .setState(updatedState)
                    .build();
                ZToA updatedZToA = new ZToABuilder(ztoA)
                    .setId(ztoAid)
                    .setResource(updatedResource)
                    .build();
                newztoaMap.put(updatedZToA.key(), updatedZToA);
            }
        }
        return newztoaMap;
    }

    private Map<AToZKey, AToZ> changePathElementStateAZ(Map<TopologyChangesKey,
            TopologyChanges> topologyChanges, PathDescription pathDescription) {

        Map<AToZKey, AToZ> newatozMap = new HashMap<>(pathDescription.getAToZDirection().getAToZ());
        List<AToZ> tpResources = pathDescription.getAToZDirection().getAToZ().values().stream()
            .filter(ele -> ele.getResource().getResource().implementedInterface().getSimpleName()
                .equals("TerminationPoint"))
            .collect(Collectors.toList());
        for (AToZ atoZ : tpResources) {
            String atoZid = atoZ.getId();
            State atoZState = atoZ.getResource().getState();
            TerminationPoint tp = (TerminationPoint) atoZ.getResource().getResource();
            if (topologyChanges.containsKey(new TopologyChangesKey(tp.getTpNodeId(), tp.getTpId()))
                && !topologyChanges.get(new TopologyChangesKey(tp.getTpNodeId(), tp.getTpId())).getState()
                .equals(atoZState)) {
                LOG.debug("updating atoz tp {}", atoZ);
                State updatedState = topologyChanges.get(new TopologyChangesKey(tp.getTpNodeId(), tp.getTpId()))
                    .getState();
                Resource updatedResource = new ResourceBuilder()
                    .setResource(tp)
                    .setState(updatedState)
                    .build();
                AToZ updatedAToZ = new AToZBuilder(atoZ)
                    .setId(atoZid)
                    .setResource(updatedResource)
                    .build();
                newatozMap.put(updatedAToZ.key(), updatedAToZ);
            }
        }
        return newatozMap;
    }

    private PathDescription buildNewPathDescription(PathDescription pathDescription, Map<AToZKey, AToZ> updatedAtoZ,
                                                    Map<ZToAKey, ZToA> updatedZtoA) {
        AToZDirection atozDir = new AToZDirectionBuilder(pathDescription.getAToZDirection())
                .setAToZ(updatedAtoZ)
                .build();
        ZToADirection ztoaDir = new ZToADirectionBuilder(pathDescription.getZToADirection())
                .setZToA(updatedZtoA)
                .build();
        return new PathDescriptionBuilder()
            .setAToZDirection(atozDir)
            .setZToADirection(ztoaDir)
            .build();
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

    private boolean compareTopologyUpdateResult(TopologyUpdateResult notification) {
        if (topologyUpdateResult == null) {
            return false;
        }
        if (topologyUpdateResult.getTopologyChanges().values()
                .equals(notification.getTopologyChanges().values())) {
            return false;
        }
        return true;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }
}
