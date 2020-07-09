/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.topology.update.result.Changes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsAToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsAToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsZToA;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = {"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_PKGPROTECT"},
        justification = "It will have conflicts with TAPI module")
public class NetworkModelListenerImpl implements TransportpceNetworkmodelListener {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelListenerImpl.class);
    public static ServiceDataStoreOperations serviceDataStoreOperations;
    private final NotificationPublishService notificationPublishService;

    public NetworkModelListenerImpl(NotificationPublishService notificationPublishService,
                                    ServiceDataStoreOperations serviceDataStoreOperations) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.notificationPublishService = notificationPublishService;

    }

    @Override
    public void onTopologyUpdateResult(TopologyUpdateResult notification) {
        LOG.info("Received topology update notification: {}", notification);
        List<Changes> changesList = notification.getChanges();
        String type = notification.getNotificationType().getName();
        switch (type) {
            case "openroadm-topology-update":
                LOG.info("Openroadm topology update type of notification");
                if (this.serviceDataStoreOperations != null) {
                    LOG.info("Service datastore is not null");
                    Optional<ServiceList> serviceListOptional = this.serviceDataStoreOperations.getServices();
                    if (serviceListOptional.isPresent()) {
                        ServiceList serviceList = serviceListOptional.get();
                        LOG.info("Service list in datastore is: {}", serviceList.toString());
                        if (serviceList.getServices() != null) {
                            LOG.info("Service list is not empty");
                            for (Services services:serviceList.getServices()) {
                                List<PcePathDescriptionElementsAToZ>
                                        pceatoz = services.getPcePathDescriptionElementsAToZ();
                                List<PcePathDescriptionElementsAToZ> newpceatoz = pceatoz.stream()
                                        .collect(Collectors.toList());
                                List<PcePathDescriptionElementsZToA>
                                        pceztoa = services.getPcePathDescriptionElementsZToA();
                                List<PcePathDescriptionElementsZToA> newpceztoa = pceztoa.stream()
                                        .collect(Collectors.toList());
                                boolean update = false;
                                int count;
                                for (Changes changes:changesList) {
                                    count = 0;
                                    for (PcePathDescriptionElementsAToZ elem:pceatoz) {
                                        if (elem.getElementIdentifier().equals(changes.getId())) {
                                            LOG.info("Match in names: {}-{}", elem.getElementIdentifier(),
                                                    changes.getId());
                                            if (!elem.getOperationalState().getName().equals(changes.getState()
                                                    .getName())) {
                                                LOG.info("Changing element of PathListA-Z: {}-{}",
                                                        elem.getElementIdentifier(),
                                                        State.forValue(changes.getState().getIntValue()));
                                                newpceatoz.set(count, new PcePathDescriptionElementsAToZBuilder()
                                                        .setElementIdentifier(elem.getElementIdentifier())
                                                        .setOperationalState(State.forValue(changes.getState()
                                                                .getIntValue())).build());
                                                update = true;
                                            }
                                            break;
                                        }
                                        count++;
                                    }
                                    count = 0;
                                    for (PcePathDescriptionElementsZToA elem:pceztoa) {
                                        if (elem.getElementIdentifier().equals(changes.getId())) {
                                            LOG.info("Match in names: {}-{}", elem.getElementIdentifier(),
                                                    changes.getId());
                                            if (!elem.getOperationalState().getName().equals(changes.getState()
                                                    .getName())) {
                                                LOG.info("Changing element of PathListZ-A: {}-{}",
                                                        elem.getElementIdentifier(),
                                                        State.forValue(changes.getState().getIntValue()));
                                                newpceztoa.set(count, new PcePathDescriptionElementsZToABuilder()
                                                        .setElementIdentifier(elem.getElementIdentifier())
                                                        .setOperationalState(State.forValue(changes.getState()
                                                                .getIntValue())).build());
                                                update = true;
                                            }
                                            break;
                                        }
                                        count++;
                                    }
                                }
                                try {
                                    LOG.info("Going to update service state");
                                    updateService(services, newpceatoz, newpceztoa, update);
                                } catch (ParseException e) {
                                    LOG.error("Error updating service datastore", e);
                                }
                            }
                        }
                    }
                }
                break;
            case "openroadm-network-update":
                break;
            case "clli-network-update":
                break;
            case "otn-topology-update":
                break;
            default:
                LOG.error("Type={} of notification not recognized", type);
        }
    }

    private void updateService(Services services, List<PcePathDescriptionElementsAToZ> pceatoz,
                               List<PcePathDescriptionElementsZToA> pceztoa, boolean update) throws ParseException {
        LOG.info("Updating service");
        if (update) {
            if (services.getOperationalState().equals(org.opendaylight.yang.gen.v1.http.org.openroadm.common.state
                    .types.rev181130.State.InService)) {
                LOG.info("Setting service to OOS");
                this.serviceDataStoreOperations.modifyServiceNM(services.getServiceName(),
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types
                                .rev181130.State.OutOfService, AdminStates.OutOfService, LifecycleState.DeployFailed,
                        pceatoz, pceztoa);
            } else {
                LOG.info("Potential case of service back to IS");
                boolean backinservice = true;
                for (int i = 0; i < pceatoz.size(); i++) {
                    if (pceatoz.get(i).getOperationalState().equals(State.OutOfService) || pceztoa.get(i)
                            .getOperationalState().equals(State.OutOfService)) {
                        backinservice = false;
                        LOG.info("Service cannot go back to IS");
                        break;
                    }
                }
                if (backinservice) {
                    LOG.info("Service back to IS");
                    this.serviceDataStoreOperations.modifyServiceNM(services.getServiceName(),
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types
                                    .rev181130.State.InService, AdminStates.InService, LifecycleState.Deployed,
                            pceatoz, pceztoa);
                }
            }
        }
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }
}
