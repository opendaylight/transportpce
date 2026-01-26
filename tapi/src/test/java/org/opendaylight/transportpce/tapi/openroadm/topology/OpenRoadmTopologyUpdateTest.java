/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.OpenRoadmTopologyUpdate;
import org.opendaylight.transportpce.tapi.openroadm.topology.changes.TapiTopologyChangesExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.MdSalOpenRoadmTopologyRepository;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.MdSalOwnedNodeEdgePointRepository;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.NepIdentifier;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TapiIdentifierFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.DefaultTapiPhotonicSublayerMapper;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OpenRoadmToTapiTerminationPointMappingFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointName;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMapping;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TopologyTerminationPointTypeResolver;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.TapiSpectrumGridConfig;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;

class OpenRoadmTopologyUpdateTest extends AbstractTest {

    @Test
    @DisplayName("copyToTAPI applies spectrum capability updates when OpenROADM topology is present")
    void copyToTAPI() throws ExecutionException, InterruptedException {

        DataStoreContextImpl dataStoreContextUtil = new DataStoreContextImpl();

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                dataStoreContextUtil,
                TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE,
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        NetworkTransactionImpl networkTransactionService =
                new NetworkTransactionImpl(dataStoreContextUtil.getDataBroker());

        TopologyDataUtils.writeTapiTopologyFromFileToDatastore(
                dataStoreContextUtil,
                TapiTopologyDataUtils.TAPI_SBI_TOPOLOGY_FILE,
                TapiConstants.TAPI_TOPOLOGY_T0_FULL_IID);

        OpenRoadmToTapiTerminationPointMappingFactory mappingFactory =
                new OpenRoadmToTapiTerminationPointMappingFactory(
                        new TopologyTerminationPointTypeResolver(),
                        new DefaultTapiPhotonicSublayerMapper()
        );

        MdSalOpenRoadmTopologyRepository topologyRepository = new MdSalOpenRoadmTopologyRepository(
                networkTransactionService);

        TapiIdentifierFactory identifierFactory = new TapiIdentifierFactory();

        MdSalOwnedNodeEdgePointRepository nepRepository = new MdSalOwnedNodeEdgePointRepository(
                networkTransactionService);

        OpenRoadmTopologyUpdate topologyUpdater =
                new OpenRoadmTopologyUpdate(
                        nepRepository,
                        mappingFactory,
                        topologyRepository,
                        identifierFactory,
                        TapiTopologyChangesExtractor.create(
                                networkTransactionService,
                                new TapiSpectrumGridConfig(
                                        191.325,
                                        6.25,
                                        768)
                        )
                );

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

        TopologyUpdateResultBuilder resultBuilder = new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChangesMap);

        // Assert copy went well
        TopologyUpdateResult result = resultBuilder.build();
        assertTrue(topologyUpdater.copyToTAPI(result));

        // Read network
        Network topo = topologyRepository.read(
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifiers.OPENROADM_TOPOLOGY_II)
                .orElseThrow();

        Set<TerminationPointMapping> mappings = mappingFactory.create(result, topo);

        assertFalse(mappings.isEmpty());

        // Assert data from datastore
        for (TerminationPointMapping mapping : mappings) {

            for (OwnedNodeEdgePointName nepName : mapping.nodeEdgePointNames()) {

                NepIdentifier nepId = identifierFactory.nepIdentifier(
                        TapiConstants.T0_FULL_MULTILAYER,
                        mapping.terminationPointId(),
                        nepName);

                OwnedNodeEdgePoint nep = nepRepository.read(nepId).orElseThrow(
                        () -> new AssertionError("Missing NEP: " + nepId.toLogString()));

                OwnedNodeEdgePoint1 aug = nep.augmentation(OwnedNodeEdgePoint1.class);

                assertNotNull(aug, "Missing augmentation for " + nepId.toLogString());

                assertNotNull(
                        aug.getPhotonicMediaNodeEdgePointSpec(),
                        "Missing spec for " + nepId.toLogString());

                assertNotNull(
                        aug.getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac(),
                        "Missing spectrum capability for " + nepId.toLogString());
            }
        }
    }

    @Test
    @DisplayName("copyToTAPI throws when OpenROADM topology is missing and there are changes to apply")
    void copyToTAPI_missingOpenRoadmTopology_throws() {

        DataStoreContextImpl dataStoreContextUtil = new DataStoreContextImpl();

        // Intentionally NOT writing OPENROADM_TOPOLOGY_II
        NetworkTransactionImpl networkTransactionService =
                new NetworkTransactionImpl(dataStoreContextUtil.getDataBroker());

        OpenRoadmTopologyUpdate copyFromOpenRoadm =
                new OpenRoadmTopologyUpdate(
                        new MdSalOwnedNodeEdgePointRepository(networkTransactionService),
                        new OpenRoadmToTapiTerminationPointMappingFactory(
                                new TopologyTerminationPointTypeResolver(),
                                new DefaultTapiPhotonicSublayerMapper()
                        ),
                        new MdSalOpenRoadmTopologyRepository(networkTransactionService),
                        new TapiIdentifierFactory(),
                        TapiTopologyChangesExtractor.create(
                                networkTransactionService,
                                new TapiSpectrumGridConfig(
                                        191.325,
                                        6.25,
                                        768)
                        )
                );

        // Provide at least one change so copyToTAPI() is not a no-op
        TopologyChanges topologyChanges = new TopologyChangesBuilder()
                .setNodeId("ROADM-A1-SRG1")
                .setTpId("SRG1-PP1-TXRX")
                .build();

        Map<TopologyChangesKey, TopologyChanges> topologyChangesMap = Map.of(topologyChanges.key(), topologyChanges);

        TopologyUpdateResultBuilder topologyUpdateResultBuilder = new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChangesMap);

        assertThrows(IllegalStateException.class,
                () -> copyFromOpenRoadm.copyToTAPI(topologyUpdateResultBuilder.build()));
    }
}
