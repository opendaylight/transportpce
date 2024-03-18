/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.olm.rpc.impl.CalculateSpanlossBaseImpl;
import org.opendaylight.transportpce.olm.rpc.impl.CalculateSpanlossCurrentImpl;
import org.opendaylight.transportpce.olm.rpc.impl.GetPmImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerResetImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerSetupImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerTurndownImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OlmPowerServiceRpcImpl.
 */
@Component
public class OlmPowerServiceRpcImpl {
    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerServiceRpcImpl.class);
    private Registration rpcRegistration;

    @Activate
    public OlmPowerServiceRpcImpl(@Reference OlmPowerService olmPowerService,
            @Reference RpcProviderService rpcProviderService) {
        this.rpcRegistration = rpcProviderService.registerRpcImplementations(
            new GetPmImpl(olmPowerService),
            new ServicePowerSetupImpl(olmPowerService),
            new ServicePowerTurndownImpl(olmPowerService),
            new CalculateSpanlossBaseImpl(olmPowerService),
            new CalculateSpanlossCurrentImpl(olmPowerService),
            new ServicePowerResetImpl(olmPowerService));
        LOG.info("OlmPowerServiceRpcImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.rpcRegistration.close();
        LOG.info("OlmPowerServiceRpcImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return rpcRegistration;
    }
}
