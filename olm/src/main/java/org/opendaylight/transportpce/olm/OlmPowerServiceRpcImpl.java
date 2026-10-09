/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm;

import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.executor.DefaultExecutorPoolManager;
import org.opendaylight.transportpce.common.executor.DefaultRpcRejections;
import org.opendaylight.transportpce.common.executor.ExecutorPoolManager;
import org.opendaylight.transportpce.common.executor.RpcRejections;
import org.opendaylight.transportpce.common.executor.ThreadPoolName;
import org.opendaylight.transportpce.common.executor.config.ExecutorPoolConfig;
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
    private final ExecutorPoolManager pools;
    private Registration rpcRegistration;

    @Activate
    public OlmPowerServiceRpcImpl(@Reference OlmPowerService olmPowerService,
            @Reference RpcProviderService rpcProviderService,
            @Reference ExecutorPoolConfig poolConfig) {
        this(olmPowerService, rpcProviderService, new DefaultExecutorPoolManager(poolConfig));
    }

    /**
     * Creates the RPC implementations with executors from the given manager.
     *
     * @param pools the manager of the executors; this object takes ownership of it and closes it in {@link #close()}
     */
    public OlmPowerServiceRpcImpl(OlmPowerService olmPowerService, RpcProviderService rpcProviderService,
            ExecutorPoolManager pools) {
        this.pools = pools;
        ListeningExecutorService getPmExecutor =
            pools.executorService(OlmPowerServiceRpcImpl.class, ThreadPoolName.OLM_GET_PM);
        RpcRejections rpcRejections = new DefaultRpcRejections();
        this.rpcRegistration = rpcProviderService.registerRpcImplementations(
            new GetPmImpl(olmPowerService, getPmExecutor, rpcRejections),
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
        // Shuts down the executors, which cancels the requests they are still processing
        this.pools.close();
        LOG.info("OlmPowerServiceRpcImpl Closed");
    }

    public Registration getRegisteredRpc() {
        return rpcRegistration;
    }
}
