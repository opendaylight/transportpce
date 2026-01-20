/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev250110.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev250110.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.networks.network.node.termination.point.pp.attributes.UsedWavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;

class DefaultOpenRoadmSpectrumRangeExtractorTest {

    private final OpenRoadmSpectrumRangeExtractor extractor =
            DefaultOpenRoadmSpectrumRangeExtractor.defaultInstance();

    private TerminationPoint1 degreeTp1;

    @BeforeEach
    void setUp() {
        AvailFreqMaps freqMap = availCband(allOnesLastByteZero());

        degreeTp1 = new TerminationPoint1Builder()
                .setTxTtpAttributes(new TxTtpAttributesBuilder()
                        .setAvailFreqMaps(Map.of(freqMap.key(), freqMap))
                        .build())
                .build();
    }

    @Test
    @DisplayName("DEG-TTP: occupied spectrum is decoded from bitmap")
    void extractDegreeTtpOccupied() {
        TerminationPoint tp = terminationPoint(OpenroadmTpType.DEGREETXRXTTP, degreeTp1);

        SpectrumRanges ranges = extractor.extract(tp);

        Map<Frequency, Frequency> expected =
                Map.of(new TeraHertz(196.075), new TeraHertz(196.125));
        assertEquals(expected, ranges.occupied());
    }

    @Test
    @DisplayName("DEG-TTP: available spectrum is decoded from bitmap")
    void extractDegreeTtpAvailable() {
        TerminationPoint tp = terminationPoint(OpenroadmTpType.DEGREETXRXTTP, degreeTp1);

        SpectrumRanges ranges = extractor.extract(tp);

        Map<Frequency, Frequency> expected =
                Map.of(new TeraHertz(191.325), new TeraHertz(196.075));
        assertEquals(expected, ranges.available());
    }

    @Test
    @DisplayName("SRG-PP: available spectrum is decoded from PP bitmap")
    void extractSrgPpAvailableFromBitmap() {
        AvailFreqMaps freqMap = availCband(allOnesLastByteZero());

        TerminationPoint1 srgTp1 = new TerminationPoint1Builder()
                .setPpAttributes(new PpAttributesBuilder()
                        .setAvailFreqMaps(Map.of(freqMap.key(), freqMap))
                        .build())
                .build();

        TerminationPoint tp = terminationPoint(OpenroadmTpType.SRGTXRXPP, srgTp1);

        SpectrumRanges ranges = extractor.extract(tp);

        Map<Frequency, Frequency> expected =
                Map.of(new TeraHertz(191.325), new TeraHertz(196.075));
        assertEquals(expected, ranges.available());
    }

    @Test
    @DisplayName("SRG-PP: without bitmap, ranges are empty even if used-wavelength exists")
    void extractSrgPpWithOnlyUsedWavelengthReturnsEmptyRanges() {
        // No bitmap present => extractor returns empty (because ppAvailable/ppOccupied decode bitmap)
        TerminationPoint1 srgTp1 = new TerminationPoint1Builder()
                .setPpAttributes(new PpAttributesBuilder()
                        .setUsedWavelength(Map.of(usedWavelength(760).key(), usedWavelength(760)))
                        .build())
                .build();

        TerminationPoint tp = terminationPoint(OpenroadmTpType.SRGTXRXPP, srgTp1);

        SpectrumRanges ranges = extractor.extract(tp);

        assertEquals(Map.of(), ranges.available());
        assertEquals(Map.of(), ranges.occupied());
    }

    @Test
    @DisplayName("SRG-PP: occupied spectrum ignores used-wavelength and uses bitmap")
    void extractSrgPpOccupiedUsesBitmapNotUsedWavelength() {
        UsedWavelength wl = usedWavelength(760);

        // Construct a bitmap that produces the expected occupied range.
        // (Keeping your original byte filling behavior, but making it explicit.)
        byte[] map = new byte[96];
        Arrays.fill(map, 0, 92, (byte) 255);
        Arrays.fill(map, 93, 96, (byte) 255);

        AvailFreqMaps availFreqMaps = availCband(map);

        PpAttributes ppAttributes = new PpAttributesBuilder()
                .setUsedWavelength(Map.of(wl.key(), wl))
                .setAvailFreqMaps(Map.of(availFreqMaps.key(), availFreqMaps))
                .build();

        TerminationPoint1 srgTp1 = new TerminationPoint1Builder()
                .setPpAttributes(ppAttributes)
                .build();

        TerminationPoint tp = terminationPoint(OpenroadmTpType.SRGTXRXPP, srgTp1);

        SpectrumRanges ranges = extractor.extract(tp);

        Map<Frequency, Frequency> expected =
                Map.of(new TeraHertz(195.925), new TeraHertz(195.975));
        assertEquals(expected, ranges.occupied());
    }

    private static TerminationPoint terminationPoint(OpenroadmTpType type, TerminationPoint1 topologyTp1) {
        var commonTp1 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110
                .TerminationPoint1Builder()
                .setTpType(type)
                .build();

        return new TerminationPointBuilder()
                .setTpId(TpId.getDefaultInstance("1"))
                .addAugmentation(topologyTp1)
                .addAugmentation(commonTp1)
                .build();
    }

    private static AvailFreqMaps availCband(byte[] bitmap) {
        return new AvailFreqMapsBuilder()
                .setMapName(new AvailFreqMapsKey("cband").getMapName())
                .setFreqMap(bitmap)
                .build();
    }

    private static byte[] allOnesLastByteZero() {
        byte[] bytes = new byte[96];
        Arrays.fill(bytes, (byte) 0xFF);
        bytes[95] = 0;
        return bytes;
    }

    private static UsedWavelength usedWavelength(int index) {
        return new UsedWavelengthBuilder()
                .setIndex(index)
                .setFrequency(FrequencyTHz.getDefaultInstance("196.075"))
                .setWidth(FrequencyGHz.getDefaultInstance("50"))
                .build();
    }
}
