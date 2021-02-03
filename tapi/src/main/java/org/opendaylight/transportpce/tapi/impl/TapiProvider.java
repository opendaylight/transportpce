/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.connectivity.TapiConnectivityImpl;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyImpl;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.ServiceInterfacePoints;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register TAPI interface Service and Notification.
 *
 * @author Gilles Thouenon (gilles.thouenon@orange.com) on behalf of Orange
 *
 */
public class TapiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TapiProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderService;
    private ObjectRegistration<TapiConnectivityService> rpcRegistration;
    private final OrgOpenroadmServiceService serviceHandler;
    private final TapiListener tapiListener;
    private final NetworkTransactionService networkTransactionService;

    public TapiProvider(DataBroker dataBroker, RpcProviderService rpcProviderService,
            OrgOpenroadmServiceService serviceHandler, TapiListener tapiListener,
            NetworkTransactionService networkTransactionService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
        this.serviceHandler = serviceHandler;
        this.tapiListener = tapiListener;
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("TapiProvider Session Initiated");
        TapiContext tapiContext = new TapiContext(this.networkTransactionService);
        LOG.info("Empty TAPI context created: {}", tapiContext.getTapiContext());
        TapiConnectivityImpl tapi = new TapiConnectivityImpl(this.serviceHandler);
        TapiTopologyImpl topo = new TapiTopologyImpl(this.dataBroker);
        rpcRegistration = rpcProviderService.registerRpcImplementation(TapiConnectivityService.class, tapi);
        rpcProviderService.registerRpcImplementation(TapiTopologyService.class, topo);
        @NonNull
        InstanceIdentifier<ServiceInterfacePoints> sipIID = InstanceIdentifier.create(ServiceInterfacePoints.class);
        dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, sipIID), tapiListener);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("TapiProvider Session Closed");
        rpcRegistration.close();
    }

}
