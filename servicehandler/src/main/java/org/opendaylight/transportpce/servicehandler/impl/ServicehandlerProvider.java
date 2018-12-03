/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.OrgOpenroadmServiceService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register
 * Servicehandler Service and Notification.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class ServicehandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerProvider.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationService notificationService;
    private final NetworkModelWavelengthService networkModelWavelengthService;
    private ListenerRegistration<TransportpcePceListener> pcelistenerRegistration;
    private RpcRegistration<OrgOpenroadmServiceService> rpcRegistration;
    private PathComputationService pathComputationService;
    private RendererServiceOperations rendererServiceOperations;

    public ServicehandlerProvider(final DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry,
            NotificationService notificationService, PathComputationService pathComputationService,
            RendererServiceOperations rendererServiceOperations,
            NetworkModelWavelengthService networkModelWavelengthService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcProviderRegistry;
        this.notificationService = notificationService;
        this.pathComputationService = pathComputationService;
        this.rendererServiceOperations = rendererServiceOperations;
        this.networkModelWavelengthService = networkModelWavelengthService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ServicehandlerProvider Session Initiated");
        final ServicehandlerImpl servicehandler = new ServicehandlerImpl(dataBroker, pathComputationService,
                rendererServiceOperations, networkModelWavelengthService);
        final PceListenerImpl pceListener = new PceListenerImpl();
        rpcRegistration = rpcRegistry.addRpcImplementation(OrgOpenroadmServiceService.class, servicehandler);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ServicehandlerProvider Closed");
        pcelistenerRegistration.close();
        rpcRegistration.close();
    }

}
