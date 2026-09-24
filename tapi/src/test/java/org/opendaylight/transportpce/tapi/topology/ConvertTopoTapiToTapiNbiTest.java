/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yangtools.binding.util.BindingMap;

public class ConvertTopoTapiToTapiNbiTest {

    private static final String NODE_ID = "ROADM-A1";

    @Test
    void convertRoadmInfrastructureKeepsAddDropOtsAndOtsiMcNeps() {
        OwnedNodeEdgePoint srgOts = nep(TapiConstants.PHTNC_MEDIA_OTS, "SRG1-PP1-TXRX");
        OwnedNodeEdgePoint srgOtsiMc = nep(TapiConstants.OTSI_MC, "SRG1-PP1-TXRX");
        Name roadmName = new NameBuilder().setValueName(TapiConstants.VALUE_NAME_ROADM_NODE).setValue(NODE_ID).build();
        Node roadm = new NodeBuilder()
            .setUuid(uuid(NODE_ID))
            .setName(Map.of(roadmName.key(), roadmName))
            .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA))
            .setOwnedNodeEdgePoint(BindingMap.of(
                srgOts,
                srgOtsiMc,
                nep(TapiConstants.PHTNC_MEDIA_OTS, "DEG1-TTP-TXRX"),
                nep(TapiConstants.OTSI_MC, "DEG1-TTP-TXRX"),
                nep(TapiConstants.MC, "DEG1-TTP-TXRX")))
            .build();

        ConvertTopoTapiToTapiNbi converter = new ConvertTopoTapiToTapiNbi(TapiConstants.T0_FULL_MULTILAYER_UUID);
        converter.setTapiNodes(Map.of(roadm.key(), roadm));
        converter.convertRoadmInfrastructure();

        Node rdmInfra = converter.getTapiNodes().values().stream().findFirst().orElseThrow();
        assertEquals(1, converter.getTapiNodes().size());
        assertEquals(uuid(TapiConstants.RDM_INFRA), rdmInfra.getUuid());
        assertEquals(Set.of(srgOts.key(), srgOtsiMc.key()), rdmInfra.nonnullOwnedNodeEdgePoint().keySet());
    }

    private static OwnedNodeEdgePoint nep(String sublayer, String tpId) {
        String value = String.join("+", NODE_ID, sublayer, tpId);
        Name name = new NameBuilder().setValueName(sublayer + "NodeEdgePoint").setValue(value).build();
        return new OwnedNodeEdgePointBuilder()
            .setUuid(uuid(value))
            .setName(Map.of(name.key(), name))
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
            .build();
    }

    private static Uuid uuid(String value) {
        return new Uuid(UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
