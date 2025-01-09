/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumerImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;

@ExtendWith(MockitoExtension.class)
class PceSendingPceRPCsTest extends AbstractTest {

    private PceSendingPceRPCs pceSendingPceRPCs;
    private NetworkTransactionImpl networkTransaction;
    private Mapping mapping;
    private DataBroker dataBroker;
    private GnpyConsumer gnpyConsumer;
    @Mock
    private PortMapping portMapping;


    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException {
        this.dataBroker = getNewDataBroker();
        networkTransaction = new NetworkTransactionImpl(this.dataBroker);
        gnpyConsumer = new GnpyConsumerImpl(
            "http://localhost:9998", "mylogin", "mypassword", getDataStoreContextUtil().getBindingDOMCodecServices());
        pceSendingPceRPCs = new PceSendingPceRPCs(
            PceTestData.getPCE_test1_request_54(), networkTransaction, gnpyConsumer, portMapping);
        mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual("xpdr-client")
            .build();
        NodeInfo info = new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).build();
        Nodes node = new NodesBuilder().withKey(new NodesKey("node")).setNodeId("node").setNodeInfo(info).build();
        lenient().when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        lenient().when(portMapping.getNode(anyString())).thenReturn(node);
    }

    @Test
    void cancelResourceReserve() {
        pceSendingPceRPCs.cancelResourceReserve();
        assertTrue(pceSendingPceRPCs.getSuccess(), "Success should equal to true");
    }

    @Test
    void pathComputationTest() throws Exception {
        // GIVEN
        WireMockServer wireMockServer = new WireMockServer(9998);
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/status"))
                .willReturn(okJson(Files
                        .readString(Paths
                                .get("src", "test", "resources", "gnpy", "gnpy_status.json")))));
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/path-computation"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Files
                                .readString(Paths
                                        .get("src", "test", "resources", "gnpy", "gnpy_result_with_path.json")))));
        pceSendingPceRPCs = new PceSendingPceRPCs(PceTestData.getGnpyPCERequest("XPONDER-1", "XPONDER-2"),
                networkTransaction, gnpyConsumer, portMapping);
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        // WHEN
        pceSendingPceRPCs.pathComputation();
        // THEN
        assertTrue(gnpyConsumer.isAvailable());
        wireMockServer.stop();
    }

    @Test
    void checkMessage() {
        assertNull(pceSendingPceRPCs.getMessage());
    }


    @Test
    void responseCodeTest() {
        assertNull(pceSendingPceRPCs.getResponseCode());
    }

    @Test
    void gnpyAtoZ() {
        assertNull(pceSendingPceRPCs.getGnpyAtoZ());
    }

    @Test
    void getGnpyZtoA() {
        assertNull(pceSendingPceRPCs.getGnpyZtoA());
    }
}
