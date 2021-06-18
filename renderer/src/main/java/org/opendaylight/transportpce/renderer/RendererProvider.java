/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.TransportpceDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.TransportpceRendererService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RendererProvider.class);
    private final RpcProviderService rpcProviderService;
    private DeviceRendererRPCImpl deviceRendererRPCImpl;
    private ObjectRegistration<DeviceRendererRPCImpl> deviceRendererRegistration;
    private ObjectRegistration<TransportpceRendererService> tpceServiceRegistry;
    private RendererServiceOperations rendererServiceOperations;

    public RendererProvider(RpcProviderService rpcProviderService, DeviceRendererRPCImpl deviceRendererRPCImpl,
            RendererServiceOperations rendererServiceOperations) {
        this.rpcProviderService = rpcProviderService;
        this.deviceRendererRPCImpl = deviceRendererRPCImpl;
        this.rendererServiceOperations = rendererServiceOperations;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("RendererProvider Session Initiated");
        TransportPCEServicePathRPCImpl transportPCEServicePathRPCImpl =
            new TransportPCEServicePathRPCImpl(this.rendererServiceOperations);
        this.deviceRendererRegistration = this.rpcProviderService
                .registerRpcImplementation(TransportpceDeviceRendererService.class, deviceRendererRPCImpl);
        this.tpceServiceRegistry = this.rpcProviderService
                .registerRpcImplementation(TransportpceRendererService.class, transportPCEServicePathRPCImpl);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("RendererProvider Closed");
        if (this.deviceRendererRegistration != null) {
            this.deviceRendererRegistration.close();
        }
        if (this.tpceServiceRegistry != null) {
            this.tpceServiceRegistry.close();
        }
    }

}
