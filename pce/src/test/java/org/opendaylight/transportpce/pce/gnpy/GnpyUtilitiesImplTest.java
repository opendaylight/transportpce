/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumerImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.JsonDataConverter;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GnpyUtilitiesImplTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(GnpyUtilitiesImplTest.class);
    private GnpyUtilitiesImpl gnpyUtilitiesImpl;
    private NetworkTransactionImpl networkTransaction;
    private GnpyConsumer gnpyConsumer;
    private final WireMockServer wireMockServer = new WireMockServer(9998);

    @BeforeEach
    void setUp() throws IOException {
        wireMockServer.start();
        wireMockServer.resetAll();
        configureFor("localhost", 9998);
        stubFor(get(urlEqualTo("/api/v1/status"))
                .willReturn(okJson(Files
                        .readString(Paths
                                .get("src", "test", "resources", "gnpy", "gnpy_status.json")))));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    GnpyUtilitiesImplTest() throws IOException {
        networkTransaction = new NetworkTransactionImpl(getDataBroker());
        try {
            // load openroadm-network
            Reader gnpyNetwork = new FileReader("src/test/resources/gnpy/gnpy_network.json", StandardCharsets.UTF_8);
            Networks networks = (Networks) new JsonDataConverter(null).deserialize(gnpyNetwork, Networks.QNAME);
            saveOpenRoadmNetwork(networks.getNetwork().values().iterator().next(), StringConstants.OPENROADM_NETWORK);
            // load openroadm-topology
            Reader gnpyTopo = new FileReader("src/test/resources/gnpy/gnpy_topology.json", StandardCharsets.UTF_8);
            networks = (Networks) new JsonDataConverter(null).deserialize(gnpyTopo, Networks.QNAME);
            saveOpenRoadmNetwork(networks.getNetwork().values().iterator().next(), StringConstants.OPENROADM_TOPOLOGY);
        } catch (FileNotFoundException | InterruptedException | ExecutionException e) {
            LOG.error("Cannot init test ", e);
            fail("Cannot init test ");
        } catch (IOException e) {
            LOG.warn("Cannot close reader ", e);
        }
    }

    @BeforeEach
    void initConsumer() {
        gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998", "mylogin", "mypassword",
                getDataStoreContextUtil().getBindingDOMCodecServices());
    }


    private void saveOpenRoadmNetwork(Network network, String networkId)
            throws InterruptedException, ExecutionException {
        DataObjectIdentifier<Network> nwInstanceIdentifier = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(networkId)))
                .build();
        networkTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, network);
        networkTransaction.commit().get();
    }

    @Test
    void askNewPathFromGnpyNullResultTest() throws Exception {
        // GIVEN
        stubFor(post(urlEqualTo("/api/v1/path-computation"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Files
                                .readString(Paths
                                        .get("src", "test", "resources", "gnpy", "gnpy_result_no_path.json")))));
       // WHEN
        gnpyUtilitiesImpl = new GnpyUtilitiesImpl(networkTransaction,
                PceTestData.getGnpyPCERequest("XPONDER-1", "XPONDER-2"),
                gnpyConsumer);
        // THEN
        assertNull(gnpyUtilitiesImpl.askNewPathFromGnpy(null), "No hard constraints should be available");
    }

    @Test
    void askNewPathFromGnpyTest() throws Exception {
        stubFor(post(urlEqualTo("/api/v1/path-computation"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Files
                                .readString(Paths
                                        .get("src", "test", "resources", "gnpy", "gnpy_result_with_path.json")))));
        gnpyUtilitiesImpl = new GnpyUtilitiesImpl(networkTransaction,
                PceTestData.getGnpyPCERequest("XPONDER-3", "XPONDER-4"),
                gnpyConsumer);
        PceConstraintsCalc constraints = new PceConstraintsCalc(PceTestData.getPCE_simpletopology_test1_request(),
                networkTransaction);
        PceConstraints pceHardConstraints = constraints.getPceHardConstraints();
        assertNotNull(gnpyUtilitiesImpl.askNewPathFromGnpy(pceHardConstraints), "Hard constraints should be available");
    }

    @Test
    void verifyComputationByGnpyTest() throws Exception {
        stubFor(post(urlEqualTo("/api/v1/path-computation"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Files
                                .readString(Paths
                                        .get("src", "test", "resources", "gnpy", "gnpy_result_no_path.json")))));
        // build AtoZ
        AToZDirectionBuilder atoZDirectionBldr = buildAtZ();
        // build ZtoA
        ZToADirectionBuilder ztoADirectionBldr = buildZtoA();

        gnpyUtilitiesImpl = new GnpyUtilitiesImpl(networkTransaction,
                PceTestData.getGnpyPCERequest("XPONDER-1", "XPONDER-2"),
                gnpyConsumer);
        PceConstraintsCalc constraints = new PceConstraintsCalc(PceTestData.getPCE_simpletopology_test1_request(),
                networkTransaction);
        PceConstraints pceHardConstraints = constraints.getPceHardConstraints();
        boolean result = gnpyUtilitiesImpl.verifyComputationByGnpy(atoZDirectionBldr.build(),
                ztoADirectionBldr.build(),
                pceHardConstraints);
        assertFalse(result, "Gnpy Computation should be false");
    }

    private AToZDirectionBuilder buildAtZ() {
        AToZKey clientKey = new AToZKey("key");
        TerminationPoint stp = new TerminationPointBuilder()
                .setTpId("tpName").setTpNodeId("xname")
                .build();
        Resource clientResource = new ResourceBuilder().setResource(stp).build();
        AToZ firstResource = new AToZBuilder().setId("tpName").withKey(clientKey).setResource(clientResource).build();
        return new AToZDirectionBuilder()
                .setRate(Uint32.valueOf(100))
                .setAToZ(Map.of(firstResource.key(),firstResource))
                .setAToZWavelengthNumber(Uint32.valueOf(0));
    }

    private ZToADirectionBuilder buildZtoA() {
        ZToAKey clientKey = new ZToAKey("key");
        TerminationPoint stp = new TerminationPointBuilder()
                .setTpId("tpName").setTpNodeId("xname")
                .build();
        Resource clientResource = new ResourceBuilder().setResource(stp).build();
        ZToA firstResource = new ZToABuilder().setId("tpName").withKey(clientKey).setResource(clientResource).build();
        return new ZToADirectionBuilder()
                .setRate(Uint32.valueOf(100))
                .setZToA(Map.of(firstResource.key(),firstResource))
                .setZToAWavelengthNumber(Uint32.valueOf(0));
    }
}
