/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.OpenRoadmTopologyUpdate;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmDataStore;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TAPI;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TapiDataStore;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;

class OpenRoadmTopologyUpdateTest {

    @Test
    void copyToTAPI() throws ExecutionException, InterruptedException {

        DataStoreContextImpl dataStoreContextUtil = new DataStoreContextImpl();

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                dataStoreContextUtil,
                TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE,
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        NetworkTransactionImpl networkTransactionService =
                new NetworkTransactionImpl(dataStoreContextUtil.getDataBroker());
        TapiContext tapiContext = new TapiContext(networkTransactionService);

        TopologyDataUtils.writeTapiTopologyFromFileToDatastore(
                dataStoreContextUtil,
                TapiTopologyDataUtils.TAPI_SBI_TOPOLOGY_FILE,
                TapiConstants.TAPI_TOPOLOGY_II);

        TAPI tapiStorageMock = new TapiDataStore(networkTransactionService);

        OpenRoadmTopologyUpdate copyFromOpenRoadm =
            new OpenRoadmTopologyUpdate(
                    tapiContext,
                    tapiStorageMock,
                    new OpenRoadmDataStore(networkTransactionService),
                    networkTransactionService
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

}
