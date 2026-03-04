/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.openroadm.TopologyNodeId;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Integration tests for {@link MdSalOpenRoadmTerminationPointReader}.
 *
 * <p>This test loads an OpenROADM topology from an XML file into the MD-SAL
 * datastore and then verifies that {@link MdSalOpenRoadmTerminationPointReader}
 * can correctly retrieve termination point information from the datastore.
 *
 * <p>The test therefore exercises the integration between:
 * <ul>
 *   <li>Topology XML test data</li>
 *   <li>MD-SAL datastore persistence</li>
 *   <li>{@link NetworkTransactionService} access</li>
 *   <li>{@link MdSalOpenRoadmTerminationPointReader} read logic</li>
 * </ul>
 *
 * <p>Because the datastore is populated from "real" topology data and queried
 * through the MD-SAL APIs, this test behaves as an integration test rather
 * than a pure unit test.
 */
class MdSalOpenRoadmTerminationPointReaderTest extends AbstractTest {

    private OpenRoadmTerminationPointReader openRoadmTerminationPointReader;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(getDataBroker());

        openRoadmTerminationPointReader = new MdSalOpenRoadmTerminationPointReader(networkTransactionService);
    }

    @Test
    @DisplayName("readTerminationPoint returns TerminationPoint when TP exists")
    void shouldReturnTerminationPointWhenTerminationPointExists() {
        assertInstanceOf(
                TerminationPoint.class,
                openRoadmTerminationPointReader.readTerminationPoint(
                                TopologyNodeId.fromNodeAndTpId("ROADM-A1", "SRG1-PP1-TXRX"),
                                "SRG1-PP1-TXRX")
                        .orElseThrow()
        );
    }

    @Test
    @DisplayName("readTerminationPoint returns empty when TP does not exist")
    void shouldReturnEmptyWhenTerminationPointDoesNotExist() {
        assertEquals(
                Optional.empty(),
                openRoadmTerminationPointReader.readTerminationPoint(
                        TopologyNodeId.fromNodeAndTpId("ROADM-A", "SRG1-PP1-TXRX"),
                        "SRG1-PP1-TXRX")
        );
    }

    @Test
    @DisplayName("readCommonTerminationPoint1 returns TerminationPoint1 when augmentation exists")
    void shouldReturnCommonTerminationPoint1WhenAugmentationExists() {
        assertInstanceOf(
                TerminationPoint1.class,
                openRoadmTerminationPointReader.readCommonTerminationPoint1(
                                TopologyNodeId.fromNodeAndTpId("ROADM-A1", "SRG1-PP1-TXRX"),
                                "SRG1-PP1-TXRX")
                        .orElseThrow()
        );
    }

    @Test
    @DisplayName("readCommonTerminationPoint1 returns empty when TP or augmentation does not exist")
    void shouldReturnEmptyWhenCommonTerminationPoint1DoesNotExist() {
        assertEquals(
                Optional.empty(),
                openRoadmTerminationPointReader.readCommonTerminationPoint1(
                        TopologyNodeId.fromNodeAndTpId("ROADM-A", "SRG1-PP1-TXRX"),
                        "SRG1-PP1-TXRX")
        );
    }

    @Test
    @DisplayName("readTopologyTerminationPoint1 returns topology TerminationPoint1 when TP exists")
    void shouldReturnTopologyTerminationPoint1WhenTopologyTerminationPointExists() {
        assertInstanceOf(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1.class,
                openRoadmTerminationPointReader.readTopologyTerminationPoint1(
                                TopologyNodeId.fromNodeAndTpId("SPDR-SA1", "XPDR1-NETWORK1"),
                                "XPDR1-NETWORK1")
                        .orElseThrow()
        );
    }

    @Test
    @DisplayName("readTopologyTerminationPoint1 returns empty when topology TP does not exist")
    void shouldReturnEmptyWhenTopologyTerminationPointDoesNotExist() {
        assertEquals(
                Optional.empty(),
                openRoadmTerminationPointReader.readTopologyTerminationPoint1(
                        TopologyNodeId.fromNodeAndTpId("SPDR-SA", "XPDR1-NETWORK1"),
                        "XPDR1-NETWORK1")
        );
    }
}
