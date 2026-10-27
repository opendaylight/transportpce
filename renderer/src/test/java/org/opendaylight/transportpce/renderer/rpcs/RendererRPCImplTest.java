/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.rpcs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.executor.RequestLimitName;
import org.opendaylight.transportpce.common.executor.config.ExecutorPoolConfig;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.concepts.Registration;

@ExtendWith(MockitoExtension.class)
class RendererRPCImplTest {

    @Mock
    private RendererServiceOperations rendererServiceOperations;

    @Mock
    private ExecutorPoolConfig poolConfig;

    @Test
    void testRpcRegistrationUsesTheConfiguredLimits() {
        when(poolConfig.maxRequests(any())).thenReturn(1);
        RpcProviderService rpcProviderService = mock(RpcProviderService.class, RETURNS_MOCKS);

        new RendererRPCImpl(rendererServiceOperations, rpcProviderService, poolConfig);

        // Explicit Rpc<?, ?> matcher types: registerRpcImplementations() is otherwise ambiguous between its
        // Rpc<?,?>... overload and two others, specifically for a two-argument call
        verify(rpcProviderService).registerRpcImplementations(
            (Rpc<?, ?>) any(ServiceImplementationRequestImpl.class), (Rpc<?, ?>) any(ServiceDeleteImpl.class));
        verify(poolConfig).maxRequests(RequestLimitName.SERVICE_IMPLEMENTATION_REQUEST);
        verify(poolConfig).maxRequests(RequestLimitName.SERVICE_DELETE);
    }

    @Test
    void testCloseClosesTheRegistration() {
        when(poolConfig.maxRequests(any())).thenReturn(16);
        Registration registration = mock(Registration.class);
        RpcProviderService rpcProviderService = mock(RpcProviderService.class);
        when(rpcProviderService.registerRpcImplementations((Rpc<?, ?>) any(), (Rpc<?, ?>) any()))
            .thenReturn(registration);

        new RendererRPCImpl(rendererServiceOperations, rpcProviderService, poolConfig).close();

        verify(registration).close();
    }
}
