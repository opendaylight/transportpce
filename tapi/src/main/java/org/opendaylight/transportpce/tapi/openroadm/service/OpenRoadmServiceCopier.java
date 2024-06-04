/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies an OpenROADM service to TAPI.
 */
public class OpenRoadmServiceCopier {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmServiceCopier.class);

    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiContext tapiContext;

    public OpenRoadmServiceCopier(
            DataBroker dataBroker,
            NetworkTransactionService networkTransactionService,
            ServiceDataStoreOperations serviceDataStoreOperations,
            TapiContext tapiContext) {

        this.dataBroker = dataBroker;
        this.networkTransactionService = networkTransactionService;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiContext = tapiContext;
    }

    /**
     * Copies an OpenROADM service to TAPI.
     *
     * @param serviceName the name of the service to copy
     * @return true if the service was successfully copied, false otherwise
     */
    public boolean copyToTapi(String serviceName) {
        LOG.info("Starting to copy OpenROADM service {} to TAPI...", serviceName);
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                this.serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                this.networkTransactionService,
                TapiProvider.TAPI_TOPO_UUID);

        Map<ConnectivityServiceKey, ConnectivityService> connectivityServices =
                Optional.ofNullable(tapiContext.getConnectivityServices()).orElse(new HashMap<>());

        if (tapiServiceExists(serviceName, connectivityServices)) {
            return false;
        }

        TapiLink tapiLink = new TapiLinkImpl(this.networkTransactionService, tapiContext);

        TopologyUtils topologyUtils = new TopologyUtils(this.networkTransactionService, this.dataBroker, tapiLink);

        TapiInitialORMapping tapiInitialORMapping = new TapiInitialORMapping(
                topologyUtils,
                connectivityUtils,
                tapiContext,
                this.serviceDataStoreOperations);

        if (tapiInitialORMapping.performServInitialMapping(serviceName)) {
            LOG.info("Done, OpenROADM service {} copied to TAPI!", serviceName);
            return true;
        }

        LOG.error("Copy OpenROADM service {} to TAPI failed!", serviceName);
        return false;
    }

    private boolean tapiServiceExists(
            String openRoadmServiceName,
            Map<ConnectivityServiceKey, ConnectivityService> connectivityServices) {

        for (Map.Entry<ConnectivityServiceKey, ConnectivityService> entry : connectivityServices.entrySet()) {
            for (Map.Entry<NameKey, Name> nme : Objects.requireNonNull(entry.getValue().getName()).entrySet()) {
                if (serviceNameEqualsTapiServiceName(openRoadmServiceName, nme.getValue())) {
                    LOG.info("Service {} already exists in TAPI", openRoadmServiceName);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean serviceNameEqualsTapiServiceName(String openRoadmServiceName, Name tapiServiceName) {
        return (tapiServiceName.getValueName().equals("Connectivity Service Name")
                && openRoadmServiceName.equals(tapiServiceName.getValue()));
    }
}
