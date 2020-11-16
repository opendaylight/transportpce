/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.impl;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TransportpceNetworkmodelListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
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
    private final RpcProviderService rpcService;
    private final NotificationService notificationService;
    private final NetworkModelWavelengthService networkModelWavelengthService;
    private final NotificationPublishService notificationPublishService;
    private ListenerRegistration<TransportpcePceListener> pcelistenerRegistration;
    private ListenerRegistration<TransportpceRendererListener> rendererlistenerRegistration;
    private ListenerRegistration<TransportpceNetworkmodelListener> networkmodellistenerRegistration;
    private ObjectRegistration<OrgOpenroadmServiceService> rpcRegistration;
    private PathComputationService pathComputationService;
    private RendererServiceOperations rendererServiceOperations;

    public ServicehandlerProvider(final DataBroker dataBroker, RpcProviderService rpcProviderService,
            NotificationService notificationService, PathComputationService pathComputationService,
            RendererServiceOperations rendererServiceOperations,
            NetworkModelWavelengthService networkModelWavelengthService,
            NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.rpcService = rpcProviderService;
        this.notificationService = notificationService;
        this.pathComputationService = pathComputationService;
        this.rendererServiceOperations = rendererServiceOperations;
        this.networkModelWavelengthService = networkModelWavelengthService;
        this.notificationPublishService = notificationPublishService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ServicehandlerProvider Session Initiated");
        final PceListenerImpl pceListenerImpl = new PceListenerImpl(rendererServiceOperations,
                pathComputationService, notificationPublishService, null);
        final RendererListenerImpl rendererListenerImpl =
                new RendererListenerImpl(pathComputationService, notificationPublishService);
        final NetworkModelListenerImpl networkModelListenerImpl =
                new NetworkModelListenerImpl(notificationPublishService, null);
        pcelistenerRegistration = notificationService.registerNotificationListener(pceListenerImpl);
        rendererlistenerRegistration = notificationService.registerNotificationListener(rendererListenerImpl);
        networkmodellistenerRegistration = notificationService.registerNotificationListener(networkModelListenerImpl);
        final ServicehandlerImpl servicehandler = new ServicehandlerImpl(dataBroker, pathComputationService,
                rendererServiceOperations, notificationPublishService, pceListenerImpl, rendererListenerImpl,
                networkModelListenerImpl, networkModelWavelengthService);
        rpcRegistration =
            rpcService.registerRpcImplementation(OrgOpenroadmServiceService.class, servicehandler);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ServicehandlerProvider Closed");
        pcelistenerRegistration.close();
        rendererlistenerRegistration.close();
        networkmodellistenerRegistration.close();
        rpcRegistration.close();
    }

}
