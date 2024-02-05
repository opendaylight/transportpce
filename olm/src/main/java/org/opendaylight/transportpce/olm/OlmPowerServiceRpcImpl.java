/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.olm.rpc.impl.CalculateSpanlossBaseImpl;
import org.opendaylight.transportpce.olm.rpc.impl.CalculateSpanlossCurrentImpl;
import org.opendaylight.transportpce.olm.rpc.impl.GetPmImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerResetImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerSetupImpl;
import org.opendaylight.transportpce.olm.rpc.impl.ServicePowerTurndownImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBase;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrent;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPm;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerReset;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndown;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
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
                ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(GetPm.class, new GetPmImpl(olmPowerService))
            .put(ServicePowerSetup.class, new ServicePowerSetupImpl(olmPowerService))
            .put(ServicePowerTurndown.class, new ServicePowerTurndownImpl(olmPowerService))
            .put(CalculateSpanlossBase.class, new CalculateSpanlossBaseImpl(olmPowerService))
            .put(CalculateSpanlossCurrent.class, new CalculateSpanlossCurrentImpl(olmPowerService))
            .put(ServicePowerReset.class, new ServicePowerResetImpl(olmPowerService))
            .build());
        LOG.info("OlmPowerServiceRpcImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.rpcRegistration.close();
        LOG.info("OlmPowerServiceRpcImpl Closed");
    }
}
