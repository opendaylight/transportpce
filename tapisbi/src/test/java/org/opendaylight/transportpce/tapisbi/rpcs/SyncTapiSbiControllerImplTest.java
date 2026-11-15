/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiControllerService;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiDiscoveryService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.SyncTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.SyncTapiSbiControllerOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

@ExtendWith(MockitoExtension.class)
class SyncTapiSbiControllerImplTest {

    @Mock
    private TapiSbiControllerService controllerService;
    @Mock
    private TapiSbiDiscoveryService discoveryService;
    @Mock
    private SyncTapiSbiControllerInput input;

    private SyncTapiSbiControllerImpl rpc;

    @BeforeEach
    void setup() {
        rpc = new SyncTapiSbiControllerImpl(controllerService, discoveryService);
    }

    @Test
    void testSyncControllerSuccess() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(controllerService.controllerExists("ctrl-1")).thenReturn(true);

        RpcResult<SyncTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertTrue(result.isSuccessful());
        verify(discoveryService).rediscoverController("ctrl-1");
    }

    @Test
    void testSyncControllerNotFound() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-unknown");
        when(controllerService.controllerExists("ctrl-unknown")).thenReturn(false);

        RpcResult<SyncTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
        verify(discoveryService, never()).rediscoverController(anyString());
    }

    @Test
    void testSyncControllerMissingId() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn(null);

        RpcResult<SyncTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
        verify(discoveryService, never()).rediscoverController(anyString());
    }

    @Test
    void testSyncControllerDiscoveryFails() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(controllerService.controllerExists("ctrl-1")).thenReturn(true);
        doThrow(new IllegalStateException("discovery failed"))
            .when(discoveryService).rediscoverController("ctrl-1");

        RpcResult<SyncTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
    }

    @Test
    void testSyncControllerJsonSyntaxFailureReturnsFailedRpc() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(controllerService.controllerExists("ctrl-1")).thenReturn(true);
        doThrow(new com.google.gson.JsonSyntaxException("invalid JSON"))
            .when(discoveryService).rediscoverController("ctrl-1");

        RpcResult<SyncTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
    }
}
