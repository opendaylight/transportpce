/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.orchestrator;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* OSGi component orchestrating cross-domain services.
*   Used to initialize the path computation, notification, and network transaction services,
*   and to manage manage the lifecycle.
*/
@Component
public class CrossDomainServiceOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(CrossDomainServiceOrchestrator.class);
    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private PceCrossDomainOrchestrator pceCrossDomainOrchestrator;
    private NetworkTransactionService networkTransactionService;

    /**
    * Retrieves the singleton instance of the orchestrator, initializing dependencies.
    *
    * @param pcs the path computation service
    * @param nps the notification publish service
    * @param nts the network transaction service
    */
    @Activate
    public CrossDomainServiceOrchestrator(
//            @Reference RpcProviderService rpcProviderService,
            @Reference PathComputationService pcs,
            @Reference NotificationPublishService nps,
            @Reference NetworkTransactionService nts) {

        this.notificationPublishService = nps;
        this.pathComputationService = pcs;
        this.networkTransactionService = nts;
        this.pceCrossDomainOrchestrator = PceCrossDomainOrchestrator.getInstance(
                pathComputationService, notificationPublishService, networkTransactionService);
        LOG.info("CrossDomainServiceOrchestrator Initiated");
        if (pceCrossDomainOrchestrator != null) {
            LOG.info("PceCrossDomainOrchestrator Instantiated");
        }
    }

    /**
    * Called when the component is deactivated, performing cleanup.
    */
    @Deactivate
    public void close() {
        LOG.info("CrossDomainServiceOrchestrator Closed");
    }
}
