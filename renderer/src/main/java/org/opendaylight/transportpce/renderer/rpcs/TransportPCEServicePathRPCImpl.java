/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.renderer.ModelMappingUtils;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.TransportpceRendererService;
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
        LOG.info("Calling RPC service delete request {} {}", serviceName);
        return ModelMappingUtils
                .createServiceDeleteRpcResponse(this.rendererServiceOperations.serviceDelete(input));
    }

    @Override
    public ListenableFuture<RpcResult<ServiceImplementationRequestOutput>> serviceImplementationRequest(
            ServiceImplementationRequestInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service impl request {} {}", serviceName);
        return ModelMappingUtils
                .createServiceImplementationRpcResponse(this.rendererServiceOperations.serviceImplementation(input));
    }

}

