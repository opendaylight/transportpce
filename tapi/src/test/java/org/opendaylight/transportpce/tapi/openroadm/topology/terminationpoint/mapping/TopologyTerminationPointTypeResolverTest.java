/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TopologyTerminationPointTypeResolverTest extends AbstractTest {

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
    @DisplayName("Returns OpenROADM TP type for an existing node and termination point")
    void testOpenRoadmTypeReturned() {
        OpenRoadmTpTypeResolver terminationPointType =
                new TopologyTerminationPointTypeResolver();

        assertEquals(
                new TerminationPointId(
                        "ROADM-C1",
                        "ROADM-C1-SRG1",
                        "SRG1-PP1-TXRX",
                        OpenroadmTpType.SRGTXRXPP
                ),
                terminationPointType.terminationPointId("ROADM-C1-SRG1", "SRG1-PP1-TXRX", readOpenRoadmTopology()));
    }

    @Test
    @DisplayName("Throws NODE_NOT_FOUND when the OpenROADM node does not exist")
    void testNodeNotFoundThrowsException() {
        OpenRoadmTpTypeResolver terminationPointType =
                new TopologyTerminationPointTypeResolver();

        assertThatThrownBy(() -> terminationPointType.terminationPointId(
                "MISSING-ROADM-NODE",
                "SRG1-PP1-TXRX",
                readOpenRoadmTopology()))
                .isInstanceOf(TpTypeResolutionException.class)
                .satisfies(ex -> {
                    TpTypeResolutionException exception = (TpTypeResolutionException) ex;
                    assertThat(exception.reason()).isEqualTo(TpTypeResolutionException.Reason.NODE_NOT_FOUND);
                    assertThat(exception.nodeId()).isEqualTo("MISSING-ROADM-NODE");
                    assertThat(exception.tpId()).isEqualTo("SRG1-PP1-TXRX");
                });
    }

    @Test
    @DisplayName("Throws TP_NOT_FOUND when the termination point does not exist on the node")
    void testTerminationPointNotFoundThrowsException() {
        OpenRoadmTpTypeResolver terminationPointType =
                new TopologyTerminationPointTypeResolver();

        assertThatThrownBy(() -> terminationPointType.terminationPointId(
                "ROADM-C1-SRG1",
                "MISSING-TP",
                readOpenRoadmTopology()))
                .isInstanceOf(TpTypeResolutionException.class)
                .satisfies(ex -> {
                    TpTypeResolutionException exception = (TpTypeResolutionException) ex;
                    assertThat(exception.reason()).isEqualTo(TpTypeResolutionException.Reason.TP_NOT_FOUND);
                    assertThat(exception.nodeId()).isEqualTo("ROADM-C1-SRG1");
                    assertThat(exception.tpId()).isEqualTo("MISSING-TP");
                });
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
}
