/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.changes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.frequency.TeraHertzFactory;
import org.opendaylight.transportpce.tapi.frequency.grid.FrequencyMath;
import org.opendaylight.transportpce.tapi.frequency.grid.NumericFrequency;
import org.opendaylight.transportpce.tapi.frequency.range.FrequencyRangeFactory;
import org.opendaylight.transportpce.tapi.openroadm.TopologyNodeId;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.MdSalOpenRoadmTerminationPointReader;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmTerminationPointReader;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointName;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointSpectrumCapability;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMapping;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.DefaultOpenRoadmSpectrumRangeExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.DefaultTapiSpectrumCapabilityPacFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.OpenRoadmSpectrumRangeExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.TapiSpectrumCapabilityPacFactory;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumKey;
import org.opendaylight.yangtools.yang.common.Uint64;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TapiTopologyChangesExtractorTest extends AbstractTest {

    private OpenRoadmTerminationPointReader openRoadmTerminationPointReader;

    private static final Uint64 THZ_191_325 = Uint64.valueOf("191325000000000");
    private static final Uint64 THZ_196_075 = Uint64.valueOf("196075000000000");
    private static final Uint64 THZ_196_125 = Uint64.valueOf("196125000000000");

    private NetworkTransactionService networkTransactionService;

    @BeforeAll
    void setUpOnce() throws InterruptedException, ExecutionException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        networkTransactionService = new NetworkTransactionImpl(getDataBroker());

        openRoadmTerminationPointReader = new MdSalOpenRoadmTerminationPointReader(networkTransactionService);
    }

    @Test
    @DisplayName("spectrumCapabilityPacs: builds expected Spectrum Capability PACs from OpenROADM topology")
    void spectrumCapabilityPacs_createsOneResultPerNepName_reusesPacPerMapping() {
        TopologyChangesExtractor topologyChangesExtractor = new TapiTopologyChangesExtractor(
                openRoadmTerminationPointReader,
                new DefaultOpenRoadmSpectrumRangeExtractor(
                        new NumericFrequency(
                                GridConstant.START_EDGE_FREQUENCY_THZ,
                                GridConstant.EFFECTIVE_BITS,
                                new FrequencyMath()
                        ),
                        new TeraHertzFactory(),
                        new FrequencyRangeFactory()
                ),
                new DefaultTapiSpectrumCapabilityPacFactory(new TeraHertzFactory())
        );

        Set<OwnedNodeEdgePointSpectrumCapability> ownedNodeEdgePointSpectrumCapabilities =
                topologyChangesExtractor.spectrumCapabilityPacs(terminationPointMappings());

        assertEquals(expectedOwnedNodeEdgePointSpectrumCapability(), ownedNodeEdgePointSpectrumCapabilities);
    }

    @Test
    @DisplayName("spectrumCapabilityPacs: creates one capability per TP and preserves ids/names")
    void spectrumCapabilityPacs_createsOncePerTP() {
        // Arrange
        OpenRoadmTerminationPointReader tpReader = mock(OpenRoadmTerminationPointReader.class);
        OpenRoadmSpectrumRangeExtractor rangeExtractor = mock(OpenRoadmSpectrumRangeExtractor.class);
        TapiSpectrumCapabilityPacFactory pacFactory = mock(TapiSpectrumCapabilityPacFactory.class);

        TapiTopologyChangesExtractor extractor =
                new TapiTopologyChangesExtractor(tpReader, rangeExtractor, pacFactory);

        TerminationPointId tpId = new TerminationPointId(
                "ROADM-A1",
                "ROADM-A1-DEG2",
                "DEG2-TTP-TXRX",
                OpenroadmTpType.DEGREETXRXTTP);

        OwnedNodeEdgePointName nepOms = OwnedNodeEdgePointName.create(
                "ROADM-A1", NepPhotonicSublayer.PHTNC_MEDIA_OMS, "DEG2-TTP-TXRX");
        OwnedNodeEdgePointName nepOts = OwnedNodeEdgePointName.create(
                "ROADM-A1", NepPhotonicSublayer.PHTNC_MEDIA_OTS, "DEG2-TTP-TXRX");

        TerminationPointMapping mapping = new TerminationPointMapping(tpId, Set.of(nepOms, nepOts));

        TerminationPoint tp = mock(TerminationPoint.class);
        when(tpReader.readTerminationPoint(eq(new TopologyNodeId(tpId.nodeId())), eq(tpId.tpId())))
                .thenReturn(Optional.of(tp));

        SpectrumCapabilityPac pac = mock(SpectrumCapabilityPac.class);
        when(pacFactory.create(eq(rangeExtractor), eq(Optional.of(tp)))).thenReturn(pac);

        // Act
        Set<OwnedNodeEdgePointSpectrumCapability> out =
                extractor.spectrumCapabilityPacs(Set.of(mapping));

        // Assert
        Set<OwnedNodeEdgePointSpectrumCapability> expected = Set.of(
                new OwnedNodeEdgePointSpectrumCapability(nepOms, tpId, pac),
                new OwnedNodeEdgePointSpectrumCapability(nepOts, tpId, pac)
        );

        assertEquals(expected, out);

        verify(tpReader, times(1)).readTerminationPoint(eq(new TopologyNodeId(tpId.nodeId())), eq(tpId.tpId()));
        // once per termination point mapping
        verify(pacFactory, times(1)).create(eq(rangeExtractor), eq(Optional.of(tp)));
        verifyNoMoreInteractions(tpReader, pacFactory);
    }

    @Test
    @DisplayName("spectrumCapabilityPacs: passes Optional.empty when termination point is missing")
    void spectrumCapabilityPacs_handlesMissingTerminationPoint() {
        // Arrange
        OpenRoadmTerminationPointReader tpReader = mock(OpenRoadmTerminationPointReader.class);
        OpenRoadmSpectrumRangeExtractor rangeExtractor = mock(OpenRoadmSpectrumRangeExtractor.class);
        TapiSpectrumCapabilityPacFactory pacFactory = mock(TapiSpectrumCapabilityPacFactory.class);

        TapiTopologyChangesExtractor extractor =
                new TapiTopologyChangesExtractor(tpReader, rangeExtractor, pacFactory);

        TerminationPointId tpId = new TerminationPointId(
                "ROADM-C1",
                "ROADM-C1-SRG1",
                "SRG1-PP1-TXRX",
                OpenroadmTpType.SRGTXRXPP);

        OwnedNodeEdgePointName nep = OwnedNodeEdgePointName.create(
                "ROADM-C1", NepPhotonicSublayer.PHTNC_MEDIA_OTS, "SRG1-PP1-TXRX");

        TerminationPointMapping mapping = new TerminationPointMapping(tpId, Set.of(nep));

        when(tpReader.readTerminationPoint(eq(new TopologyNodeId(tpId.nodeId())), eq(tpId.tpId())))
                .thenReturn(Optional.empty());

        SpectrumCapabilityPac pac = mock(SpectrumCapabilityPac.class);
        when(pacFactory.create(eq(rangeExtractor), eq(Optional.empty()))).thenReturn(pac);

        // Act
        Set<OwnedNodeEdgePointSpectrumCapability> out =
                extractor.spectrumCapabilityPacs(Set.of(mapping));

        // Assert
        assertEquals(Set.of(new OwnedNodeEdgePointSpectrumCapability(nep, tpId, pac)), out);
        verify(pacFactory, times(1)).create(eq(rangeExtractor), eq(Optional.empty()));
    }

    @Test
    @DisplayName("spectrumCapabilityPacs: throws NullPointerException on null input")
    void spectrumCapabilityPacs_nullInputThrows() {
        TapiTopologyChangesExtractor extractor = new TapiTopologyChangesExtractor(
                mock(OpenRoadmTerminationPointReader.class),
                mock(OpenRoadmSpectrumRangeExtractor.class),
                mock(TapiSpectrumCapabilityPacFactory.class));

        assertThrows(NullPointerException.class, () -> extractor.spectrumCapabilityPacs(null));
    }

    private Name name(String valueName, String value) {
        return new NameBuilder()
                .setValueName(valueName)
                .setValue(value)
                .build();

    }

    private Set<TerminationPointMapping> terminationPointMappings() {
        return Set.of(
                new TerminationPointMapping(
                        new TerminationPointId("ROADM-C1", "ROADM-C1-SRG1", "SRG1-PP1-TXRX", OpenroadmTpType.SRGTXRXPP),
                        Set.of(
                                onep(
                                        "ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX",
                                        "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                                        NepPhotonicSublayer.PHTNC_MEDIA_OTS
                                )
                        )
                ),
                new TerminationPointMapping(
                        new TerminationPointId("ROADM-A1", "ROADM-A1-DEG2", "DEG2-TTP-TXRX",
                                OpenroadmTpType.DEGREETXRXTTP),
                        Set.of(
                                onep(
                                        "ROADM-A1+PHOTONIC_MEDIA_OMS+DEG2-TTP-TXRX",
                                        "PHOTONIC_MEDIA_OMSNodeEdgePoint",
                                        NepPhotonicSublayer.PHTNC_MEDIA_OMS
                                ),
                                onep(
                                        "ROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
                                        "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                                        NepPhotonicSublayer.PHTNC_MEDIA_OTS
                                )
                        )
                ),
                new TerminationPointMapping(
                        new TerminationPointId("ROADM-A1", "ROADM-A1-SRG1", "SRG1-PP1-TXRX", OpenroadmTpType.SRGTXRXPP),
                        Set.of(
                                onep(
                                        "ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX",
                                        "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                                        NepPhotonicSublayer.PHTNC_MEDIA_OTS
                                )
                        )
                ),
                new TerminationPointMapping(
                        new TerminationPointId("ROADM-C1", "ROADM-C1-DEG1", "DEG1-TTP-TXRX",
                                OpenroadmTpType.DEGREETXRXTTP),
                        Set.of(
                                onep(
                                        "ROADM-C1+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX",
                                        "PHOTONIC_MEDIA_OMSNodeEdgePoint",
                                        NepPhotonicSublayer.PHTNC_MEDIA_OMS
                                ),
                                onep(
                                        "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX",
                                        "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                                        NepPhotonicSublayer.PHTNC_MEDIA_OTS
                                )
                        )
                )
        );
    }

    private OwnedNodeEdgePointName onep(String value, String valueName, NepPhotonicSublayer sublayer) {
        return new OwnedNodeEdgePointName(name(valueName, value), sublayer);
    }

    private Set<OwnedNodeEdgePointSpectrumCapability> expectedOwnedNodeEdgePointSpectrumCapability() {

        // Default spectrum: available=191.325..196.125, supportable=191.325..196.125
        var defaultSpectrumPac = new SpectrumCapabilityPacBuilder()
                .setAvailableSpectrum(Map.of(
                        new AvailableSpectrumKey(THZ_191_325, THZ_196_125),
                        new AvailableSpectrumBuilder()
                                .setLowerFrequency(THZ_191_325)
                                .setUpperFrequency(THZ_196_125)
                                .build()
                ))
                .setSupportableSpectrum(Map.of(
                        new SupportableSpectrumKey(THZ_191_325, THZ_196_125),
                        new SupportableSpectrumBuilder()
                                .setLowerFrequency(THZ_191_325)
                                .setUpperFrequency(THZ_196_125)
                                .build()
                ))
                .build();

        // available=191.325..196.075, occupied=196.075..196.125, supportable=191.325..196.125
        var spectrumPacWithOccupied = new SpectrumCapabilityPacBuilder()
                .setAvailableSpectrum(Map.of(
                        new AvailableSpectrumKey(THZ_191_325, THZ_196_075),
                        new AvailableSpectrumBuilder()
                                .setLowerFrequency(THZ_191_325)
                                .setUpperFrequency(THZ_196_075)
                                .build()
                ))
                .setOccupiedSpectrum(Map.of(
                        new OccupiedSpectrumKey(THZ_196_075, THZ_196_125),
                        new OccupiedSpectrumBuilder()
                                .setLowerFrequency(THZ_196_075)
                                .setUpperFrequency(THZ_196_125)
                                .build()
                ))
                .setSupportableSpectrum(Map.of(
                        new SupportableSpectrumKey(THZ_191_325, THZ_196_125),
                        new SupportableSpectrumBuilder()
                                .setLowerFrequency(THZ_191_325)
                                .setUpperFrequency(THZ_196_125)
                                .build()
                ))
                .build();

        return Set.of(
                new OwnedNodeEdgePointSpectrumCapability(
                        OwnedNodeEdgePointName.create(
                                "ROADM-A1",
                                NepPhotonicSublayer.PHTNC_MEDIA_OMS,
                                "DEG2-TTP-TXRX"
                        ),
                        new TerminationPointId(
                                "ROADM-A1",
                                "ROADM-A1-DEG2",
                                "DEG2-TTP-TXRX",
                                OpenroadmTpType.DEGREETXRXTTP
                        ),
                        defaultSpectrumPac
                ),
                new OwnedNodeEdgePointSpectrumCapability(
                        OwnedNodeEdgePointName.create(
                                "ROADM-A1",
                                NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                                "DEG2-TTP-TXRX"
                        ),
                        new TerminationPointId(
                                "ROADM-A1",
                                "ROADM-A1-DEG2",
                                "DEG2-TTP-TXRX",
                                OpenroadmTpType.DEGREETXRXTTP
                        ),
                        defaultSpectrumPac
                ),
                new OwnedNodeEdgePointSpectrumCapability(
                        OwnedNodeEdgePointName.create(
                                "ROADM-C1",
                                NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                                "SRG1-PP1-TXRX"
                        ),
                        new TerminationPointId(
                                "ROADM-C1",
                                "ROADM-C1-SRG1",
                                "SRG1-PP1-TXRX",
                                OpenroadmTpType.SRGTXRXPP
                        ),
                        spectrumPacWithOccupied
                ),
                new OwnedNodeEdgePointSpectrumCapability(
                        OwnedNodeEdgePointName.create(
                                "ROADM-C1",
                                NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                                "DEG1-TTP-TXRX"
                        ),
                        new TerminationPointId(
                                "ROADM-C1",
                                "ROADM-C1-DEG1",
                                "DEG1-TTP-TXRX",
                                OpenroadmTpType.DEGREETXRXTTP
                        ),
                        defaultSpectrumPac
                ),
                new OwnedNodeEdgePointSpectrumCapability(
                        OwnedNodeEdgePointName.create(
                                "ROADM-C1",
                                NepPhotonicSublayer.PHTNC_MEDIA_OMS,
                                "DEG1-TTP-TXRX"
                        ),
                        new TerminationPointId(
                                "ROADM-C1",
                                "ROADM-C1-DEG1",
                                "DEG1-TTP-TXRX",
                                OpenroadmTpType.DEGREETXRXTTP
                        ),
                        defaultSpectrumPac
                ),
                new OwnedNodeEdgePointSpectrumCapability(
                        OwnedNodeEdgePointName.create(
                                "ROADM-A1",
                                NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                                "SRG1-PP1-TXRX"
                        ),
                        new TerminationPointId(
                                "ROADM-A1",
                                "ROADM-A1-SRG1",
                                "SRG1-PP1-TXRX",
                                OpenroadmTpType.SRGTXRXPP
                        ),
                        defaultSpectrumPac
                )
        );
    }
}
