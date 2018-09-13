/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev170228.TransportpceDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.TransportpceServicepathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RendererProvider.class);
    private final RpcProviderRegistry rpcProviderRegistry;
    private DeviceRendererRPCImpl deviceRendererRPCImpl;
    private RpcRegistration<TransportpceDeviceRendererService> deviceRendererRegistration;
    private RpcRegistration<TransportpceServicepathService> tpceServiceRegistry;
    private RendererServiceOperations rendererServiceOperations;

    public RendererProvider(RpcProviderRegistry rpcProviderRegistry, DeviceRendererRPCImpl deviceRendererRPCImpl,
            RendererServiceOperations rendererServiceOperations) {
        this.rpcProviderRegistry = rpcProviderRegistry;
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
        this.deviceRendererRegistration = this.rpcProviderRegistry
                .addRpcImplementation(TransportpceDeviceRendererService.class, deviceRendererRPCImpl);
        this.tpceServiceRegistry = this.rpcProviderRegistry
                .addRpcImplementation(TransportpceServicepathService.class, transportPCEServicePathRPCImpl);
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
