/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;

class OpenRoadmTopologyTest {

    @Test
    void copyTopologyToTAPI() throws ExecutionException, InterruptedException {

        DataStoreContextImpl dataStoreContextUtil = new DataStoreContextImpl();

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                dataStoreContextUtil,
                "src/test/resources/jt-openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        OpenRoadmTopology copyFromOpenRoadm =
            new OpenRoadmTopology(new NetworkTransactionImpl(dataStoreContextUtil.getDataBroker()));

        Set<String> srgTp = Set.of("SRG1-PP1-TXRX", "SRG1-CP-TXRX");
        Set<String> degTp = Set.of("DEG1-CTP-TXRX", "DEG1-TTP-TXRX");
        Map<String, Set<String>> idPair = Map.of(
                "ROADM-A-SRG1", srgTp,
                "ROADM-A-DEG1", degTp,
                "ROADM-B-SRG1", srgTp,
                "ROADM-B-DEG1", degTp
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
        copyFromOpenRoadm.copyTopologyToTAPI(topologyUpdateResultBuilder.build());
    }

}
