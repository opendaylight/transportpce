/*
 * Copyright (c) 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;

class PreferenceFactoryTest {

    @Test
    void emptyPathComputationRequest_returnEmptyHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        Map<String, Set<String>> expected = new HashMap<>();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionWithoutDeviceAndPort_returnEmptyHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = mock(ServiceAEnd.class);
        RxDirection rxDirection = mock(RxDirection.class);

        when(rxDirection.getPort()).thenReturn(new PortBuilder().build());
        when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Map<String, Set<String>> expected = new HashMap<>();
        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionWithoutPort_returnEmptyHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = mock(ServiceAEnd.class);
        RxDirection rxDirection = mock(RxDirection.class);

        when(rxDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .build());
        when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Map<String, Set<String>> expected = new HashMap<>();
        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionTxRx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = mock(ServiceAEnd.class);
        RxDirection rxDirection = mock(RxDirection.class);

        when(rxDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-TXRX")
                    .build());
        when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionTx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = mock(ServiceAEnd.class);
        RxDirection rxDirection = mock(RxDirection.class);

        when(rxDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-TX")
                    .build());
        when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionRx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = mock(ServiceAEnd.class);
        TxDirection txDirection = mock(TxDirection.class);

        when(txDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-RX")
                    .build());
        when(serviceAEnd.getTxDirection()).thenReturn(txDirection);
        when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-RX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceZEndRx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = mock(ServiceZEnd.class);
        RxDirection rxDirection = mock(RxDirection.class);

        when(rxDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-TXRX")
                    .build());
        when(serviceZEnd.getRxDirection()).thenReturn(rxDirection);
        when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceZEndTxDirectionTxRx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = mock(ServiceZEnd.class);
        TxDirection txDirection = mock(TxDirection.class);

        when(txDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-TXRX")
                    .build());
        when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceZEndTxDirectionTx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = mock(ServiceZEnd.class);
        TxDirection txDirection = mock(TxDirection.class);

        when(txDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-TX")
                    .build());
        when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathComputationRequestServiceZEndTxDirectionRx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = mock(ServiceZEnd.class);
        TxDirection txDirection = mock(TxDirection.class);

        when(txDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("SRG1-PP1-RX")
                    .build());
        when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-RX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathEmptyComputationRequestServiceZEndTx_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = mock(ServiceZEnd.class);
        TxDirection txDirection = mock(TxDirection.class);

        when(txDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName(" ")
                    .build());
        when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void pathUnexpectedPortName_returnHashmap() {
        PathComputationRequestInput pathComputationRequestInput = mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = mock(ServiceZEnd.class);
        TxDirection txDirection = mock(TxDirection.class);

        when(txDirection.getPort())
            .thenReturn(new PortBuilder()
                    .setPortDeviceName("ROADM-B-SRG1")
                    .setPortName("FUBAR")
                    .build());
        when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("FUBAR"));

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));
    }

    @Test
    void addingMultiplePort() {
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        Map<String, Set<String>> mapper = new HashMap<>();

        //New ports
        assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP1-TXRX", mapper));
        assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP2-TXRX", mapper));
        assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP3-RX", mapper));
        assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP3-TX", mapper));

        //This port already exists, should return false.
        assertFalse(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP2-TXRX", mapper));

        assertEquals(
            Set.of("SRG1-PP1-TXRX", "SRG1-PP2-TXRX", "SRG1-PP3-RX", "SRG1-PP3-TX"),
            mapper.get("ROADM-B-SRG1")
        );
    }
}