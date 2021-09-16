/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.TransportpceRendererService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportPCEServicePathRPCImpl implements TransportpceRendererService {

    private static final Logger LOG = LoggerFactory.getLogger(TransportPCEServicePathRPCImpl.class);

    private final RendererServiceOperations rendererServiceOperations;

    public TransportPCEServicePathRPCImpl(RendererServiceOperations rendererServiceOperations) {
        this.rendererServiceOperations = rendererServiceOperations;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service delete request {}", serviceName);
        ServiceDeleteOutput output = null;
        try {
            output = this.rendererServiceOperations.serviceDelete(input, null).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC service delete failed !", e);
        }
        return ModelMappingUtils.createServiceDeleteRpcResponse(output);
    }

    @Override
    public ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> serviceImplementationRequest(
            ServiceImplementationRequestInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service impl request {}", serviceName);
        ServiceImplementationRequestOutput output = null;
        try {
            output = this.rendererServiceOperations.serviceImplementation(input).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC service implementation failed !", e);
        }
        return ModelMappingUtils.createServiceImplementationRpcResponse(output);
    }

}

