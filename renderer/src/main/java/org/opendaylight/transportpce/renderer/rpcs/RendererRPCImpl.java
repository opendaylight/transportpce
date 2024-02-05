/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequest;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class RendererRPCImpl {

    private static final Logger LOG = LoggerFactory.getLogger(RendererRPCImpl.class);
    private Registration reg;

    @Activate
    public RendererRPCImpl(@Reference RendererServiceOperations rendererServiceOperations,
            @Reference RpcProviderService rpcProviderService) {
        this.reg = rpcProviderService.registerRpcImplementations(ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(ServiceImplementationRequest.class, new ServiceImplementationRequestImpl(rendererServiceOperations))
            .put(ServiceDelete.class, new ServiceDeleteImpl(rendererServiceOperations))
            .build());
        LOG.debug("TransportPCEServicePathRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("TransportPCEServicePathRPCImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return reg;
    }

}
