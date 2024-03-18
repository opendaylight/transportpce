/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.impl;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PceService implementation.
 */
@Component(immediate = true)
public class PceServiceRPCImpl {
    private static final Logger LOG = LoggerFactory.getLogger(PceServiceRPCImpl.class);
    private Registration reg;

    @Activate
    public PceServiceRPCImpl(@Reference RpcProviderService rpcProviderService,
            @Reference PathComputationService pathComputationService) {
        this.reg = rpcProviderService.registerRpcImplementations(
                new CancelResourceReserveImpl(pathComputationService),
                new PathComputationRequestImpl(pathComputationService),
                new PathComputationRerouteRequestImpl(pathComputationService));
        LOG.info("PceServiceRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("PceServiceRPCImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return reg;
    }
}
