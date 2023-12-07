/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
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
    void testPCRI_returnClientPreference() {
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
        Factory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertInstanceOf(
                ClientPreference.class,
                portPreferenceFactory.portPreference(pathComputationRequestInput)
        );
    }

    @Test
    void emptyPCRI_returnNoPreference() {
        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);

        Factory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertInstanceOf(
                NoPreference.class,
                portPreferenceFactory.portPreference(pathComputationRequestInput)
        );
    }


    @Test
    void emptyPathComputationRequest_returnEmptyHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        Map<String, Set<String>> expected = new HashMap<>();
        Factory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndRx_returnHashmap() {

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
        Factory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceAEndTx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceAEnd serviceAEnd = Mockito.mock(ServiceAEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("SRG1-PP1-TXRX")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceAEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceAEnd()).thenReturn(serviceAEnd);

        Map<String, Set<String>> expected = new HashMap<>();
        expected.put("ROADM-B-SRG1", Set.of("SRG1-PP1-TXRX"));
        Factory portPreferenceFactory = new PreferenceFactory();

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
        Factory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathComputationRequestServiceZEndTx_returnHashmap() {

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
        Factory portPreferenceFactory = new PreferenceFactory();

        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void pathEmptyComputationRequestServiceZEndTx_returnHashmap() {

        PathComputationRequestInput pathComputationRequestInput = Mockito.mock(PathComputationRequestInput.class);
        ServiceZEnd serviceZEnd = Mockito.mock(ServiceZEnd.class);
        TxDirection txDirection = Mockito.mock(TxDirection.class);

        Port port = new PortBuilder()
            .setPortDeviceName("ROADM-B-SRG1")
            .setPortName("N/A")
            .build();

        Mockito.when(txDirection.getPort()).thenReturn(port);
        Mockito.when(serviceZEnd.getTxDirection()).thenReturn(txDirection);
        Mockito.when(pathComputationRequestInput.getServiceZEnd()).thenReturn(serviceZEnd);

        Map<String, Set<String>> expected = new HashMap<>();

        Factory portPreferenceFactory = new PreferenceFactory();
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

        Factory portPreferenceFactory = new PreferenceFactory();
        Assertions.assertEquals(expected, portPreferenceFactory.nodePortMap(pathComputationRequestInput));

    }

    @Test
    void addingMultiplePort() {

        Factory portPreferenceFactory = new PreferenceFactory();
        Map<String, Set<String>> mapper = new HashMap<>();
        Assertions.assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP1-TXRX", mapper));
        Assertions.assertTrue(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP2-TXRX", mapper));
        Assertions.assertFalse(portPreferenceFactory.add("ROADM-B-SRG1", "SRG1-PP2-TXRX", mapper));

    }
}