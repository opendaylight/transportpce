/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.OlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OlmProvider.
 */
public class OlmProvider {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(OlmProvider.class);

    /** The data broker. */
    private final DataBroker dataBroker;

    /** The mount point service. */
    private final MountPointService mountPointService;

    /** The rpc provider registry. */
    private final RpcProviderRegistry rpcProviderRegistry;

    /** The get pm registration. */
    private RpcRegistration<OlmService> olmRPCRegistration;

    /**
     * Instantiates a new olm provider.
     *
     * @param dataBroker
     *            the data broker
     * @param mountPointService
     *            the mount point service
     * @param rpcProviderRegistry
     *            the rpc provider registry
     */
    public OlmProvider(final DataBroker dataBroker, final MountPointService mountPointService,
        final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.mountPointService = mountPointService;
        this.rpcProviderRegistry = rpcProviderRegistry;
        if (mountPointService == null) {
            LOG.error("Mount service is null");
        }
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("OlmProvider Session Initiated");
        // Initializing Notification module
        olmRPCRegistration = rpcProviderRegistry.addRpcImplementation(OlmService.class, new OlmPowerSetupImpl(
            dataBroker,mountPointService));
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("RendererProvider Closed");
        // Clean up the RPC service registration
        if (olmRPCRegistration != null) {
            olmRPCRegistration.close();
        }
    }
}