/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiInitialORMapping {

    private static final Logger LOG = LoggerFactory.getLogger(TapiInitialORMapping.class);
    private static final int LOGGED_SERVICE_NAMES = 10;
    private static final Comparator<Services> BY_SERVICE_FORMAT =
        Comparator.comparing((Services serv) -> serv.getServiceAEnd().getServiceFormat().getName())
            .reversed()
            .thenComparing(Services::getServiceName);
    private final TapiContext tapiContext;
    private final TopologyUtils topologyUtils;
    private final ConnectivityUtils connectivityUtils;
    private final ServiceDataStoreOperations serviceDataStoreOperations;

    public TapiInitialORMapping(TopologyUtils topologyUtils, ConnectivityUtils connectivityUtils,
                                TapiContext tapiContext, ServiceDataStoreOperations serviceDataStoreOperations) {
        this.topologyUtils = topologyUtils;
        this.tapiContext = tapiContext;
        this.connectivityUtils = connectivityUtils;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
    }

    public void performTopoInitialMapping() {
        // creation of both topologies but with the fully roadm infrastructure.
        try {
            LOG.info("Performing initial mapping between OR and TAPI models.");
            Topology t0topology = this.topologyUtils.createOtnTopology();
            Map<TopologyKey, Topology> topologyMap = new HashMap<>();
            topologyMap.put(t0topology.key(), t0topology);
            this.tapiContext.updateTopologyContext(topologyMap);
            Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap = this.topologyUtils.getSipMap();
            this.tapiContext.updateSIPContext(sipMap);
            this.connectivityUtils.setSipMap(sipMap);
        } catch (TapiTopologyException e) {
            LOG.error("error building TAPI topology", e);
        }
    }

    public boolean performServInitialMapping() {
        Optional<ServiceList> optOrServices = this.serviceDataStoreOperations.getServices();
        if (!optOrServices.isPresent()) {
            LOG.error("Couldnt obtain OR services from datastore");
            return false;
        }

        return performServInitialMapping(optOrServices.orElseThrow());
    }

    public boolean performServInitialMapping(String serviceName) {
        LOG.info("Convert OpenROADM service {} to TAPI", serviceName);
        Optional<Services> optService = this.serviceDataStoreOperations.getService(serviceName);
        if (!optService.isPresent()) {
            LOG.warn("Service {} not found in datastore, aborting.", serviceName);
            return false;
        }
        Services services = optService.orElseThrow();
        ServiceListBuilder serviceListBuilder = new ServiceListBuilder();
        serviceListBuilder.setServices(Map.of(services.key(), services));

        return performServInitialMapping(serviceListBuilder.build());
    }

    public boolean performServInitialMapping(ServiceList orServices) {
        LOG.info("Performing initial service mapping between OR and TAPI models.");

        if (orServices.getServices() == null) {
            LOG.info("No services in datastore. No mapping needed");
            return false;
        }

        List<Services> orderedServices = sortBySupportingServices(orServices);

        List<String> firstServiceNames =
            orderedServices.stream().map(Services::getServiceName).limit(LOGGED_SERVICE_NAMES).toList();
        LOG.info("Mapping {} services, first {} in mapping order = {}",
            orderedServices.size(), firstServiceNames.size(), firstServiceNames);
        Map<ConnectivityServiceKey, ConnectivityService> connServMap = new HashMap<>();
        Map<ConnectionKey, Connection> connectionMap = new HashMap<>();
        int unmapped = 0;
        for (Service service:orderedServices) {
            // map services
            // connections needed to be created --> looking at path description
            ConnectivityService connServ = this.connectivityUtils.mapORServiceToTapiConnectivity(service);

            if (connServ == null) {
                LOG.warn("Couldn't map service {} to TAPI", service.getServiceName());
                unmapped++;
                continue;
            }

            connServMap.put(connServ.key(), connServ);
            // ConnectivityUtils clears its connection map on every service, so collect it before the next one
            connectionMap.putAll(this.connectivityUtils.getConnectionFullMap());
        }
        if (!connServMap.isEmpty()) {
            // Put in datastore connectivity services and connections
            this.tapiContext.updateConnectivityContext(connServMap, connectionMap);
        }

        if (unmapped > 0) {
            LOG.error("{} of the {} OpenROADM services could not be mapped to TAPI",
                unmapped, orderedServices.size());
            return false;
        }

        return true;
    }

    /**
     * Orders services so that supporting services in the batch are mapped before the services
     * that depend on them, while keeping each service stack together.
     *
     * <p>Services involved in or dependent on a cycle are appended in service-format order.
     *
     * @param services the services to order
     * @return the services in mapping order
     */
    @VisibleForTesting
    static List<Services> sortBySupportingServices(ServiceList services) {
        List<Services> unordered = new ArrayList<>(services.nonnullServices().values());
        unordered.sort(BY_SERVICE_FORMAT);
        Set<String> batch = unordered.stream().map(Services::getServiceName).collect(Collectors.toSet());

        // The services riding on each service, in format order because that is the order they are
        // collected in, and how many of the services each one rides on are still to be mapped.
        Map<String, List<Services>> supportedServices = new HashMap<>();
        Map<String, Integer> pendingSupport = new HashMap<>();
        for (Services service : unordered) {
            List<String> supportingServices = supportingServicesIn(batch, service);
            for (String supportingService : supportingServices) {
                supportedServices.computeIfAbsent(supportingService, name -> new ArrayList<>()).add(service);
            }
            pendingSupport.put(service.getServiceName(), supportingServices.size());
        }

        // One service stack at a time means depth first: take the services a service unblocks
        // before the ones that were already waiting, so LIFO rather than FIFO.
        Deque<Services> ready = new ArrayDeque<>();
        push(ready, unordered.stream().filter(service -> pendingSupport.get(service.getServiceName()) == 0).toList());
        List<Services> ordered = new ArrayList<>(unordered.size());
        while (!ready.isEmpty()) {
            Services service = ready.pop();
            ordered.add(service);
            push(ready, supportedServices.getOrDefault(service.getServiceName(), List.of()).stream()
                    .filter(supported -> pendingSupport.merge(supported.getServiceName(), -1, Integer::sum) == 0)
                    .toList());
        }

        if (ordered.size() < unordered.size()) {
            // Every service still waiting for a supporting service is in a cycle, or rides on one.
            List<Services> cyclic = unordered.stream()
                    .filter(service -> pendingSupport.get(service.getServiceName()) > 0)
                    .toList();
            LOG.warn("The supporting services of {} form a cycle, so the order they ride on each other in is not "
                    + "known. Mapping them by service format instead: {}",
                cyclic.size(), cyclic.stream().map(Services::getServiceName).toList());
            ordered.addAll(cyclic);
        }

        return ordered;
    }

    /**
     * Returns the names of the services the given service rides on that are being mapped alongside
     * it. A supporting service outside the batch orders nothing, so it is left out.
     *
     * @param batch the names of the services being mapped
     * @param service the service whose supporting services to look up
     * @return the names of the supporting services of the service that are in the batch
     */
    private static List<String> supportingServicesIn(Set<String> batch, Services service) {
        Set<String> supportingServices = service.getSupportingServiceName();

        return supportingServices == null
            ? List.of()
            : supportingServices.stream().filter(batch::contains).toList();
    }

    /**
     * Pushes services onto the stack in reverse, so that the first of them ends up on top of it and
     * the stack hands them back in the order they were given in.
     *
     * @param stack the stack to push onto
     * @param services the services to push
     */
    private static void push(Deque<Services> stack, List<Services> services) {
        for (int i = services.size() - 1; i >= 0; i--) {
            stack.push(services.get(i));
        }
    }
}
