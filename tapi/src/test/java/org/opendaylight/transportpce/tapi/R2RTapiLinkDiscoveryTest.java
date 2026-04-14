/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yangtools.yang.common.Decimal64;

class R2RTapiLinkDiscoveryTest extends AbstractTest {

    private NetworkTransactionService networkTransactionService;
    private TapiLinkImpl tapiLinkImpl;
    private static final Uuid TOPOLOGY_UUID =
            new Uuid(UUID.nameUUIDFromBytes(
                    "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7".getBytes(StandardCharsets.UTF_8)).toString());

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE,
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.OPENROADM_NETWORK_FILE,
                InstanceIdentifiers.OPENROADM_NETWORK_II);

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.OTN_TOPOLOGY_FILE,
                InstanceIdentifiers.OTN_NETWORK_II);

        TopologyDataUtils.writePortmappingFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.PORTMAPPING_FILE);

        DataBroker dataBroker = getDataBroker();
        networkTransactionService = new NetworkTransactionImpl(dataBroker);
        tapiLinkImpl = new TapiLinkImpl(
                networkTransactionService,
                new TapiContext(networkTransactionService));
    }

    @Test
    void createR2RTapiLink_shouldCreateRoadmToRoadmOmsLink() {

        R2RTapiLinkDiscovery r2RTapiLinkDiscovery = new R2RTapiLinkDiscovery(
                networkTransactionService,
                mock(DeviceTransactionManager.class),
                tapiLinkImpl
        );

        Link actual = r2RTapiLinkDiscovery.createR2RTapiLink(
                NodeId.getDefaultInstance("ROADM-C1"),
                "1GE-interface-1",
                "ROADM-A1",
                "1GE-interface-2",
                TOPOLOGY_UUID);

        assertNotNull(actual);

        assertNull(actual.getAdministrativeState());
        assertNull(actual.getOperationalState());
        assertEquals(LifecycleState.INSTALLED, actual.getLifecycleState());
        assertEquals(ForwardingDirection.BIDIRECTIONAL, actual.getDirection());

        assertEquals(
                uuidOf("ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"),
                actual.getUuid().getValue());

        assertEquals(1, actual.getName().size());
        assertEquals(
                "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
                firstNameValue(actual));

        assertEquals(2, actual.getNodeEdgePoint().size());
        assertEquals(
                Map.of(
                        uuidOf("ROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"),
                        uuidOf("ROADM-A1+PHOTONIC_MEDIA"),
                        uuidOf("ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX"),
                        uuidOf("ROADM-C1+PHOTONIC_MEDIA")),
                actual.getNodeEdgePoint().values().stream().collect(
                        java.util.stream.Collectors.toMap(
                                nep -> nep.getNodeEdgePointUuid().getValue(),
                                nep -> nep.getNodeUuid().getValue())));

        assertEquals(1, actual.getLayerProtocolName().size());
        assertEquals(
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName.PHOTONICMEDIA,
                actual.getLayerProtocolName().iterator().next());

        assertEquals(2, actual.getTransitionedLayerProtocolName().size());
        assertEquals(
                java.util.Set.of("PHOTONIC_MEDIA_OTS", "PHOTONIC_MEDIA_OMS"),
                actual.getTransitionedLayerProtocolName());

        assertEquals(
                Decimal64.valueOf("100.0"),
                actual.getAvailableCapacity().getTotalSize().getValue());
        assertEquals(
                CAPACITYUNITGBPS.VALUE,
                actual.getAvailableCapacity().getTotalSize().getUnit());

        assertEquals(
                Decimal64.valueOf("100.0"),
                actual.getTotalPotentialCapacity().getTotalSize().getValue());
        assertEquals(
                CAPACITYUNITGBPS.VALUE,
                actual.getTotalPotentialCapacity().getTotalSize().getUnit());
    }

    private static String firstNameValue(Link link) {
        return link.getName().values().stream()
                .map(Name::getValue)
                .findFirst()
                .orElseThrow();
    }

    private static String uuidOf(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
