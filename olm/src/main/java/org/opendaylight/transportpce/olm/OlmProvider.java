/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OlmProvider.
 */
public class OlmProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OlmProvider.class);
    private final RpcProviderService rpcProviderService;
    private final OlmPowerService olmPowerService;
    private ObjectRegistration<TransportpceOlmService> olmRPCRegistration;

    /**
     * Instantiates a new olm provider.
     * @param olmPowerService
     *            implementation of OlmService
     * @param rpcProviderService
     *            the rpc provider service
     */
    public OlmProvider(final RpcProviderService rpcProviderService, final OlmPowerService olmPowerService) {
        this.rpcProviderService = rpcProviderService;
        this.olmPowerService = olmPowerService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("OlmProvider Session Initiated");
        // Initializing Notification module
        olmRPCRegistration = rpcProviderService.registerRpcImplementation(TransportpceOlmService.class,
                new OlmPowerServiceRpcImpl(this.olmPowerService));
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("RendererProvider Closed");
        // Clean up the RPC service registration
        if (olmRPCRegistration != null) {
            olmRPCRegistration.close();
        }
    }
}
