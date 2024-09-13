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
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.transportpce.tapi.frequency.TeraHertzFactory;
import org.opendaylight.transportpce.tapi.frequency.grid.Math;
import org.opendaylight.transportpce.tapi.frequency.grid.NumericFrequency;
import org.opendaylight.transportpce.tapi.frequency.range.FrequencyRangeFactory;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.pp.attributes.UsedWavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;

class ConvertORToTapiTopologyTest {

    private ConvertORToTapiTopology convertORToTapiTopology;

    private TerminationPoint1 terminationPoint1;

    private TerminationPoint terminationPoint;

    @BeforeEach
    void setUp() {
        Math math = Mockito.mock(Math.class);
        Mockito.when(math.getStartFrequencyFromIndex(760)).thenReturn(196.075);
        Mockito.when(math.getStopFrequencyFromIndex(767)).thenReturn(196.125);

        convertORToTapiTopology = new ConvertORToTapiTopology(
                Uuid.getDefaultInstance("123"),
                new NumericFrequency(
                        191.325,
                        768,
                        math
                ),
                new FrequencyRangeFactory(),
                new TeraHertzFactory()
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
        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(196.075), new TeraHertz(196.125));
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTPUsedFreqMap(terminationPoint).ranges());
    }

    @Test
    void getTTPAvailableFreqMap() {
        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(191.325), new TeraHertz(196.075));
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTPAvailableFreqMap(terminationPoint).ranges());
    }

    @Test
    void getTTP11UsedFreqMap() {
        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(196.075), new TeraHertz(196.125));
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTP11UsedFreqMap(terminationPoint1).ranges());
    }

    @Test
    void getTTP11AvailableFreqMap() {
        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(191.325), new TeraHertz(196.075));
        Assertions.assertEquals(expected, convertORToTapiTopology.getTTP11AvailableFreqMap(terminationPoint1).ranges());
    }

    @Test
    void getPP11UsedWavelength() {

        UsedWavelengthBuilder usedWavelengthBuilder = new UsedWavelengthBuilder();
        usedWavelengthBuilder.setIndex(760)
                .setFrequency(FrequencyTHz.getDefaultInstance("196.075"))
                .setWidth(FrequencyGHz.getDefaultInstance("50"));

        UsedWavelength usedWavelength = usedWavelengthBuilder.build();

        PpAttributesBuilder ppAttributesBuilder = new PpAttributesBuilder();
        PpAttributes ppAttributes = ppAttributesBuilder
                .setUsedWavelength(Map.of(usedWavelength.key(), usedWavelength))
                .build();

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder();
        TerminationPoint1 tp = terminationPoint1Builder
                .setPpAttributes(ppAttributes)
                .build();

        Map<Frequency, Frequency> expected = Map.of(new TeraHertz(196.05), new TeraHertz(196.1));
        Assertions.assertEquals(expected, convertORToTapiTopology.getPP11UsedWavelength(tp));
        Assertions.assertTrue(expected.equals(convertORToTapiTopology.getPP11UsedWavelength(tp)));

    }
}
