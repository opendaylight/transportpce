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
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyImpl;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.ServiceInterfacePoints;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
    private ObjectRegistration<TapiTopologyService> rpcRegistration2;
    private final ServiceHandlerOperations serviceHandler;
    private ListenerRegistration<TapiListener> listenerRegistration;
    private final TapiListener tapiListener;

    public TapiProvider(DataBroker dataBroker, RpcProviderService rpcProviderService,
            ServiceHandlerOperations serviceHanlder, TapiListener tapiListener) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
        this.serviceHandler = serviceHanlder;
        this.tapiListener = tapiListener;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("TapiProvider Session Initiated");
        TapiImpl tapi = new TapiImpl(this.serviceHandler);
        TapiTopologyImpl topo = new TapiTopologyImpl(this.dataBroker);
        rpcRegistration = rpcProviderService.registerRpcImplementation(TapiConnectivityService.class, tapi);
        rpcRegistration2 = rpcProviderService.registerRpcImplementation(TapiTopologyService.class, topo);
        @NonNull
        InstanceIdentifier<ServiceInterfacePoints> sipIID = InstanceIdentifier.create(ServiceInterfacePoints.class);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, sipIID), tapiListener);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("TapiProvider Session Closed");
        // pcelistenerRegistration.close();
        rpcRegistration.close();
    }

}
