package org.opendaylight.transportpce.tapi.utils;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
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
    private final ServiceHandlerOperations serviceHandler;

    public TapiInitialORMapping(TopologyUtils topologyUtils, TapiContext tapiContext,
                                ServiceHandlerOperations serviceHandler) {
        this.topologyUtils = topologyUtils;
        this.tapiContext = tapiContext;
        this.serviceHandler = serviceHandler;
    }

    public void performTopoInitialMapping() {
        // TODO: creation of both topologies but with the fully roadm infrastructure.
        try {
            LOG.info("Performing initial mapping between OR and TAPI models.");
            Topology t0MultiLayer = this.topologyUtils.createAbstractedOtnTopology();
            Topology tpdr100GB = this.topologyUtils.createAbstracted100GTpdrTopology(t0MultiLayer);
            Map<TopologyKey, Topology> topologyMap = new HashMap<>();
            topologyMap.put(t0MultiLayer.key(), t0MultiLayer);
            topologyMap.put(tpdr100GB.key(), tpdr100GB);
            this.tapiContext.updateTopologyContext(topologyMap);
            Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap = this.topologyUtils.getSipMap();
            this.tapiContext.updateSIPContext(sipMap);
        } catch (TapiTopologyException e) {
            LOG.error("error building TAPI topology", e);
        }
    }

    public void performServInitialMapping() {
        ServiceList orServices = this.serviceHandler.getORServices();
        ConnectivityUtils connUtils = new ConnectivityUtils(this.serviceHandler);
        if (orServices.getServices() == null) {
            LOG.info("No services in datastore. No mapping needed");
            return;
        }
        Map<ConnectivityServiceKey, ConnectivityService> connServMap = new HashMap<>();
        for (Service service:orServices.getServices().values()) {
            // TODO: map services
            // TODO: endpoints of service
            // TODO: connections needed to be created --> look at path description
            ConnectivityService connServ = connUtils.mapORServiceToTapiConnectivity(service);
            connServMap.put(connServ.key(), connServ);
        }
        LOG.info("Coonectivity Map {}", connServMap.size());
    }
}
