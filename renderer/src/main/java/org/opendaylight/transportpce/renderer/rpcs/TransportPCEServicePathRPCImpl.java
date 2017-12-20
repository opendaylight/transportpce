/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import java.util.concurrent.Future;

import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.TransportpceServicepathService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportPCEServicePathRPCImpl implements TransportpceServicepathService {

    private static final Logger LOG = LoggerFactory.getLogger(TransportPCEServicePathRPCImpl.class);

    private final RendererServiceOperations rendererServiceOperations;

    public TransportPCEServicePathRPCImpl(RendererServiceOperations rendererServiceOperations) {
        this.rendererServiceOperations = rendererServiceOperations;
    }

    @Override
    public Future<RpcResult<CancelResourceReserveOutput>> cancelResourceReserve(CancelResourceReserveInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<ServiceImplementationRequestOutput>> serviceImplementationRequest(
            ServiceImplementationRequestInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service impl request {} {}", serviceName);
        return ModelMappingUtils.createRpcResponse(rendererServiceOperations.serviceImplementation(input));
    }

    @Override
    public Future<RpcResult<PathComputationRequestOutput>> pathComputationRequest(PathComputationRequestInput input) {
        // TODO Auto-generated method stub
        return null;
    }

}

