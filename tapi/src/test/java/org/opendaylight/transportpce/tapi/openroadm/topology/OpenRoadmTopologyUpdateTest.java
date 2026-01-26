/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.OpenRoadmTopologyUpdate;
import org.opendaylight.transportpce.tapi.openroadm.topology.changes.TapiTopologyChangesExtractor;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.MdSalOpenRoadmTopologyRepository;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.MdSalOwnedNodeEdgePointRepository;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TapiIdentifierFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.DefaultTapiPhotonicSublayerMapper;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OpenRoadmToTapiTerminationPointMappingFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TopologyTerminationPointTypeResolver;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum.TapiSpectrumGridConfig;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;

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

        Map<String, Set<String>> idPair = Map.of(
                "ROADM-A1-SRG1", Set.of("SRG1-PP1-TXRX", "SRG1-CP-TXRX"),
                "ROADM-A1-DEG2", Set.of("DEG2-CTP-TXRX", "DEG2-TTP-TXRX"),
                "ROADM-C1-SRG1", Set.of("SRG1-PP1-TXRX", "SRG1-CP-TXRX"),
                "ROADM-C1-DEG1", Set.of("DEG1-CTP-TXRX", "DEG1-TTP-TXRX")
        );

        Map<TopologyChangesKey, TopologyChanges> topologyChangesMap = new HashMap<>();
        idPair.entrySet().forEach(entry -> {
            entry.getValue().forEach(tpId -> {
                TopologyChanges topologyChanges = new TopologyChangesBuilder()
                        .setNodeId(entry.getKey())
                        .setTpId(tpId)
                        .build();
                topologyChangesMap.put(topologyChanges.key(), topologyChanges);
            });
        });

        TopologyUpdateResultBuilder topologyUpdateResultBuilder = new TopologyUpdateResultBuilder()
                .setTopologyChanges(topologyChangesMap);

        assertTrue(copyFromOpenRoadm.copyToTAPI(topologyUpdateResultBuilder.build()));
    }

    @Test
    @DisplayName("copyToTAPI throws when OpenROADM topology is missing and there are changes to apply")
    void copyToTAPI_missingOpenRoadmTopology_throws() throws ExecutionException, InterruptedException {

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
