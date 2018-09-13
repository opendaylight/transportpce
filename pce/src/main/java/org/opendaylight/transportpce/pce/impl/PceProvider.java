/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.TransportpcePceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class to register
 * Pce Service & Notification.
 */
public class PceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PceProvider.class);

    private final RpcProviderRegistry rpcRegistry;
    private final PathComputationService pathComputationService;
    private BindingAwareBroker.RpcRegistration<TransportpcePceService> rpcRegistration;

    public PceProvider(RpcProviderRegistry rpcProviderRegistry, PathComputationService pathComputationService) {
        this.rpcRegistry = rpcProviderRegistry;
        this.pathComputationService = pathComputationService;
    }

    /*
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("PceProvider Session Initiated");
        final PceServiceRPCImpl consumer = new PceServiceRPCImpl(pathComputationService);
        rpcRegistration = rpcRegistry.addRpcImplementation(TransportpcePceService.class, consumer);
    }

    /*
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("PceProvider Closed");
        rpcRegistration.close();
    }

}
