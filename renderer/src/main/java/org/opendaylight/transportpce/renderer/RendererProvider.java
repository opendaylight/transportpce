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
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRenderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.RendererService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RendererProvider.class);
    private final DataBroker dataBroker;
    private final MountPointService mountPointService;
    private final RpcProviderRegistry rpcProviderRegistry;
    private RendererNotificationsImpl rendererNotificationImpl;
    private RpcRegistration<RendererService> deviceRendererRegistration;
    private final Set<String> currentMountedDevice;

    public RendererProvider(final DataBroker dataBroker, final MountPointService mountPointService,
        final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.mountPointService = mountPointService;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.currentMountedDevice = new HashSet<>();
        if (mountPointService == null) {
            LOG.error("Mount service is null");
        }
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("RendererProvider Session Initiated");
        // Initializing Notification module
        rendererNotificationImpl = new RendererNotificationsImpl(dataBroker, mountPointService,
            currentMountedDevice);
        //Register REST API RPC implementation for Renderer Service
        deviceRendererRegistration = rpcProviderRegistry.addRpcImplementation(RendererService.class, new DeviceRenderer(
            dataBroker, mountPointService, currentMountedDevice));
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("RendererProvider Closed");
        // Clean up the RPC service registration
        if (deviceRendererRegistration != null) {
            deviceRendererRegistration.close();
        }
        // Clean up the RendererNotificationsImpl
        if (rendererNotificationImpl != null) {
            rendererNotificationImpl.close();
        }
    }
}