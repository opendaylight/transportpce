/*
 * Copyright (c) 2023 Smartoptics, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;

class PreferenceFactoryTest {

    @Test
    void emptyPathComputationRequest_returnEmptyHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Map<String, Set<String>> expected = new HashMap<>();
        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionWithoutDeviceAndPort_returnEmptyHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = Mockito.mock(ServiceAEnd.class);
        RxDirection rxDirection = Mockito.mock(RxDirection.class);

        Port port = new PortBuilder()
            .build();

        Mockito.when(rxDirection.getPort()).thenReturn(port);
        Mockito.when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        Mockito.when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Map<String, Set<String>> expected = new HashMap<>();
        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionWithoutPort_returnEmptyHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = Mockito.mock(ServiceAEnd.class);
        RxDirection rxDirection = Mockito.mock(RxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .build();

        Mockito.when(rxDirection.getPort()).thenReturn(port);
        Mockito.when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        Mockito.when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Map<String, Set<String>> expected = new HashMap<>();
        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionTxRx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = Mockito.mock(ServiceAEnd.class);
        RxDirection rxDirection = Mockito.mock(RxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-TXRX")
            .build();

        Mockito.when(rxDirection.getPort()).thenReturn(port);
        Mockito.when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        Mockito.when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionTx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = Mockito.mock(ServiceAEnd.class);
        RxDirection rxDirection = Mockito.mock(RxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-TX")
            .build();

        Mockito.when(rxDirection.getPort()).thenReturn(port);
        Mockito.when(serviceAEnd.getRxDirection()).thenReturn(rxDirection);
        Mockito.when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndRxDirectionRx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = Mockito.mock(ServiceAEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-RX")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceAEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-RX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceZEndRx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        RxDirection rxDirection = Mockito.mock(RxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-TXRX")
            .build();

        Mockito.when(rxDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getRxDirection()).thenReturn(rxDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceZEndTxDirectionTxRx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-TXRX")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceZEndTxDirectionTx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-TX")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceZEndTxDirectionRx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-RX")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-RX"));
        PreferenceFactory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathEmptyComputationRequestServiceZEndTx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName(" ")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathUnexpectedPortName_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("FUBAR")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("FUBAR"));

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void addingMultiplePort() {

        PreferenceFactory portPreferenceFactory = new PreferenceFactory();
        Map<String, Set<String>> mapper = new HashMap<>();

        //New ports
        Assertions.assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP1-TXRX", mapper));
        Assertions.assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP2-TXRX", mapper));
        Assertions.assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP3-RX", mapper));
        Assertions.assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP3-TX", mapper));

        //This port already exists, should return false.
        Assertions.assertFalse(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP2-TXRX", mapper));

        Set<String> expected = Set.of("SRG1-PP1-TXRX", "SRG1-PP2-TXRX", "SRG1-PP3-RX", "SRG1-PP3-TX");
        Assertions.assertEquals(expected, mapper.get("ROADM-B-SRG1"));
    }
}