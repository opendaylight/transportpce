/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class RendererRPCImpl {

    private static final Logger LOG = LoggerFactory.getLogger(RendererRPCImpl.class);
    // Maximum number of requests in progress for each RPC. The renderer runs 4 services at a time, so this allows a
    // few to wait without letting requests pile up behind an unresponsive device.
    private static final int MAX_REQUESTS_IN_PROGRESS = 16;
    private Registration reg;

    @Activate
    public RendererRPCImpl(@Reference RendererServiceOperations rendererServiceOperations,
            @Reference RpcProviderService rpcProviderService) {
        this.reg = rpcProviderService.registerRpcImplementations(
                new ServiceImplementationRequestImpl(rendererServiceOperations, MAX_REQUESTS_IN_PROGRESS),
                new ServiceDeleteImpl(rendererServiceOperations));
        LOG.debug("TransportPCEServicePathRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("TransportPCEServicePathRPCImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return reg;
    }

}
