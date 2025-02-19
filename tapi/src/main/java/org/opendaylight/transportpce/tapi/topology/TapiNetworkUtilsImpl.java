/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.impl.rpc.DeleteTapiLinkImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.InitRoadmRoadmTapiLinkImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.InitXpdrRdmTapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TapiNetworkUtilsImpl {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkUtilsImpl.class);
    private Registration reg;

    @Activate
    public TapiNetworkUtilsImpl(@Reference RpcProviderService rpcProviderService,
            @Reference NetworkTransactionService networkTransactionService, @Reference TapiLink tapiLink) {
        this.reg = rpcProviderService.registerRpcImplementations(
                new InitRoadmRoadmTapiLinkImpl(tapiLink, networkTransactionService),
                new InitXpdrRdmTapiLinkImpl(tapiLink, networkTransactionService),
                new DeleteTapiLinkImpl(networkTransactionService));
        LOG.info("TapiNetworkUtilsImpl instantiated");
    }

    @Deactivate
    public void close() {
        this.reg.close();
        LOG.info("TapiNetworkUtilsImpl Closed");
    }


    public Registration getRegisteredRpc() {
        return reg;
    }

}
