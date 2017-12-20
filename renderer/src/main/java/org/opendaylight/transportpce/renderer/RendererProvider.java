/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.TransportpceServicepathService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.RendererService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RendererProvider.class);
    private final DataBroker dataBroker;
    private final MountPointService mountPointService;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final PortMapping portMapping;
    private final DeviceTransactionManager deviceTransactionManager;
    private RpcRegistration<RendererService> deviceRendererRegistration;
    private DeviceRendererRPCImpl deviceRendererRPCImpl;
    private RpcRegistration<TransportpceServicepathService> tpceServiceRegistry;
    private RendererServiceOperations rendererServiceOperations;
    private RendererNotificationsImpl rendererNotificationsImpl;
    private final Set<String> currentMountedDevice;

    public RendererProvider(RpcProviderRegistry rpcProviderRegistry, DeviceRendererRPCImpl deviceRendererRPCImpl,
                            RendererServiceOperations rendererServiceOperations,DataBroker dataBroker,
                            MountPointService mountPointService, PortMapping portMapping,
                            DeviceTransactionManager deviceTransactionManager) {
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.deviceRendererRPCImpl = deviceRendererRPCImpl;
        this.rendererServiceOperations = rendererServiceOperations;
        this.dataBroker = dataBroker;
        this.mountPointService = mountPointService;
        this.currentMountedDevice = new HashSet<>();
        this.portMapping = portMapping;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("RendererProvider Session Initiated");
        TransportPCEServicePathRPCImpl transportPCEServicePathRPCImpl =
            new TransportPCEServicePathRPCImpl(this.rendererServiceOperations);
        this.deviceRendererRegistration = this.rpcProviderRegistry
                .addRpcImplementation(RendererService.class, this.deviceRendererRPCImpl);
        this.tpceServiceRegistry = this.rpcProviderRegistry
                .addRpcImplementation(TransportpceServicepathService.class, transportPCEServicePathRPCImpl);
        this.rendererNotificationsImpl = new RendererNotificationsImpl(this.dataBroker, this.mountPointService,
                this.currentMountedDevice,this.portMapping,this.deviceTransactionManager);
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
        // Clean up the RendererNotificationsImpl
        if (this.rendererNotificationsImpl != null) {
            this.rendererNotificationsImpl.close();
        }
    }

}
