/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.topology;


import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.tapi.frequency.Math;
import org.opendaylight.transportpce.tapi.frequency.NumericFrequency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;

class ConvertORToTapiTopologyTest {

    private ORToTapiTopoConversionFactory convertORToTapiTopology;

    private TerminationPoint1 terminationPoint1;

    private TerminationPoint terminationPoint;

    @BeforeEach
    void setUp() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        Mockito.when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        convertORToTapiTopology = new ORToTapiTopoConversionFactory(
                Uuid.getDefaultInstance("123"),
                new NumericFrequency(
                        191.325,
                        768,
                        math
                )
        );

        byte[] availableFrequencyMap = new byte[96];
        Arrays.fill(availableFrequencyMap, (byte) 255);
        availableFrequencyMap[96 - 1] = (byte) 0;

        AvailFreqMaps freqMap = new AvailFreqMapsBuilder()
                .setMapName(new AvailFreqMapsKey("cband").getMapName())
                .setFreqMap(availableFrequencyMap)
                .build();

        terminationPoint1 = new TerminationPoint1Builder()
                .setTxTtpAttributes(
                        new TxTtpAttributesBuilder()
                                .setAvailFreqMaps(Map.of(freqMap.key(), freqMap))
                                .build()
                ).build();

        terminationPoint = new TerminationPointBuilder()
                .setTpId(TpId.getDefaultInstance("1"))
                .addAugmentation(terminationPoint1)
                .build();
    }

    @Test
    void getTTPUsedFreqMap() {
        Map<Double, Double> expected = Map.of(196.075, 196.125);
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTPUsedFreqMap(terminationPoint));
    }

    @Test
    void getTTPAvailableFreqMap() {
        Map<Double, Double> expected = Map.of(191.325, 196.075);
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTPAvailableFreqMap(terminationPoint));
    }

    @Test
    void getTTP11UsedFreqMap() {
        Map<Double, Double> expected = Map.of(196.075, 196.125);
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTP11UsedFreqMap(terminationPoint1));
    }

    @Test
    void getTTP11AvailableFreqMap() {
        Map<Double, Double> expected = Map.of(191.325, 196.075);
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTP11AvailableFreqMap(terminationPoint1));
    }

}
