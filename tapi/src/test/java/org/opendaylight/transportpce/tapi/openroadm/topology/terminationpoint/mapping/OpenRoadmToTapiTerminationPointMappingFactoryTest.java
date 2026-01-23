/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("OpenROADM → TAPI termination point mapping")
class OpenRoadmToTapiTerminationPointMappingFactoryTest extends AbstractTest {

    @Mock
    private TapiLink tapiLink;

    private TopologyUtils topologyUtils;

    @BeforeAll
    void setUpOnce() throws InterruptedException, ExecutionException {
        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(getDataBroker());

        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/connectivity-utils/openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        topologyUtils = new TopologyUtils(networkTransactionService, getDataBroker(), tapiLink);
    }

    @Test
    @DisplayName("Maps an SRG termination point to multiple TAPI owned node edge points")
    void createUsingSRG() {
        TerminationPointMappingFactory factory = new OpenRoadmToTapiTerminationPointMappingFactory(
                new TopologyTerminationPointTypeResolver(),
                new DefaultTapiPhotonicSublayerMapper()
        );

        String supportingNode = "ROADM-C1";
        String nodeId = "ROADM-C1-SRG1";
        String tpId = "SRG1-PP1-TXRX";

        TerminationPointMapping expected = new TerminationPointMapping(
                new TerminationPointId(supportingNode, nodeId, tpId, OpenroadmTpType.SRGTXRXPP),
                new LinkedHashSet<>(Set.of(
                        onepName(
                                "OTSi_MEDIA_CHANNELNodeEdgePoint",
                                "ROADM-C1+OTSi_MEDIA_CHANNEL+SRG1-PP1-TXRX",
                                NepPhotonicSublayer.OTSI_MC),
                        onepName(
                                "MEDIA_CHANNELNodeEdgePoint",
                                "ROADM-C1+MEDIA_CHANNEL+SRG1-PP1-TXRX",
                                NepPhotonicSublayer.MC),
                        onepName(
                                "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                                "ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX",
                                NepPhotonicSublayer.PHTNC_MEDIA_OTS)
                ))
        );

        TerminationPointMapping terminationPointMapping = factory.create(nodeId, tpId, readOpenRoadmTopology());

        assertEquals(expected, terminationPointMapping);
    }

    @Test
    @DisplayName("Maps a DEGREE termination point to multiple TAPI owned node edge points")
    void createUsingDegree() {
        TerminationPointMappingFactory factory = new OpenRoadmToTapiTerminationPointMappingFactory(
                new TopologyTerminationPointTypeResolver(),
                new DefaultTapiPhotonicSublayerMapper()
        );

        String supportingNode = "ROADM-C1";
        String nodeId = "ROADM-C1-DEG1";
        String tpId = "DEG1-TTP-TXRX";

        TerminationPointMapping expected = new TerminationPointMapping(
                new TerminationPointId(supportingNode, nodeId, tpId, OpenroadmTpType.DEGREETXRXTTP),
                new LinkedHashSet<>(Set.of(
                        onepName(
                                "PHOTONIC_MEDIA_OTSNodeEdgePoint",
                                "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX",
                                NepPhotonicSublayer.PHTNC_MEDIA_OTS),
                        onepName(
                                "PHOTONIC_MEDIA_OMSNodeEdgePoint",
                                "ROADM-C1+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX",
                                NepPhotonicSublayer.PHTNC_MEDIA_OMS)

                ))
        );

        TerminationPointMapping terminationPointMapping = factory.create(nodeId, tpId, readOpenRoadmTopology());

        assertEquals(expected, terminationPointMapping);
    }

    @Test
    public void testMe() {
        Set<NepPhotonicSublayer> srgMapper = Set.of(
                NepPhotonicSublayer.PHTNC_MEDIA_OTS);

        Set<NepPhotonicSublayer> degreeMapper = Set.of(
                NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                NepPhotonicSublayer.PHTNC_MEDIA_OMS);

        Map<OpenroadmTpType, Set<NepPhotonicSublayer>> mapper = Map.of(
                OpenroadmTpType.SRGTXRXPP, srgMapper,
                OpenroadmTpType.SRGRXPP, srgMapper,
                OpenroadmTpType.SRGTXPP, srgMapper,
                OpenroadmTpType.DEGREETXRXTTP, degreeMapper,
                OpenroadmTpType.DEGREERXTTP, degreeMapper,
                OpenroadmTpType.DEGREETXTTP, degreeMapper
        );

        TerminationPointMappingFactory factory = new OpenRoadmToTapiTerminationPointMappingFactory(
                new TopologyTerminationPointTypeResolver(),
                new DefaultTapiPhotonicSublayerMapper(mapper)
        );

        Set<TerminationPointMapping> terminationPointMappings = factory.create(
                topologyChangesMap(),
                readOpenRoadmTopology()
        );
        assertEquals(expectedTerminationPointMappings(), terminationPointMappings);
    }

    private @NonNull OwnedNodeEdgePointName onepName(
            String name,
            String value,
            NepPhotonicSublayer nepPhotonicSublayer) {

        return new OwnedNodeEdgePointName(name(name, value), nepPhotonicSublayer);
    }

    private Name name(String name, String value) {
        return new NameBuilder().setValueName(name).setValue(value).build();
    }

    private Network readOpenRoadmTopology() {
        Network openroadmTopo;
        try {
            openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);
        } catch (TapiTopologyException e) {
            throw new IllegalStateException("Failed to read OpenROADM topology", e);
        }

        if (openroadmTopo == null) {
            throw new IllegalStateException("OpenROADM topology could not be retrieved from datastore");
        }

        return openroadmTopo;
    }

    private Map<TopologyChangesKey, TopologyChanges> topologyChangesMap() {
        Map<String, Set<String>> idPair = Map.of(
                "ROADM-A1-SRG1", Set.of("SRG1-PP1-TXRX", "SRG1-CP-TXRX"),
                "ROADM-A1-DEG2", Set.of("DEG2-CTP-TXRX", "DEG2-TTP-TXRX"),
                "ROADM-C1-SRG1", Set.of("SRG1-PP1-TXRX", "SRG1-CP-TXRX"),
                "ROADM-C1-DEG1", Set.of("DEG1-CTP-TXRX", "DEG1-TTP-TXRX")
        );

        Map<TopologyChangesKey, TopologyChanges> topologyChangesMap = new HashMap<>();
        idPair.forEach((key, value) -> value.forEach(tpId -> {
            TopologyChanges topologyChanges = new TopologyChangesBuilder()
                    .setNodeId(key)
                    .setTpId(tpId)
                    .build();
            topologyChangesMap.put(topologyChanges.key(), topologyChanges);
        }));

        return topologyChangesMap;
    }

    private static Name nme(String value, String valueName) {
        return new NameBuilder()
                .setValue(value)
                .setValueName(valueName)
                .build();
    }

    private static OwnedNodeEdgePointName onep(String value, String valueName, NepPhotonicSublayer sublayer) {
        return new OwnedNodeEdgePointName(nme(value, valueName), sublayer);
    }

    public static Set<TerminationPointMapping> expectedTerminationPointMappings() {
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

}
