/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.impl;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.CancelResourceReserve;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRerouteRequest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRerouteRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRerouteRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.TransportpcePceService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
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
public class PceServiceRPCImpl implements TransportpcePceService {

    private static final Logger LOG = LoggerFactory.getLogger(PceServiceRPCImpl.class);

    private final PathComputationService pathComputationService;
    private Registration reg;

    @Activate
    public PceServiceRPCImpl(@Reference RpcProviderService rpcProviderService,
            @Reference PathComputationService pathComputationService) {
        this.pathComputationService = pathComputationService;
        this.reg = rpcProviderService.registerRpcImplementations(ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(CancelResourceReserve.class, this::cancelResourceReserve)
            .put(PathComputationRequest.class, this::pathComputationRequest)
            .put(PathComputationRerouteRequest.class, this::pathComputationRerouteRequest)
            .build());

        LOG.info("PceServiceRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("PceServiceRPCImpl Closed");
    }

    @Override
    public final ListenableFuture<RpcResult<CancelResourceReserveOutput>>
            cancelResourceReserve(CancelResourceReserveInput input) {
        LOG.info("RPC cancelResourceReserve request received");
        try {
            return RpcResultBuilder
                    .success(
                            this.pathComputationService.cancelResourceReserve(input).get())
                    .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC cancelResourceReserve failed !", e);
            return RpcResultBuilder.success((CancelResourceReserveOutput) null).buildFuture();
        }
    }

    @Override
    public final ListenableFuture<RpcResult<PathComputationRequestOutput>>
            pathComputationRequest(PathComputationRequestInput input) {
        LOG.info("RPC path computation request received");
        LOG.debug("input parameters are : input = {}", input);
        try {
            return RpcResultBuilder
                    .success(
                            this.pathComputationService.pathComputationRequest(input).get())
                    .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC path computation request failed !", e);
        }
        return RpcResultBuilder.success((PathComputationRequestOutput) null).buildFuture();
    }

    @Override
    public final ListenableFuture<RpcResult<PathComputationRerouteRequestOutput>> pathComputationRerouteRequest(
            PathComputationRerouteRequestInput input) {
        LOG.info("RPC path computation reroute request received");
        LOG.debug("input parameters are : input = {}", input);
        try {
            return RpcResultBuilder
                    .success(
                            this.pathComputationService.pathComputationRerouteRequest(input).get())
                    .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC path computation request failed !", e);
            return RpcResultBuilder.success((PathComputationRerouteRequestOutput) null).buildFuture();
        }
    }

    public Registration getRegisteredRpc() {
        return reg;
    }
}
