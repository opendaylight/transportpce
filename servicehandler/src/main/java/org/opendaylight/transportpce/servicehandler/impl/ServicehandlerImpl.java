/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Top level service interface providing main OpenROADM controller services.
 */
@Component
public class ServicehandlerImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);

    private PCEServiceWrapper pceServiceWrapper;
    private RendererServiceWrapper rendererServiceWrapper;
    private Registration rpcRegistration;

    @Activate
    public ServicehandlerImpl(@Reference RpcProviderService rpcProviderService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference PceListener pceListenerImpl,
            @Reference RendererListener rendererListenerImpl,
            @Reference NetworkListener networkModelListenerImpl,
            @Reference CatalogDataStoreOperations catalogDataStoreOperations,
            @Reference PathComputationService pathComputationService,
            @Reference RendererServiceOperations rendererServiceOperations,
            @Reference NotificationPublishService notificationPublishService) {
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.rendererServiceWrapper = new RendererServiceWrapper(rendererServiceOperations, notificationPublishService);
        this.rpcRegistration = rpcProviderService.registerRpcImplementations(
                new ServiceCreateImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper, notificationPublishService),
                new ServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper, notificationPublishService),
                new ServiceFeasibilityCheckImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper),
                new ServiceReconfigureImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper),
                new ServiceRestorationImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, rendererServiceWrapper),
                new ServiceRerouteImpl(serviceDataStoreOperations, pceServiceWrapper),
                new TempServiceCreateImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        networkModelListenerImpl, pceServiceWrapper),
                new TempServiceDeleteImpl(serviceDataStoreOperations, pceListenerImpl, rendererListenerImpl,
                        rendererServiceWrapper),
                new AddOpenroadmOperationalModesToCatalogImpl(catalogDataStoreOperations),
                new AddSpecificOperationalModesToCatalogImpl(catalogDataStoreOperations));
        LOG.info("ServicehandlerImpl Initiated");
    }

    @Deactivate
    public void close() {
        this.rpcRegistration.close();
        LOG.info("ServicehandlerImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return rpcRegistration;
    }


    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        public static final String PCE_CALLING;
        public static final String ABORT_PCE_FAILED;
        public static final String PCE_FAILED;
        public static final String ABORT_SERVICE_NON_COMPLIANT;
        public static final String SERVICE_NON_COMPLIANT;
        public static final String RENDERER_DELETE_FAILED;
        public static final String ABORT_VALID_FAILED;
        public static final String ABORT_OR_TO_CATALOG_FAILED;
        public static final String ABORT_SPECIFIC_TO_CATALOG_FAILED;

        // Static blocks are generated once and spare memory.
        static {
            PCE_CALLING = "Calling PCE";
            ABORT_PCE_FAILED = "Aborting: PCE calculation failed ";
            PCE_FAILED = "PCE calculation failed";
            ABORT_SERVICE_NON_COMPLIANT = "Aborting: non-compliant service ";
            SERVICE_NON_COMPLIANT = "non-compliant service";
            RENDERER_DELETE_FAILED = "Renderer service delete failed";
            ABORT_VALID_FAILED = "Aborting: validation of service create request failed";
            ABORT_OR_TO_CATALOG_FAILED = "Aborting: validation of add OR to catalog request failed";
            ABORT_SPECIFIC_TO_CATALOG_FAILED = "Aborting: validation of add Specific to catalog request failed";
        }

        public static String serviceInDS(String serviceName) {
            return "Service '" + serviceName + "' already exists in datastore";
        }

        public static String serviceNotInDS(String serviceName) {
            return "Service '" + serviceName + "' does not exist in datastore";
        }

        public static String servicePathNotInDS(String serviceName) {
            return "Service Path from '" + serviceName + "' does not exist in datastore";
        }

        public static String serviceInService(String serviceName) {
            return "Service '" + serviceName + "' is in 'inService' state";
        }

        private LogMessages() {
        }
    }
}

