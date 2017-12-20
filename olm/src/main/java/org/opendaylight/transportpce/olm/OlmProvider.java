/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.OlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OlmProvider.
 */
public class OlmProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OlmProvider.class);
    private final RpcProviderRegistry rpcProviderRegistry;
    private final OlmPowerService olmPowerService;
    private RpcRegistration<OlmService> olmRPCRegistration;

    /**
     * Instantiates a new olm provider.
     * @param olmPowerService
     *            implementation of OlmService
     * @param rpcProviderRegistry
     *            the rpc provider registry
     */
    public OlmProvider(final RpcProviderRegistry rpcProviderRegistry, final OlmPowerService olmPowerService) {
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.olmPowerService = olmPowerService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("OlmProvider Session Initiated");
        // Initializing Notification module
        olmRPCRegistration = rpcProviderRegistry.addRpcImplementation(OlmService.class, new OlmPowerServiceRpcImpl(
            this.olmPowerService));
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