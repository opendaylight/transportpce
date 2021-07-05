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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.TransportpcePceService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class to register
 * Pce Service & Notification.
 */
public class PceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PceProvider.class);

    private final RpcProviderService rpcService;
    private final PathComputationService pathComputationService;
    private ObjectRegistration<PceServiceRPCImpl> rpcRegistration;

    public PceProvider(RpcProviderService rpcProviderService, PathComputationService pathComputationService) {
        this.rpcService = rpcProviderService;
        this.pathComputationService = pathComputationService;
    }

    /*
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("PceProvider Session Initiated");
        final PceServiceRPCImpl consumer = new PceServiceRPCImpl(pathComputationService);
        rpcRegistration = rpcService.registerRpcImplementation(TransportpcePceService.class, consumer);
    }

    /*
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("PceProvider Closed");
        rpcRegistration.close();
    }

}
