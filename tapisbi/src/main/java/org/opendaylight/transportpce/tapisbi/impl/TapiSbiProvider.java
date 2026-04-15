/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapisbi.rpcs.TapiSbiServiceDeleteImpl;
import org.opendaylight.transportpce.tapisbi.rpcs.TapiSbiServiceImplementationRequestImpl;
import org.opendaylight.transportpce.tapisbi.rpcs.TapiSbiServicePathImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapisbi.listener.TapiSbiNotificationSubcriptionService;
import org.opendaylight.transportpce.tapisbi.listener.TapiSbiRendererNotificationHandler;
import org.opendaylight.transportpce.tapisbi.listener.TapiSbiServiceNotificationHandler;
import org.opendaylight.transportpce.tapisbi.listener.TapiSbiTopologyNotificationHandler;
import org.opendaylight.transportpce.tapisbi.renderer.TapiSbiRendererService;
import org.opendaylight.transportpce.tapisbi.renderer.TapiSbiRendererServiceImpl;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to register TAPI interface Service and Notification.
 *
 * @author Gilles Thouenon (gilles.thouenon@orange.com) on behalf of Orange
 *
 */
@Component
public class TapiSbiProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiProvider.class);

    private final NetworkModelService netModServ;
    private final TapiPceNotificationHandler pceNotificationHandler;
    private List<Registration> listeners;
    private Registration rpcRegistration;
    private Registration rendererlistenerRegistration;
    private Registration servicehandlerlistenerRegistration;
    private Registration tapinetworkmodellistenerRegistration;
    private Registration sbiTopologylistenerRegistration;
    private Registration sbiServicelistenerRegistration;

    @Activate
    public TapiSbiProvider(@Reference DataBroker dataBroker,
            @Reference RpcProviderService rpcProviderService,
            @Reference RpcService rpcService,
            @Reference NotificationService notificationService,
            @Reference NotificationPublishService notificationPublishService,
            @Reference NetworkTransactionService networkTransactionService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference NetworkModelService networkModelService,
            @Reference TapiSbiServiceNotificationHandler tapiSbiServiceNotificationHandler,
            @Reference TapiSbiTopologyNotificationHandler tapiSbiTopologyNotificationHandler,
            @Reference TapiSbiNotificationSubcriptionService tapiSbiNotificationsubscriber,
            @Reference TapiNetworkModelService tapiNetworkModelServiceImpl,
            @Reference TapiPceNotificationHandler tapiPceNotificationHandler,
            @Reference TapiLink tapiLink,
            @Reference TapiContext tapiContext) {

        this.pceNotificationHandler = tapiPceNotificationHandler;
        this.netModServ = networkModelService;
        LOG.info("TapiSbiProvider Session Initiated");
        LOG.info("Empty TAPI context created: {}", tapiContext.getTapiContext());
        TapiSbiServiceNotificationHandler sbiServiceListener = new TapiSbiServiceNotificationHandler(
            notificationPublishService);
        TapiSbiTopologyNotificationHandler sbiTopologyListener = new TapiSbiTopologyNotificationHandler(
            notificationPublishService);
        // TapiPceNotificationHandler instantiated from TAPI feature -> not to be needed in TAPI SBI
        TapiSbiRendererNotificationHandler rendererListener = new TapiSbiRendererNotificationHandler();
        TapiSbiRendererService sbiRenderer = new TapiSbiRendererServiceImpl();

        rpcRegistration = rpcProviderService.registerRpcImplementations(
            new TapiSbiServiceImplementationRequestImpl(rendererListener, pceNotificationHandler),
            new TapiSbiServiceDeleteImpl(rendererListener),
            new TapiSbiServicePathImpl(sbiRenderer));

        this.listeners = new ArrayList<>();

        // Notification Listener
        rendererlistenerRegistration = notificationService
            .registerCompositeListener(rendererListener.getCompositeListener());
        LOG.debug("Renderer Listener Registration in TapiSbiProvider : {}", rendererlistenerRegistration);
        listeners.add(rendererlistenerRegistration);
        sbiTopologylistenerRegistration = notificationService
            .registerCompositeListener(sbiTopologyListener.getCompositeListener());
        LOG.debug("Sbi Topology Listener Registration in TapiSbiProvider : {}", rendererlistenerRegistration);
        listeners.add(sbiTopologylistenerRegistration);
        sbiServicelistenerRegistration = notificationService
            .registerCompositeListener(sbiServiceListener.getCompositeListener());
        LOG.debug("Sbi Topology Listener Registration in TapiSbiProvider : {}", rendererlistenerRegistration);
        listeners.add(sbiServicelistenerRegistration);
        sbiServicelistenerRegistration = notificationService
            .registerCompositeListener(sbiServiceListener.getCompositeListener());
        LOG.debug("Sbi Topology Listener Registration in TapiSbiProvider : {}", rendererlistenerRegistration);
        listeners.add(rendererlistenerRegistration);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @Deactivate
    public void close() {
        netModServ.deleteTapiExtNode();
        listeners.forEach(lis -> lis.close());
        listeners.clear();
        rendererlistenerRegistration.close();
        servicehandlerlistenerRegistration.close();
        tapinetworkmodellistenerRegistration.close();
        rpcRegistration.close();
        LOG.info("TapiProvider Session Closed");
    }

    public Registration getRegisteredRpcs() {
        return rpcRegistration;
    }
}
