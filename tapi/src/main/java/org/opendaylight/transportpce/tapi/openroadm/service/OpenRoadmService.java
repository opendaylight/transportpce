/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.service;

import java.util.HashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmService.class);

    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiContext tapiContext;

    public OpenRoadmService(
            DataBroker dataBroker,
            NetworkTransactionService networkTransactionService,
            ServiceDataStoreOperations serviceDataStoreOperations,
            TapiContext tapiContext) {

        this.dataBroker = dataBroker;
        this.networkTransactionService = networkTransactionService;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiContext = tapiContext;
    }

    @Override
    public boolean copyServiceToTAPI(String openRoadmServiceName) {
        LOG.info("Starting to copy OpenROADM service {} to TAPI...", openRoadmServiceName);
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                this.serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                this.networkTransactionService,
                TapiProvider.TAPI_TOPO_UUID);

        TapiLink tapiLink = new TapiLinkImpl(this.networkTransactionService, tapiContext);

        TopologyUtils topologyUtils = new TopologyUtils(this.networkTransactionService, this.dataBroker, tapiLink);

        TapiInitialORMapping tapiInitialORMapping = new TapiInitialORMapping(
                topologyUtils,
                connectivityUtils,
                tapiContext,
                this.serviceDataStoreOperations);

        if (tapiInitialORMapping.performServInitialMapping(openRoadmServiceName)) {
            LOG.info("Done, OpenROADM service {} copied to TAPI!", openRoadmServiceName);
            return true;
        }

        LOG.error("Copy OpenROADM service {} to TAPI failed!", openRoadmServiceName);
        return false;
    }

}
