/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopoContextInit {

    private static final Logger LOG = LoggerFactory.getLogger(TapiTopoContextInit.class);
    private final NetworkTransactionService networkTransactionService;
    private final TapiContext tapiContext;

    public TapiTopoContextInit(TapiContext tapiContext, NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
        this.tapiContext = tapiContext;
    }

    public void initializeTopoContext() {
        String sbiTopoType = TapiStringConstants.SBI_TAPI_TOPOLOGY;
        String alienTopoType = TapiStringConstants.ALIEN_XPDR_TAPI_TOPOLOGY;
        Uuid sbiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(sbiTopoType.getBytes(Charset.forName("UTF-8"))).toString());
        Uuid alTopoUuid = new Uuid(UUID.nameUUIDFromBytes(alienTopoType.getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info("TOPO tapi-utils TapiTopoContextInit, Initializing Topo Context for topology {} UUID {} & {} UUID {}",
            sbiTopoType, sbiTopoUuid, alienTopoType, alTopoUuid);
        Name topoName = new NameBuilder().setValue(sbiTopoType).setValueName("TAPI Topology Name").build();

        Topology sbiTopo = new TopologyBuilder()
            .setName(Map.of(topoName.key(), topoName))
            .setUuid(sbiTopoUuid)
            .setLayerProtocolName(Set.of(
                    LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.ODU,
                    LayerProtocolName.DSR, LayerProtocolName.DIGITALOTN))
            .build();
        Map<TopologyKey, Topology> topoMap = new HashMap<>(Map.of(new TopologyKey(sbiTopo.getUuid()), sbiTopo));

        topoName = new NameBuilder().setValue(alienTopoType).setValueName("TAPI Topology Name").build();
        Topology alienTopo = new TopologyBuilder()
            .setName(Map.of(topoName.key(), topoName))
            .setUuid(alTopoUuid)
            .setLayerProtocolName(Set.of(
                    LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.ODU,
                    LayerProtocolName.DSR, LayerProtocolName.DIGITALOTN))
            .build();
        topoMap.put(new TopologyKey(alienTopo.getUuid()), alienTopo);
        tapiContext.updateTopologyContext(topoMap);
    }
}
