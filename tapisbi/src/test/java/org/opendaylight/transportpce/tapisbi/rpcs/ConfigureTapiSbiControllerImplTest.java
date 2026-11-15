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
import static org.mockito.ArgumentMatchers.any;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.ConfigureTapiSbiControllerOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint16;

@ExtendWith(MockitoExtension.class)
class ConfigureTapiSbiControllerImplTest {

    @Mock
    private TapiSbiControllerService controllerService;
    @Mock
    private ConfigureTapiSbiControllerInput input;

    private ConfigureTapiSbiControllerImpl rpc;

    private static final Host HOST = new Host(new IpAddress(new Ipv4Address("192.168.1.1")));
    private static final PortNumber PORT = new PortNumber(Uint16.valueOf(8182));

    @BeforeEach
    void setup() {
        rpc = new ConfigureTapiSbiControllerImpl(controllerService);
    }

    @Test
    void testConfigureControllerSuccess() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(input.getHost()).thenReturn(HOST);
        when(input.getPort()).thenReturn(PORT);
        when(input.getUsername()).thenReturn("admin");
        when(input.getPassword()).thenReturn("secret");
        when(controllerService.controllerExists("ctrl-1")).thenReturn(false);
        when(controllerService.controllerExistsByHostAndPort("192.168.1.1", "8182")).thenReturn(false);
        when(controllerService.saveController(any())).thenReturn(true);

        RpcResult<ConfigureTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertTrue(result.isSuccessful());
        verify(controllerService).saveController(any());
    }

    @Test
    void testConfigureControllerDuplicateId() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(input.getHost()).thenReturn(HOST);
        when(input.getUsername()).thenReturn("admin");
        when(input.getPassword()).thenReturn("secret");
        when(controllerService.controllerExists("ctrl-1")).thenReturn(true);

        RpcResult<ConfigureTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
        verify(controllerService, never()).saveController(any());
    }

    @Test
    void testConfigureControllerDuplicateHostPort() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-2");
        when(input.getHost()).thenReturn(HOST);
        when(input.getPort()).thenReturn(PORT);
        when(input.getUsername()).thenReturn("admin");
        when(input.getPassword()).thenReturn("secret");
        when(controllerService.controllerExists("ctrl-2")).thenReturn(false);
        when(controllerService.controllerExistsByHostAndPort("192.168.1.1", "8182")).thenReturn(true);

        RpcResult<ConfigureTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
        verify(controllerService, never()).saveController(any());
    }

    @Test
    void testConfigureControllerMissingHost() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(input.getHost()).thenReturn(null);

        RpcResult<ConfigureTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
        verify(controllerService, never()).saveController(any());
    }

    @Test
    void testConfigureControllerCommitFails() throws ExecutionException, InterruptedException {
        when(input.getControllerId()).thenReturn("ctrl-1");
        when(input.getHost()).thenReturn(HOST);
        when(input.getPort()).thenReturn(PORT);
        when(input.getUsername()).thenReturn("admin");
        when(input.getPassword()).thenReturn("secret");
        when(controllerService.controllerExists("ctrl-1")).thenReturn(false);
        when(controllerService.controllerExistsByHostAndPort("192.168.1.1", "8182")).thenReturn(false);
        when(controllerService.saveController(any())).thenReturn(false);

        RpcResult<ConfigureTapiSbiControllerOutput> result = rpc.invoke(input).get();

        assertFalse(result.isSuccessful());
    }
}
