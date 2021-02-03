/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiInitialORMapping {

    private static final Logger LOG = LoggerFactory.getLogger(TapiInitialORMapping.class);
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
            Topology t0FullMultiLayer = this.topologyUtils.createFullOtnTopology();
            Map<TopologyKey, Topology> topologyMap = new HashMap<>();
            topologyMap.put(t0FullMultiLayer.key(), t0FullMultiLayer);
            this.tapiContext.updateTopologyContext(topologyMap);
            Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap = this.topologyUtils.getSipMap();
            this.tapiContext.updateSIPContext(sipMap);
            this.connectivityUtils.setSipMap(sipMap);
        } catch (TapiTopologyException e) {
            LOG.error("error building TAPI topology", e);
        }
    }

    public void performServInitialMapping() {
        Optional<ServiceList> optOrServices = this.serviceDataStoreOperations.getServices();
        if (!optOrServices.isPresent()) {
            LOG.error("Couldnt obtain OR services from datastore");
            return;
        }
        ServiceList orServices = optOrServices.get();
        if (orServices.getServices() == null) {
            LOG.info("No services in datastore. No mapping needed");
            return;
        }
        Map<ConnectivityServiceKey, ConnectivityService> connServMap = new HashMap<>();
        for (Service service:orServices.getServices().values()) {
            // map services
            // connections needed to be created --> looking at path description
            ConnectivityService connServ = this.connectivityUtils.mapORServiceToTapiConnectivity(service);
            connServMap.put(connServ.key(), connServ);
        }
        // Put in datastore connectivity services and connections
        this.tapiContext.updateConnectivityContext(connServMap, this.connectivityUtils.getConnectionFullMap());
    }
}
