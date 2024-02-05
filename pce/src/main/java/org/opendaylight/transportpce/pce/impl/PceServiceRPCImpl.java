/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.impl;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserve;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequest;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
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
        this.reg = rpcProviderService.registerRpcImplementations(ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(CancelResourceReserve.class, new CancelResourceReserveImpl(pathComputationService))
            .put(PathComputationRequest.class, new PathComputationRequestImpl(pathComputationService))
            .put(PathComputationRerouteRequest.class, new PathComputationRerouteRequestImpl(pathComputationService))
            .build());
        LOG.info("PceServiceRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("PceServiceRPCImpl Closed");
    }

}
