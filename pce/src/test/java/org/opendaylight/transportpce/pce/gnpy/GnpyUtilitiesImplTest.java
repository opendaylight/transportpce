/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.gson.stream.JsonReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumerImpl;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyStub;
import org.opendaylight.transportpce.pce.utils.JsonUtil;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnpyUtilitiesImplTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(GnpyUtilitiesImplTest.class);
    private GnpyUtilitiesImpl gnpyUtilitiesImpl;
    private NetworkTransactionImpl networkTransaction;
    private static HttpServer httpServer;
    private GnpyConsumer gnpyConsumer;

    public GnpyUtilitiesImplTest() throws IOException {
        networkTransaction = new NetworkTransactionImpl(new RequestProcessor(getDataBroker()));
        JsonReader networkReader = null;
        JsonReader topoReader = null;

        try {
            // load openroadm-network
            Reader gnpyNetwork = new FileReader("src/test/resources/gnpy/gnpy_network.json",
                    StandardCharsets.UTF_8);

            networkReader = new JsonReader(gnpyNetwork);
            Networks networks = (Networks) JsonUtil.getInstance().getDataObjectFromJson(networkReader,
                    QName.create("urn:ietf:params:xml:ns:yang:ietf-network", "2018-02-26", "networks"));
            saveOpenRoadmNetwork(networks.getNetwork().values().iterator().next(), NetworkUtils.UNDERLAY_NETWORK_ID);
            // load openroadm-topology
            Reader gnpyTopo = new FileReader("src/test/resources/gnpy/gnpy_topology.json",
                    StandardCharsets.UTF_8);
            topoReader = new JsonReader(gnpyTopo);
            networks = (Networks) JsonUtil.getInstance().getDataObjectFromJson(topoReader,
                    QName.create("urn:ietf:params:xml:ns:yang:ietf-network", "2018-02-26", "networks"));
            saveOpenRoadmNetwork(networks.getNetwork().values().iterator().next(), NetworkUtils.OVERLAY_NETWORK_ID);
        } catch (FileNotFoundException | InterruptedException | ExecutionException e) {
            LOG.error("Cannot init test ", e);
            fail("Cannot init test ");

        } finally {
            try {
                if (networkReader != null) {
                    networkReader.close();
                }
                if (topoReader != null) {
                    topoReader.close();
                }
            } catch (IOException e) {
                LOG.warn("Cannot close reader ", e);
            }
        }

    }

    @Before
    public void initConsumer() {
        gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998", "mylogin", "mypassword",
                getDataStoreContextUtil().getBindingDOMCodecServices());
    }

    @BeforeClass
    public static void init() {
        // here we cannot use JerseyTest as we already extends AbstractTest
        final ResourceConfig rc = new ResourceConfig(GnpyStub.class);
        httpServer = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://localhost:9998"), rc);
    }

    @AfterClass
    public static void tearDown() {
        httpServer.shutdownNow();
    }

    private void saveOpenRoadmNetwork(Network network, String networkId)
            throws InterruptedException, ExecutionException {
        InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(networkId))).build();
        WriteTransaction dataWriteTransaction = getDataBroker().newWriteOnlyTransaction();
        dataWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, network);
        dataWriteTransaction.commit().get();
    }

    @Test
    public void askNewPathFromGnpyNullResultTest() throws Exception {
        gnpyUtilitiesImpl = new GnpyUtilitiesImpl(networkTransaction,
                PceTestData.getGnpyPCERequest("XPONDER-1", "XPONDER-2"),
                gnpyConsumer);
        assertNull("No hard constraints should be available", gnpyUtilitiesImpl.askNewPathFromGnpy(null));

    }

    @Test
    public void askNewPathFromGnpyTest() throws Exception {
        gnpyUtilitiesImpl = new GnpyUtilitiesImpl(networkTransaction,
                PceTestData.getGnpyPCERequest("XPONDER-3", "XPONDER-4"),
                gnpyConsumer);
        PceConstraintsCalc constraints = new PceConstraintsCalc(PceTestData.getPCE_simpletopology_test1_request(),
                networkTransaction);
        PceConstraints pceHardConstraints = constraints.getPceHardConstraints();
        assertNotNull("Hard constraints should be available", gnpyUtilitiesImpl.askNewPathFromGnpy(pceHardConstraints));


    }

    @Test
    public void verifyComputationByGnpyTest() throws Exception {
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
        Assert.assertFalse("Gnpy Computation should be false",result);
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
