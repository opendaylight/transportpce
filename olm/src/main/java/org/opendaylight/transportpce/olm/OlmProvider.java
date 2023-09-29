/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OlmProvider.
 */
@Component
public class OlmProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OlmProvider.class);
    private final Registration reg;

    /**
     * Instantiates a new olm provider.
     * @param olmPowerServiceRpc
     *            implementation of TransportpceOlmService
     * @param rpcProviderService
     *            the rpc provider service
     */
    @Activate
    public OlmProvider(@Reference final RpcProviderService rpcProviderService,
            @Reference final OlmPowerServiceRpcImpl olmPowerServiceRpc) {
        reg = olmPowerServiceRpc.registerWith(rpcProviderService);
        LOG.info("OlmProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @Deactivate
    public void close() {
        LOG.info("OlmProvider Closed");
        // Clean up the RPC service registration
        if (reg != null) {
            reg.close();
        }
    }
}
