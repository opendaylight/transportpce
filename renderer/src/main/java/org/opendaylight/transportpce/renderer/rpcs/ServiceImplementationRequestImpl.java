/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceImplementationRequestImpl implements ServiceImplementationRequest {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImplementationRequestImpl.class);
    private final RendererServiceOperations rendererServiceOperations;

    public ServiceImplementationRequestImpl(final RendererServiceOperations rendererServiceOperations) {
        this.rendererServiceOperations = requireNonNull(rendererServiceOperations);
    }

    @Override
    public ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> invoke(
            ServiceImplementationRequestInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service impl request {}", serviceName);
        ServiceImplementationRequestOutput output = null;
        try {
            output = this.rendererServiceOperations.serviceImplementation(input, false).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC service implementation failed !", e);
            Thread.currentThread().interrupt();
        }
        return ModelMappingUtils.createServiceImplementationRpcResponse(output);
    }

}
