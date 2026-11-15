/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiControllerService;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiDiscoveryService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the TAPI SBI controller RPCs with the MD-SAL RPC provider.
 */
@Component
public class TapiSbiControllerRPCImpl {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiControllerRPCImpl.class);

    private final Registration reg;

    @Activate
    public TapiSbiControllerRPCImpl(@Reference RpcProviderService rpcProviderService,
            @Reference TapiSbiControllerService controllerService,
            @Reference TapiSbiDiscoveryService discoveryService) {
        this.reg = rpcProviderService.registerRpcImplementations(
            new ConfigureTapiSbiControllerImpl(controllerService),
            new DeleteTapiSbiControllerImpl(controllerService),
            new SyncTapiSbiControllerImpl(controllerService, discoveryService));
        LOG.info("TapiSbiControllerRPCImpl instantiated");
    }

    @Deactivate
    public void close() {
        reg.close();
        LOG.info("TapiSbiControllerRPCImpl closed");
    }

    public Registration getRegisteredRpc() {
        return reg;
    }
}
