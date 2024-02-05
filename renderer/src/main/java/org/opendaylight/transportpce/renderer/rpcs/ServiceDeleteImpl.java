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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceDeleteImpl implements ServiceDelete {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeleteImpl.class);
    private final RendererServiceOperations rendererServiceOperations;

    public ServiceDeleteImpl(final RendererServiceOperations rendererServiceOperations) {
        this.rendererServiceOperations = requireNonNull(rendererServiceOperations);
    }

    @Override
    public ListenableFuture<RpcResult<ServiceDeleteOutput>> invoke(ServiceDeleteInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service delete request {}", serviceName);
        ServiceDeleteOutput output = null;
        try {
            output = this.rendererServiceOperations.serviceDelete(input, null).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("RPC service delete failed !", e);
            Thread.currentThread().interrupt();
        }
        return ModelMappingUtils.createServiceDeleteRpcResponse(output);
    }

}
